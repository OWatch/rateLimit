package singleCluster;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import common.RateLimit;

public class SlideWindowRateLimiter implements RateLimit {
    // Or use Redis to store slide window
    private long timeStamp = System.currentTimeMillis();

    private static final int LIMIT = 5;
    private long duration;//每个格子的时长
    private int bucketSize;//总格子数
    private final long windowTime;
    private final ScheduledExecutorService scheduledExecutor;
    private long startedTimestamp;
    private volatile int head;//指向第一个格子
    private AtomicInteger[] buckets;

    public SlideWindowRateLimiter(long duration, int bucketSize) {
        this.duration = duration;
        this.bucketSize = bucketSize;
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.windowTime = duration * bucketSize;
        buckets = new AtomicInteger[bucketSize];
    }

    /**
     * 初始化
     */
    protected void init(){
        startedTimestamp = System.currentTimeMillis();
        for(int i = 0; i < bucketSize;i++){
            buckets[i] = new AtomicInteger(0);
        }
        head = 0;//指向第一个格子
        scheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                timeRolling();
            }
        },duration/2,duration, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean grant(){
        long now = System.currentTimeMillis();
        long timestampDiff = now - startedTimestamp;
        long mask = timestampDiff % (windowTime);
        //相对于head的位置
        int idx = getBucketIndex(mask);
        if(idx == -1){
            throw new IllegalStateException("illegalState");
        }
        buckets[idx].incrementAndGet();
        int count = getCurrentCount();
        System.out.println("当前count:" + count);
        return count <= LIMIT;
    }

    /**
     * 查找当前的位置
     * @param mask
     * @return
     */
    private int getBucketIndex(long mask){
        int cursor = head;
        int stopIndex = cursor;
        if(mask <= duration){
            return cursor;
        }
        long d = duration;
        while (true){
            cursor = (cursor + 1) % bucketSize;
            if(cursor == stopIndex){
                return -1;
            }
            d = d + duration;
            if(mask <= d){
                return cursor;
            }
        }
    }

    /**
     * 获取当前计数
     * @return  int
     */
    private int getCurrentCount(){
        return Arrays.stream(buckets).mapToInt(buckets -> buckets.get()).sum();
    }

    /**
     * 时间滚动
     */
    private void timeRolling(){
        //每次格子移动都会更改head
        int last = head;
        head = (head + 1) % bucketSize;
        System.out.println("时间向前移动一格：" + head);
        buckets[last].set(0);//reset
    }

    /**
     * 关闭
     */
    protected void shutdown() throws InterruptedException {
        scheduledExecutor.shutdown();
        scheduledExecutor.awaitTermination(5,TimeUnit.SECONDS);
    }
}
