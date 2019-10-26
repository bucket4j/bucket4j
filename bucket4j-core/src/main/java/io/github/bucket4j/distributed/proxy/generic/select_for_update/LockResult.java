package io.github.bucket4j.distributed.proxy.generic.select_for_update;

public enum LockResult {

    DATA_EXISTS_AND_LOCKED,
    DATA_NOT_EXISTS_AND_LOCKED

}
