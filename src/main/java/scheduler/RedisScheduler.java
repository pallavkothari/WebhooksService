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
import java.util.function.Function;
import java.util.stream.Collectors;

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
        for (RedisTrigger redisTrigger : dequeue(now)) {
            try {
                callouts.processNoThrow(redisTrigger);
            } finally {
                if (redisTrigger.isRecurring()) {
                    scheduleWithFixedDelay(redisTrigger);
                }
            }
        }
        clear(now);
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
            Function<String, RedisTrigger> toTrigger = item -> GSON.fromJson(item, RedisTrigger.class);
            return items.stream().map(toTrigger.andThen(RedisTrigger::next)).collect(Collectors.toList());
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
