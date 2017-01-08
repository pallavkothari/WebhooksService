package scheduler;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.Math.max;

/**
 * Created by pallav.kothari on 1/6/17.
 */
@Component
public class RedisScheduler {
    public static final String SCHEDULER_KEY = "PROD_Q";
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final HttpCalloutService callouts;
    private final Redis redis;

    @Autowired
    RedisScheduler(HttpCalloutService callouts, Redis redis) {
        this.callouts = callouts;
        this.redis = redis;
    }

    public String schedule(RedisTrigger trigger) {
        String schedulerKey = getSchedulerKey();
        try (Jedis jedis = redis.borrow()) {
            jedis.zadd(schedulerKey, trigger.getScheduledTime(), GSON.toJson(trigger));
        }
        return schedulerKey;
    }

    public void scheduleWithFixedDelay(RedisTrigger trigger) {
        Preconditions.checkState(trigger.isRecurring());
        schedule(trigger);
    }

    String getSchedulerKey() {
        return SCHEDULER_KEY;
    }

    public void process() {
        long now = getMaxTimestampForDequeue();
        long lastProcessed = 0;
        for (RedisTrigger redisTrigger : dequeue(now)) {
            try {
                callouts.processNoThrow(redisTrigger);
            } finally {
                lastProcessed = max(lastProcessed, redisTrigger.getScheduledTime());
                if (redisTrigger.isRecurring() && redisTrigger.getNumRecurrences() > 1) {
                    scheduleWithFixedDelay(redisTrigger.next());
                }
            }
        }
        clear(lastProcessed);
    }

    long getMaxTimestampForDequeue() {
        return System.currentTimeMillis();
    }

    /**
     * @return a list of triggers ready to be processed, and if repeatable, should be re-enqueued.
     */
    List<RedisTrigger> dequeue(long maxScheduledTime) {
        try (Jedis jedis = redis.borrow()) {
            Set<String> items = jedis.zrangeByScore(getSchedulerKey(), defaultLookback(), maxScheduledTime);
            return  items.stream().map(item -> GSON.fromJson(item, RedisTrigger.class)).collect(Collectors.toList());
        }
    }

    private double defaultLookback() {
        return getMaxTimestampForDequeue() - TimeUnit.MINUTES.toMillis(10);
    }

    void clear(long lastProcessed) {
        try (Jedis jedis = redis.borrow()) {
            jedis.zremrangeByScore(getSchedulerKey(), 0, lastProcessed);
        }
    }
}
