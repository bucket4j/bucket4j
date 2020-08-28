package io.github.bucket4j.distributed.versioning;

import java.text.MessageFormat;

public class UsageOfObsoleteApiException extends BackwardCompatibilityException {

    private final int requestedFormatNumber;
    private final int minSupportedFormatNumber;

    public UsageOfObsoleteApiException(int requestedFormatNumber, int minSupportedFormatNumber) {
        super(formatMessage(requestedFormatNumber, minSupportedFormatNumber));
        this.requestedFormatNumber = requestedFormatNumber;
        this.minSupportedFormatNumber = minSupportedFormatNumber;
    }

    public int getRequestedFormatNumber() {
        return requestedFormatNumber;
    }

    public int getMinSupportedFormatNumber() {
        return minSupportedFormatNumber;
    }

    private static String formatMessage(int formatNumber, int minFormatNumber) {
        String fmt = "Command cannot be executed, because it encoded in {} format number, when minimum supported by backend is {}";
        return MessageFormat.format(fmt, formatNumber, minFormatNumber);
    }

}
