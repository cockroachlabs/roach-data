package io.roach.data.jdbi;

import com.zaxxer.hikari.HikariDataSource;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.core.transaction.SerializableTransactionRunner;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JdbiApplication {
    private static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }

    private static BigDecimal transfer(DataSource ds, List<Account> legs) {
        Jdbi jdbi = Jdbi.create(ds);
        jdbi.setTransactionHandler(new SerializableTransactionRunner());
        return jdbi.inTransaction(TransactionIsolationLevel.SERIALIZABLE, transactionHandle -> {
            BigDecimal total = BigDecimal.ZERO;
            BigDecimal checksum = BigDecimal.ZERO;

            for (Account leg : legs) {
                BigDecimal balance = readBalance(transactionHandle, leg.name);
                updateBalance(transactionHandle, leg.name, balance.add(leg.amount));
                checksum = checksum.add(leg.amount);
                total = total.add(leg.amount.abs());
            }

            if (checksum.compareTo(BigDecimal.ZERO) != 0) {
                throw new BusinessException(
                        "Sum of account legs must equal 0 (got " + checksum.toPlainString() + ")"
                );
            }

            return total;
        });
    }

    private static List<String> readAccountNames(Handle handle) {
        Query query = handle.createQuery("SELECT name FROM account");
        return query.mapTo(String.class).collect(Collectors.toList());
    }

    private static BigDecimal readBalance(Handle handle, String name) {
        Query query = handle.createQuery("SELECT balance FROM account WHERE name = ?");
        query.bind(0, name);
        return query.mapTo(BigDecimal.class).findOne()
                .orElseThrow(() -> new BusinessException("Account not found: " + name));
    }

    private static void updateBalance(Handle handle, String name, BigDecimal balance) {
        Update update = handle.createUpdate("UPDATE account SET balance = ?, updated=clock_timestamp() where name = ?");
        update.bind(0, balance);
        update.bind(1, name);
        if (update.execute() != 1) {
            throw new DataAccessException("Rows affected != 1  for " + name);
        }
    }

    private static BigDecimal readTotalBalance(Handle handle) {
        Query query = handle.createQuery("select sum(balance) balance from account");
        return query.mapTo(BigDecimal.class).findOne().orElseThrow(() -> new BusinessException("No accounts?"));
    }

    public static void main(String[] args) {
        int workers = Runtime.getRuntime().availableProcessors();
        boolean verbose = false;
        int iterations = 200;

        final HikariDataSource hikariDS = new HikariDataSource();
        {
            hikariDS.setJdbcUrl("jdbc:postgresql://localhost:26257/roach_data?sslmode=disable");
//            hikariDS.setJdbcUrl("jdbc:postgresql://192.168.1.99:26257/roach_data?sslmode=disable");
            hikariDS.setUsername("root");
            hikariDS.setMaximumPoolSize(workers);
            hikariDS.setMinimumIdle(workers);
        }

        LinkedList<String> argsList = new LinkedList<>(Arrays.asList(args));
        while (!argsList.isEmpty()) {
            String arg = argsList.pop();
            if (arg.equals("--workers")) {
                workers = Integer.parseInt(argsList.pop());
            } else if (arg.equals("--iterations")) {
                iterations = Integer.parseInt(argsList.pop());
            } else if (arg.equals("--verbose")) {
                verbose = true;
            } else if (arg.equals("--url")) {
                hikariDS.setJdbcUrl("jdbc:postgresql://" + argsList.pop());
            } else if (arg.equals("--user")) {
                hikariDS.setUsername(argsList.pop());
            } else if (arg.equals("--password")) {
                hikariDS.setUsername(argsList.pop());
            } else if (arg.equals("--pool-size")) {
                int size = Integer.parseInt(argsList.pop());
                hikariDS.setMaximumPoolSize(size);
                hikariDS.setMinimumIdle(size);
            } else {
                System.out.println("Bad option: " + arg);
                System.exit(1);
            }
        }

        hikariDS.setAutoCommit(true); // Setting it to false will mess up JDBI

        DataSource ds = verbose ?
                ProxyDataSourceBuilder
                        .create(hikariDS)
                        .asJson()
                        .logQueryBySlf4j(SLF4JLogLevel.TRACE, "io.roach.SQL_TRACE")
                        .multiline()
                        .build() : hikariDS;

        Jdbi jdbi = Jdbi.create(ds);
        jdbi.setTransactionHandler(new SerializableTransactionRunner());

        SchemaSupport.setupSchema(jdbi);

        List<String> names = jdbi.withHandle(JdbiApplication::readAccountNames);

        names.forEach(name -> {
            BigDecimal balance = jdbi.withHandle(handle -> readBalance(handle, name));
            System.out.printf("Balance before for %s: %s\n", name, balance);
        });

        final BigDecimal initialBalance = jdbi.withHandle(JdbiApplication::readTotalBalance);
        System.out.printf("Total balance before: %s\n", initialBalance);

        // Run concurrently for more exiting effects
        final ExecutorService executorService = Executors.newFixedThreadPool(workers);
        final Deque<Future<BigDecimal>> futures = new ArrayDeque<>();

        final ThreadLocalRandom random = ThreadLocalRandom.current();

        final int totalWorkers = iterations;

        IntStream.rangeClosed(1, totalWorkers).forEach(
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

                            System.out.printf("%,8d/%d done\n", value, totalWorkers);

                            return transfer(ds, legs);
                        }
                )));

        int success = 0;
        int fail = 0;
        BigDecimal total = BigDecimal.ZERO;
        while (!futures.isEmpty()) {
            System.out.printf("Awaiting completion (%d futures remains)\n", futures.size());
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

        names.forEach(name -> {
            BigDecimal balance = jdbi.withHandle(handle -> readBalance(handle, name));
            System.out.printf("Balance after for %s: %s\n", name, balance);
        });

        final BigDecimal finalBalance = jdbi.withHandle(JdbiApplication::readTotalBalance);

        System.out.println();
        System.out.printf("Transaction success: %d\n", success);
        System.out.printf("Transaction fail: %d\n", fail);
        System.out.printf("Total turnover: %s\n", total);
        System.out.printf("Total balance before: %s\n", initialBalance);
        System.out.printf("Total balance after: %s\n", finalBalance);

        if (!finalBalance.equals(initialBalance)) {
            System.out.println("Balance invariant violation! (╯°□°)╯︵ ┻━┻");
            System.out.printf("Lost funds: %s\n", initialBalance.subtract(finalBalance));
        } else {
            System.out.println("¯\\_(ツ)_/¯");
        }
    }
}
