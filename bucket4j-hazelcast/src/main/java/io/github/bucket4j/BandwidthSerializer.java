package io.github.bucket4j;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;

import java.io.IOException;


public class BandwidthSerializer implements StreamSerializer<Bandwidth>, TypedStreamDeserializer<Bandwidth> {

    private final int typeId;

    public BandwidthSerializer(int typeId) {
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return this.typeId;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void write(ObjectDataOutput out, Bandwidth bandwidth) throws IOException {
        out.writeLong(bandwidth.getCapacity());
        out.writeLong(bandwidth.getInitialTokens());
        out.writeLong(bandwidth.getRefillPeriodNanos());
        out.writeLong(bandwidth.getRefillTokens());
        out.writeBoolean(bandwidth.refillIntervally);
        out.writeLong(bandwidth.getTimeOfFirstRefillMillis());
        out.writeBoolean(bandwidth.useAdaptiveInitialTokens);
    }

    @Override
    public Bandwidth read(ObjectDataInput in) throws IOException {
        return read0(in);
    }

    @Override
    public Bandwidth read(ObjectDataInput in, Class aClass) throws IOException {
        return read0(in);
    }

    private Bandwidth read0(ObjectDataInput in) throws IOException {
        long capacity = in.readLong();
        long initialTokens = in.readLong();
        long refillPeriodNanos = in.readLong();
        long refillTokens = in.readLong();
        boolean refillIntervally = in.readBoolean();
        long timeOfFirstRefillMillis = in.readLong();
        boolean useAdaptiveInitialTokens = in.readBoolean();

        return new Bandwidth(capacity, refillPeriodNanos, refillTokens, initialTokens, refillIntervally,
                timeOfFirstRefillMillis, useAdaptiveInitialTokens);
    }
}
