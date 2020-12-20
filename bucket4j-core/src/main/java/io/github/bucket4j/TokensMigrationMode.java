package io.github.bucket4j;

/**
 * TODO
 */
public enum TokensMigrationMode {

    /**
     * TODO
     */
    PROPORTIONAL((byte) 0),

    /**
     * TODO
     */
    AS_IS((byte) 1),

    /**
     * TODO
     */
    RESET((byte) 2);

    private final byte id;

    TokensMigrationMode(byte id) {
        this.id = id;
    }

    private static final TokensMigrationMode[] modes = new TokensMigrationMode[] {
            PROPORTIONAL, AS_IS, RESET
    };

    public static TokensMigrationMode getById(byte id) {
        return modes[id];
    }

    public byte getId() {
        return id;
    }

}
