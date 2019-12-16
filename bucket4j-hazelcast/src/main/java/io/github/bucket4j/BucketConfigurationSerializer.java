package io.github.bucket4j;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BucketConfigurationSerializer implements StreamSerializer<BucketConfiguration>, TypedStreamDeserializer<BucketConfiguration> {

    private final int typeId;

    public BucketConfigurationSerializer(int typeId) {
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void write(ObjectDataOutput out, BucketConfiguration bucketConfiguration) throws IOException {
        Bandwidth[] bandwidths = bucketConfiguration.getBandwidths();
        out.writeInt(bandwidths.length);
        for (Bandwidth bandwidth : bandwidths) {
            out.writeObject(bandwidth);
        }
    }

    @Override
    public BucketConfiguration read(ObjectDataInput in) throws IOException {
        return read0(in);
    }

    @Override
    public BucketConfiguration read(ObjectDataInput in, Class aClass) throws IOException {
        return read0(in);
    }

    private BucketConfiguration read0(ObjectDataInput in) throws IOException {
        int bandwidthAmount = in.readInt();
        List<Bandwidth> bandwidths = new ArrayList<>(bandwidthAmount);
        for (int ii = 0; ii < bandwidthAmount; ii++) {
            Bandwidth bandwidth = in.readObject(Bandwidth.class);
            bandwidths.add(bandwidth);
        }
        return new BucketConfiguration(bandwidths);
    }
}
