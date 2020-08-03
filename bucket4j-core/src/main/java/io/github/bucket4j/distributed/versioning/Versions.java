package io.github.bucket4j.distributed.versioning;

public enum Versions implements Version {

    v_5_0_0(1);

    private final int number;

    private Versions(int number) {
        this.number = number;
    }

    public static void check(int formatNumber, Versions min, Versions max) {
        if (formatNumber < min.getNumber()) {
            throw new UsageOfObsoleteApiException(formatNumber, min.number);
        }
        if (formatNumber > max.getNumber()) {
            throw new UsageOfUnsupportedApiException(formatNumber, max.number);
        }
    }

    public static void checkMin(int formatNumber, Versions min) {
        if (formatNumber < min.getNumber()) {
            throw new UsageOfObsoleteApiException(formatNumber, min.number);
        }
    }

    public static void checkMax(int formatNumber, Versions max) {
        if (formatNumber > max.getNumber()) {
            throw new UsageOfUnsupportedApiException(formatNumber, max.number);
        }
    }

    @Override
    public int getNumber() {
        return number;
    }

    public static Version getLatest() {
        return v_5_0_0;
    }

    public static Version getOldest() {
        return v_5_0_0;
    }

    public static Version byNumber(int number) {
        for (Versions version : values()) {
            if (version.number == number) {
                return version;
            }
        }
        return new UnknownVersion(number);
    }

    public static class UnknownVersion implements Version {

        private final int number;

        public UnknownVersion(int number) {
            this.number = number;
        }

        @Override
        public int getNumber() {
            return number;
        }
    }

}
