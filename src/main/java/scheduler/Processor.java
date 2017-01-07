package scheduler;

import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by pallav.kothari on 1/7/17.
 */
@Component
public class Processor {
    private final RedisScheduler scheduler;
    private final ScheduledExecutorService scheduledExecutorService;

    @Autowired
    public Processor(RedisScheduler scheduler) {
        this.scheduler = scheduler;
        this.scheduledExecutorService = MoreExecutors.getExitingScheduledExecutorService((ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1));
    }

    public void init() {
        this.scheduledExecutorService.scheduleAtFixedRate(scheduler::process, 1, 1, TimeUnit.SECONDS);
    }
}
