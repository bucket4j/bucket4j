package io.github.bucket4j.distributed.proxy.synchronization;

import io.github.bucket4j.distributed.proxy.synchronization.direct.DirectSynchronization;

public class Synchronizations {

    static Synchronization direct() {
        return DirectSynchronization.instance;
    }

}
