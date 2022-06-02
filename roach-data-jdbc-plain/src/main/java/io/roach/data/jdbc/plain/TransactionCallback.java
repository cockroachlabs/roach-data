package io.roach.data.jdbc.plain;

import java.sql.Connection;
import java.sql.SQLException;

public interface TransactionCallback<T> {
    T doInTransaction(Connection conn) throws SQLException;
}
