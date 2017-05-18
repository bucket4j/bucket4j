package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.grid.GridBucketState;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.cache.Cache;
import java.io.Serializable;

public class JCacheFixture implements TestRule {

    private Cache<String, GridBucketState> cache;
    private Cloud cloud;
    private ViNode server;

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                startCloud();
                try {
                    base.evaluate();
                } finally {
                    stopCloud();
                }
            }
        };
    }

    public <T extends Runnable&Serializable> void startGridOnServer(T command) {
        server.exec(command);
    }

    public void setCache(Cache<String, GridBucketState> cache) {
        this.cache = cache;
    }

    public Cache<String, GridBucketState> getCache() {
        return cache;
    }

    private void startCloud() {
        // start separated JVM on current host
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        this.server = cloud.node("stateful-jcache-server");
    }

    private void stopCloud() {
        if (cloud != null) {
            cloud.shutdown();
        }
    }

}
