package io.github.bucket4j.distributed.versioning;

public interface Version {

    int getNumber();

    static Version max(Version first, Version second) {
        return first.getNumber() >= second.getNumber()? first: second;
    }

}
