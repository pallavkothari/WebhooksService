package scheduler;

import okhttp3.Callback;
import okhttp3.ResponseBody;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

/**
 * Created by pallav.kothari on 1/7/17.
 */
public class HttpTests extends BaseHttpTests {
    private long dequeueTimestamp = System.currentTimeMillis();

    @SpyBean
    private RedisScheduler scheduler;

    @SpyBean
    private HttpCalloutService calloutService;

    @SpyBean
    private Callback callback;

    private CountDownLatch latch;

    @Before
    public void setup() throws IOException {
        latch = new CountDownLatch(1);

        given(this.scheduler.getSchedulerKey())
                .willReturn(HttpTests.class.getSimpleName());
        given(this.scheduler.getMaxTimestampForDequeue())
                .willReturn(dequeueTimestamp);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(callback).onResponse(any(), any());
    }

    @After
    public void tearDown() {
        scheduler.clear(dequeueTimestamp);
    }


    @Test
    public void scheduleSomething() throws IOException, InterruptedException {
        Webhook req = new Webhook(testWebhookUrl(), "myPayload", dequeueTimestamp);
        try (ResponseBody body = schedule(req).body()) {
            Webhook resp = gson.fromJson(body.string(), Webhook.class);
            assertThat(req, is(resp));

            // now make sure something was actually scheduled
            verify(scheduler).schedule(eq(req));

            // and also dequeue should work
            List<Webhook> scheduled = scheduler.dequeue(req.getScheduledTime());
            assertThat(scheduled.size(), is(1));
            assertThat(scheduled, Matchers.contains(req));

            // when the queue is processed for realz, we expect a callout
            scheduler.process();
            verify(calloutService).processNoThrow(eq(req));
            verify(scheduler).clear(eq(dequeueTimestamp));

            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
    }
}
