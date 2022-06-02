package io.roach.data.jdbc.plain;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

public class PlainJdbcApplication {
    private static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }

    private static TransactionCallback<BigDecimal> transfer(List<Account> legs) {
        return conn -> {
            BigDecimal total = BigDecimal.ZERO;
            BigDecimal checksum = BigDecimal.ZERO;

            for (Account leg : legs) {
                BigDecimal balance = readBalance(conn, leg.name);
                updateBalance(conn, leg.name, balance.add(leg.amount));
                checksum = checksum.add(leg.amount);
                total = total.add(leg.amount.abs());
            }

            if (checksum.compareTo(BigDecimal.ZERO) != 0) {
                throw new BusinessException(
                        "Sum of account legs must equal 0 (got " + checksum.toPlainString() + ")"
                );
            }

            return total;
        };
    }

    private static List<String> readAccountNames(Connection conn) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM account")) {
            try (ResultSet res = ps.executeQuery()) {
                while (res.next()) {
                    names.add(res.getString(1));
                }
            }
        }
        return names;
    }

    private static BigDecimal readBalance(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM account WHERE name = ?")) {
            ps.setString(1, name);

            try (ResultSet res = ps.executeQuery()) {
                if (!res.next()) {
                    throw new BusinessException("Account not found: " + name);
                }
                return res.getBigDecimal("balance");
            }
        }
    }

    private static void updateBalance(Connection conn, String name, BigDecimal balance) throws SQLException {
        try (PreparedStatement ps = conn
                .prepareStatement(
                        "UPDATE account SET balance = ?, updated=clock_timestamp() where name = ?")) {
            ps.setBigDecimal(1, balance);
            ps.setString(2, name);
            if (ps.executeUpdate() != 1) {
                throw new DataAccessException("Rows affected != 1  for " + name);
            }
        }
    }

    private static BigDecimal readTotalBalance(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "select sum(balance) balance from account")) {
            try (ResultSet res = ps.executeQuery()) {
                if (!res.next()) {
                    throw new SQLException("Empty result");
                }
                return res.getBigDecimal("balance");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int workers = Runtime.getRuntime().availableProcessors();

        final HikariDataSource hikariDS = new HikariDataSource();
        hikariDS.setJdbcUrl("jdbc:postgresql://localhost:26257/roach_data?sslmode=disable");
        hikariDS.setUsername("root");
        hikariDS.setAutoCommit(true);
        hikariDS.setMaximumPoolSize(workers);
        hikariDS.setMinimumIdle(workers);

        boolean enableProxy = Arrays.asList(args).contains("--enable-proxy");

        DataSource ds = enableProxy ?
                ProxyDataSourceBuilder
                        .create(hikariDS)
                        .asJson()
                        .logQueryBySlf4j()
                        .build() : hikariDS;

        SchemaSupport.setupSchema(ds);

        ConnectionTemplate.execute(ds, PlainJdbcApplication::readAccountNames).forEach(name -> {
            System.out.printf("Balance before for %s: %s\n", name,
                    ConnectionTemplate.execute(ds, c -> readBalance(c, name)));
        });

        final BigDecimal initialBalance = ConnectionTemplate.execute(ds, PlainJdbcApplication::readTotalBalance);
        System.out.printf("Total balance before: %s\n", initialBalance);

        // Run concurrently for more exiting effects
        final ExecutorService executorService = Executors.newFixedThreadPool(workers);
        final Deque<Future<BigDecimal>> futures = new ArrayDeque<>();

        final int iterations = 200;
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        IntStream.rangeClosed(1, iterations).forEach(
                value -> futures.add(executorService.submit(() -> {
                            List<Account> legs = new ArrayList<>();

                            IntStream.rangeClosed(1, 4).forEach(leg -> {
                                String from = "customer:" + random.nextInt(1, 100);
                                String to = "customer:" + random.nextInt(1, 100);
                                if (!from.equals(to)) {
                                    BigDecimal amt = new BigDecimal("0.15");
                                    legs.add(new Account(from, amt));
                                    legs.add(new Account(to, amt.negate()));
                                }
                            });

                            System.out.printf("\r%,8d/%d", value, iterations);

                            return TransactionTemplate.execute(ds, transfer(legs));
                        }
                )));

        int success = 0;
        int fail = 0;
        BigDecimal total = BigDecimal.ZERO;
        while (!futures.isEmpty()) {
            System.out.printf("Awaiting completion (%d futures)\n", futures.size());
            try {
                BigDecimal tot = futures.pop().get();
                total = total.add(tot);
                success++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                e.getCause().printStackTrace();
                fail++;
            }
        }

        executorService.shutdownNow();

        ConnectionTemplate.execute(ds, PlainJdbcApplication::readAccountNames).forEach(name -> {
            System.out.printf("Balance after for %s: %s\n", name,
                    ConnectionTemplate.execute(ds, c -> readBalance(c, name)));
        });

        final BigDecimal finalBalance = ConnectionTemplate.execute(ds, PlainJdbcApplication::readTotalBalance);

        System.out.printf("Transaction success: %d\n", success);
        System.out.printf("Transaction fail: %d\n", fail);
        System.out.printf("Total turnover: %s\n", total);
        System.out.printf("Total balance before: %s\n", initialBalance);
        System.out.printf("Total balance after: %s\n", finalBalance);
        if (!finalBalance.equals(initialBalance)) {
            System.out.println("Balance invariant violation! (╯°□°)╯︵ ┻━┻");
            System.out.printf("Lost funds: %s\n", initialBalance.subtract(finalBalance));
        }
    }
}
