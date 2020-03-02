
package io.github.bucket4j;

import java.text.MessageFormat;

public class IncompatibleConfigurationException extends IllegalArgumentException {

    private static final long serialVersionUID = 42L;

    private final BucketConfiguration previousConfiguration;
    private final BucketConfiguration newConfiguration;

    public IncompatibleConfigurationException(BucketConfiguration previousConfiguration, BucketConfiguration newConfiguration) {
        super(generateMessage(previousConfiguration, newConfiguration));
        this.previousConfiguration = previousConfiguration;
        this.newConfiguration = newConfiguration;
    }

    public BucketConfiguration getNewConfiguration() {
        return newConfiguration;
    }

    public BucketConfiguration getPreviousConfiguration() {
        return previousConfiguration;
    }

    private static String generateMessage(BucketConfiguration previousConfiguration, BucketConfiguration newConfiguration) {
        String format = "New configuration {0} incompatible with previous configuration {1}";
        return MessageFormat.format(format, newConfiguration, previousConfiguration);
    }

}
