package io.github.bucket4j.distributed.versioning;

import java.text.MessageFormat;

public class UsageOfObsoleteApiException extends BackwardCompatibilityException {

    private final int formatNumber;
    private final int minFormatNumber;

    public UsageOfObsoleteApiException(int formatNumber, int minFormatNumber) {
        super(formatMessage(formatNumber, minFormatNumber));
        this.formatNumber = formatNumber;
        this.minFormatNumber = minFormatNumber;
    }

    public int getFormatNumber() {
        return formatNumber;
    }

    public int getMinFormatNumber() {
        return minFormatNumber;
    }

    private static String formatMessage(int formatNumber, int minFormatNumber) {
        String fmt = "Command cannot be executed, because it encoded in {} format number, when minimum supported by backend is {}";
        return MessageFormat.format(fmt, formatNumber, minFormatNumber);
    }

}
