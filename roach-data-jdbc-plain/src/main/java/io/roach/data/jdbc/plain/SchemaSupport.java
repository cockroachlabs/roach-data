package io.roach.data.jdbc.plain;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Statement;

import javax.sql.DataSource;

public abstract class SchemaSupport {
    private SchemaSupport() {
    }

    public static void setupSchema(DataSource ds) throws Exception {
        URL sql = PlainJdbcApplication.class.getResource("/db/create.sql");

        StringBuilder buffer = new StringBuilder();

        Files.readAllLines(Paths.get(sql.toURI())).forEach(line -> {
            if (!line.startsWith("--") && !line.isEmpty()) {
                buffer.append(line);
            }
            if (line.endsWith(";") && buffer.length() > 0) {
                ConnectionTemplate.execute(ds, conn -> {
                    try (Statement statement = conn.createStatement()) {
                        statement.execute(buffer.toString());
                    }
                    buffer.setLength(0);
                    return null;
                });
            }
        });
    }
}
