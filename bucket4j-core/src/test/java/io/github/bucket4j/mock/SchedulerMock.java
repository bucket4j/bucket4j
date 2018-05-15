/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.mock;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class SchedulerMock implements ScheduledExecutorService {

    private RuntimeException exception;
    private long acummulatedDelayNanos;
    private TimeMeterMock timeMeter;

    public long getAcummulatedDelayNanos() {
        return acummulatedDelayNanos;
    }

    public void setException(RuntimeException exception) {
        this.exception = exception;
    }

    public SchedulerMock() {
    }

    public SchedulerMock(TimeMeterMock timeMeter) {
        this.timeMeter = timeMeter;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        if (exception != null) {
            throw exception;
        }
        acummulatedDelayNanos += unit.toNanos(delay);
        if (timeMeter != null) {
            timeMeter.addTime(unit.toNanos(delay));
        }
        command.run();
        return null;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTerminated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<?> submit(Runnable task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(Runnable command) {
        throw new UnsupportedOperationException();
    }

}
