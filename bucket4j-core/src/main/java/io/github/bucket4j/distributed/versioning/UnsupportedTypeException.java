package io.github.bucket4j.distributed.versioning;

public class UnsupportedTypeException extends BackwardCompatibilityException {

    private final int typeId;

    public UnsupportedTypeException(int typeId) {
        super(formatMessage(typeId));
        this.typeId = typeId;
    }

    public int getTypeId() {
        return typeId;
    }

    private static String formatMessage(int typeId) {
        return  "Unsupported typeId " + typeId;
    }

}
