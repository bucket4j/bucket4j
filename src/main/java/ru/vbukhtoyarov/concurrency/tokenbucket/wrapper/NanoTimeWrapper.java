package ru.vbukhtoyarov.concurrency.tokenbucket.wrapper;

public class NanoTimeWrapper {

    public static final NanoTimeWrapper SYSTEM = new NanoTimeWrapper();  
    
    public long nanoTime() {
        return System.nanoTime();
    }

}
