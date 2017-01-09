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
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

/**
 * Created by pallav.kothari on 1/6/17.
 */
public class SchedulerTests {

    private Redis redis = new Redis();

    private final HttpCalloutService callouts = mock(HttpCalloutService.class);
    RedisScheduler scheduler = new RedisScheduler(callouts, redis) {
        @Override
        String getSchedulerKey() {
            return SchedulerTests.class.getSimpleName();
        }

        @Override
        long getMaxTimestampForDequeue() {
            return System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        }
    };

    @After
    public void tearDown() {
        scheduler.clear(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(100));
    }

    @Test
    public void testDequeue() throws MalformedURLException {
        long triggerTime = System.currentTimeMillis();
        RedisTrigger trigger = new RedisTrigger(new URL("http://example.com"), "myPayload", triggerTime);
        scheduler.schedule(trigger);
        List<RedisTrigger> dequeued = scheduler.dequeue(triggerTime);
        assertThat(dequeued.get(0), is(trigger));
    }

    @Test
    public void testDequeueForRecurringTriggers() throws MalformedURLException {
        RedisTrigger trigger = new RedisTrigger(
                new URL("http://example.com"),
                "myPayload",
                10, TimeUnit.MINUTES);
        scheduler.scheduleWithFixedDelay(trigger);
        long time = trigger.getScheduledTime();
        List<RedisTrigger> dequeued = scheduler.dequeue(time);
        assertThat(dequeued.size(), is(1));
        assertThat(dequeued.get(0), is(trigger));
    }

    @Test
    public void testNext() throws MalformedURLException {
        RedisTrigger trigger = new RedisTrigger(
                new URL("http://example.com"),
                "myPayload",
                10, TimeUnit.MINUTES, 2);
        RedisTrigger next = trigger.next();
        assertThat(next.getScheduledTime(), is(trigger.getScheduledTime() + TimeUnit.MINUTES.toMillis(10)));
        assertThat(next.getNumRecurrences(), is(1));
    }
    @Test
    public void testCalloutsForRecurringTasks() throws MalformedURLException {
        RedisTrigger trigger = new RedisTrigger(new URL("http://example.com"), "myPayload", 10, TimeUnit.MILLISECONDS, 3);
        scheduler.scheduleWithFixedDelay(trigger);

        for (int i = 0; i < 3; i++) {
            scheduler.process();
        }

        for (int i = 0; i < 3; i++) {
            verify(callouts).processNoThrow(eq(trigger));
            trigger = trigger.next();
        }
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
