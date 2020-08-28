package io.github.bucket4j.distributed.versioning;

import java.text.MessageFormat;

public class UsageOfUnsupportedApiException extends BackwardCompatibilityException {

    private final int requestedFormatNumber;
    private final int maxSupportedFormatNumber;

    public UsageOfUnsupportedApiException(int requestedFormatNumber, int maxSupportedFormatNumber) {
        super(formatMessage(requestedFormatNumber, maxSupportedFormatNumber));
        this.requestedFormatNumber = requestedFormatNumber;
        this.maxSupportedFormatNumber = maxSupportedFormatNumber;
    }

    public int getRequestedFormatNumber() {
        return requestedFormatNumber;
    }

    public int getMaxSupportedFormatNumber() {
        return maxSupportedFormatNumber;
    }

    private static String formatMessage(int formatNumber, int maxFormatNumber) {
        String fmt = "Command cannot be executed, because it encoded in {} format number, when maximum supported by backend is {}";
        return MessageFormat.format(fmt, formatNumber, maxFormatNumber);
    }

}
