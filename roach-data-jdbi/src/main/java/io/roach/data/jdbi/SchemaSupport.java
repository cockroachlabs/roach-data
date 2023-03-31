package io.roach.data.jdbi;

import org.jdbi.v3.core.Jdbi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class SchemaSupport {
    private SchemaSupport() {
    }

    public static void setupSchema(Jdbi jdbi) {
        StringBuilder buffer = new StringBuilder();

        try {
            URL sql = JdbiApplication.class.getResource("/db/create.sql");
            Files.readAllLines(Paths.get(sql.toURI())).forEach(line -> {
                if (!line.startsWith("--") && !line.isEmpty()) {
                    buffer.append(line);
                }
                if (line.endsWith(";") && buffer.length() > 0) {
                    jdbi.useHandle(handle -> {
                        handle.execute(buffer.toString());
                    });
                    buffer.setLength(0);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
