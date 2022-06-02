package io.roach.data.jdbc.plain;

import java.sql.Connection;
import java.sql.SQLException;

interface ConnectionCallback<T> {
    T doInConnection(Connection conn) throws SQLException;
}
