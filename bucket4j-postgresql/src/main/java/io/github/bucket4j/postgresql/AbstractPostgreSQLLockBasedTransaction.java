package io.github.bucket4j.postgresql;

import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockBasedTransaction;

import java.sql.Connection;

public interface AbstractPostgreSQLLockBasedTransaction extends LockBasedTransaction {

    Connection getConnection();

}
