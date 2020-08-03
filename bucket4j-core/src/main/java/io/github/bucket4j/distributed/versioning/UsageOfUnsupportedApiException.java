package io.github.bucket4j.distributed.versioning;

import java.text.MessageFormat;

public class UsageOfUnsupportedApiException extends BackwardCompatibilityException {

    private final int formatNumber;
    private final int maxFormatNumber;

    public UsageOfUnsupportedApiException(int formatNumber, int maxFormatNumber) {
        super(formatMessage(formatNumber, maxFormatNumber));
        this.formatNumber = formatNumber;
        this.maxFormatNumber = maxFormatNumber;
    }

    public int getFormatNumber() {
        return formatNumber;
    }

    public int getMaxFormatNumber() {
        return maxFormatNumber;
    }

    private static String formatMessage(int formatNumber, int maxFormatNumber) {
        String fmt = "Command cannot be executed, because it encoded in {} format number, when maximum supported by backend is {}";
        return MessageFormat.format(fmt, formatNumber, maxFormatNumber);
    }

}
