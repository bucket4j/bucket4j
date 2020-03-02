
package io.github.bucket4j;

public class PackageAcessor {

    public static BucketConfiguration buildConfiguration(AbstractBucketBuilder builder) {
        return builder.buildConfiguration();
    }

}
