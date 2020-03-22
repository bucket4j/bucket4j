package io.github.bucket4j.local;

import org.openjdk.jol.info.ClassLayout;

import static java.lang.System.out;

public class LockFreeBucketLayout {

    public static void main(String[] args) {
        out.println(ClassLayout.parseClass(LockFreeBucket.class).toPrintable());
    }

}
