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
        Webhook webhook = new Webhook(new URL("http://example.com"), "myPayload", triggerTime);
        scheduler.schedule(webhook);
        List<Webhook> dequeued = scheduler.dequeue(triggerTime);
        assertThat(dequeued.get(0), is(webhook));
    }

    @Test
    public void testDequeueForRecurringWebhooks() throws MalformedURLException {
        Webhook webhook = new Webhook(
                new URL("http://example.com"),
                "myPayload",
                10, TimeUnit.MINUTES);
        scheduler.scheduleWithFixedDelay(webhook);
        long time = webhook.getScheduledTime();
        List<Webhook> dequeued = scheduler.dequeue(time);
        assertThat(dequeued.size(), is(1));
        assertThat(dequeued.get(0), is(webhook));
    }

    @Test
    public void testNext() throws MalformedURLException {
        Webhook webhook = new Webhook(
                new URL("http://example.com"),
                "myPayload",
                10, TimeUnit.MINUTES, 2);
        Webhook next = webhook.next();
        assertThat(next.getScheduledTime(), is(webhook.getScheduledTime() + TimeUnit.MINUTES.toMillis(10)));
        assertThat(next.getNumRecurrences(), is(1));
    }
    @Test
    public void testCalloutsForRecurringTasks() throws MalformedURLException {
        Webhook webhook = new Webhook(new URL("http://example.com"), "myPayload", 10, TimeUnit.MILLISECONDS, 3);
        scheduler.scheduleWithFixedDelay(webhook);

        for (int i = 0; i < 3; i++) {
            scheduler.process();
        }

        for (int i = 0; i < 3; i++) {
            verify(callouts).processNoThrow(eq(webhook));
            webhook = webhook.next();
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
