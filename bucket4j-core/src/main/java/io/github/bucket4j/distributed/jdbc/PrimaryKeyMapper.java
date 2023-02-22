package io.github.bucket4j.distributed.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Vladimir Bukhtoyarov
 */
public interface PrimaryKeyMapper<T> {

    void set(PreparedStatement statement, int i, T value) throws SQLException;

    PrimaryKeyMapper<Long> LONG = PreparedStatement::setLong;

    PrimaryKeyMapper<Integer> INT = PreparedStatement::setInt;

    PrimaryKeyMapper<String> STRING = PreparedStatement::setString;

}
