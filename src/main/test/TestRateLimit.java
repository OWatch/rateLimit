import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.RateLimit;
import singleCluster.CounterRateLimiter;

public class TestRateLimit {
    public static void main(String[] args) {
        testSpecifiedRateLimiter(new CounterRateLimiter());
    }

    private static void testSpecifiedRateLimiter(RateLimit rateLimit) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i= 0; i < 10; i++) {
            executorService.submit(()->{
                while (true) {
                    if (rateLimit.grant()) {
                        System.out.println(Thread.currentThread().getName() + " Running...");
                    } else {
                        System.out.println(Thread.currentThread().getName() + " Rejected!");
                    }
                }
            }, "ti");
        }
    }
}
