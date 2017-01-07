package scheduler;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

/**
 * Created by pallav.kothari on 1/6/17.
 */
public class Tests {

    private Redis redis = new Redis();

    RedisScheduler scheduler = new RedisScheduler(new HttpCalloutService(), redis) {
        @Override
        String getSchedulerKey() {
            return Tests.class.getSimpleName();
        }
    };

    @After
    public void tearDown() {
        scheduler.clear(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(100));
    }

    @Test
    public void testNonRepeatedTrigger() throws MalformedURLException {
        long triggerTime = System.currentTimeMillis();
        RedisTrigger trigger = new RedisTrigger(new URL("http://example.com"), "myPayload", triggerTime);
        scheduler.schedule(trigger);
        List<RedisTrigger> dequeued = scheduler.dequeue(triggerTime);
        assertThat(dequeued.get(0), is(trigger));
    }

    @Test
    public void testDelaysForRepeatableTriggers() throws MalformedURLException {
        RedisTrigger trigger = new RedisTrigger(
                new URL("http://example.com"),
                "myPayload",
                0, 10, TimeUnit.MINUTES);
        scheduler.scheduleWithFixedDelay(trigger);
        long time = trigger.getScheduledTime();
        List<RedisTrigger> dequeued = scheduler.dequeue(time);
        assertThat(dequeued.size(), is(1));
        assertThat(dequeued.get(0), is(not(trigger)));
        assertThat(dequeued.get(0).getScheduledTime(), is(trigger.nextScheduledTime()));
        assertThat(dequeued.get(0), is(trigger.next()));
    }

    @Test
    public void testZSets() {
        String key = "key";
        long currentTimeMillis = System.currentTimeMillis();
        try (Jedis jedis = redis.borrow()) {
            jedis.zadd(key, currentTimeMillis, "first");
            jedis.zadd(key, currentTimeMillis + 1, "second");
            jedis.zadd(key, currentTimeMillis - 1, "third");
            // check that items returned in order
            Set<String> firstRange = jedis.zrangeByScore(key, 0, currentTimeMillis);
            assertThat(Lists.newArrayList(firstRange),
                    is(Lists.newArrayList("third", "first")));
            assertThat(jedis.zrangeByScore(key, 0, currentTimeMillis), is(firstRange));
            jedis.zremrangeByScore(key, 0, currentTimeMillis);
            assertThat(jedis.zrangeByScore(key, 0, currentTimeMillis).size(), is(0));
        }
    }
}
