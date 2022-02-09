package io.github.bucket4j.distributed.proxy.generic.select_for_update;

public class LockAndGetResult {

    private static final LockAndGetResult NOT_LOCKED = new LockAndGetResult(null, false);

    private final byte[] data;
    private final boolean locked;

    private LockAndGetResult(byte[] data, boolean locked) {
        this.data = data;
        this.locked = locked;
    }

    public static LockAndGetResult notLocked() {
        return NOT_LOCKED;
    }

    public static LockAndGetResult locked(byte[] data) {
        return new LockAndGetResult(data, true);
    }

    public byte[] getData() {
        return data;
    }

    public boolean isLocked() {
        return locked;
    }

}
