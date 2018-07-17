package singleCluster;

import java.util.concurrent.atomic.AtomicLong;

import common.RateLimit;

public class CounterRateLimiter implements RateLimit {
    // Or use Redis to store atomic counter
    private final AtomicLong counter = new AtomicLong(0);
    private long timeStamp = System.currentTimeMillis();

    @Override
    public boolean grant() {
        long now = System.currentTimeMillis();
        System.out.println(counter);
        if (now < timeStamp + INTERNAL) {
            return counter.incrementAndGet() < LIMIT;
        } else {
            timeStamp = now;
            counter.set(1);
            return true;
        }
    }
}
