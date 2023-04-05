package io.roach.data.parallel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static String executeQuery(DataSource dataSource, int locality, boolean simulateDelay) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement("select * from t where locality = ?")) {
                ps.setObject(1, locality);
                try (ResultSet rs = ps.executeQuery()) {

                    if (simulateDelay) {
                        try {
                            logger.debug("Heavy compute for 5s..");
                            Thread.sleep(5000); // Simulate heavy scan
                            logger.debug("Done");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }

                    if (rs.next()) {
                        return rs.getString("id");
                    }
                    throw new SQLException("No match!");
                }
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
        hikariDS.setConnectionTimeout(5000);

        DataSource ds =
                ProxyDataSourceBuilder
                        .create(hikariDS)
                        .asJson()
                        .logQueryBySlf4j()
                        .multiline()
                        .build();

        try (Connection c = ds.getConnection()) {
            c.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS t (" +
                            "     locality INT8 NOT NULL, " +
                            "     id STRING NOT NULL, " +
                            "     CONSTRAINT pk PRIMARY KEY (locality ASC, id ASC), " +
                            "     CONSTRAINT check_locality CHECK (locality IN (0:::INT8, 1:::INT8, 2:::INT8))" +
                            " )");
            c.createStatement().execute(
                    "insert into t values" +
                            " (0, '0abc')," +
                            " (0, '00abc')," +
                            " (1, '1abc')," +
                            " (1, '11abc')," +
                            " (2, '2abc')," +
                            " (2, '22abc')" +
                            " on conflict do nothing");
        }

        logger.info("Lets run serially..");
        {
            Instant start = Instant.now();

            logger.info("Got: {}", executeQuery(ds, 0, true));
            logger.info("Got: {}", executeQuery(ds, 1, true));
            logger.info("Got: {}", executeQuery(ds, 2, true));

            logger.info("Time spent: {}", Duration.between(start, Instant.now()));
        }

        logger.info("Now lets run again in parallel with cancellation..");
        {
            List<Callable<Object>> tasks = new ArrayList<>();
            tasks.add(() -> executeQuery(ds, 0, true));
            tasks.add(() -> executeQuery(ds, 1, true));
            tasks.add(() -> executeQuery(ds, 2, true));

            Instant start = Instant.now();

            ConcurrencyUtils.runConcurrentlyAndWait(tasks, 10, TimeUnit.SECONDS, result -> {
                logger.info("Got: {}", result);
            }, throwable -> {
                logger.error("Exception", throwable);
                return null;
            });

            logger.info("Time spent: {}", Duration.between(start, Instant.now()));
        }

        logger.info("Now lets run again massively in parallel with cancellation..");
        {
            List<Callable<Object>> tasks = new ArrayList<>();
            IntStream.range(0, 10_000).forEach(value -> {
                tasks.add(() -> executeQuery(ds, value % 3, false));
            });

            Instant start = Instant.now();

            ConcurrencyUtils.runConcurrentlyAndWait(tasks, 10, TimeUnit.MINUTES, result -> {
                logger.info("Got: {}", result);
            }, throwable -> {
                logger.error("Exception", throwable);
                return null;
            });

            logger.info("Time spent: {}", Duration.between(start, Instant.now()));
        }

        logger.info("Now lets run again in parallel without cancellation..");
        {
            List<Callable<Object>> tasks = new ArrayList<>();
            tasks.add(() -> executeQuery(ds, 0, true));
            tasks.add(() -> executeQuery(ds, 1, true));
            tasks.add(() -> executeQuery(ds, 2, true));

            Instant start = Instant.now();

            ConcurrencyUtils.runConcurrentlyAndWait(tasks, result -> {
                logger.info("Got: {}", result);
            }, throwable -> {
                logger.error("Exception", throwable);
                return null;
            });

            logger.info("Time spent: {}", Duration.between(start, Instant.now()));
        }

        logger.info("Bye!");

        ds.unwrap(HikariDataSource.class).close();
    }
}
