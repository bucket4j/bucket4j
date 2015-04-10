package com.github.bandwidthlimiter.bucket;

public final class ImmutableCapacity implements Capacity {

    private final long value;

    public ImmutableCapacity(long value) {
        this.value = value;
    }

    @Override
    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ImmutableCapacity{" +
                "value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImmutableCapacity that = (ImmutableCapacity) o;

        if (value != that.value) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

}
