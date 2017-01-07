package scheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Created by pallav.kothari on 1/7/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FuncTests {
    private static OkHttpClient client = new OkHttpClient();
    private static Gson gson = new GsonBuilder().serializeNulls().create();
    @LocalServerPort private int localServerPort;
    private long dequeueTimestamp = System.currentTimeMillis();

    @SpyBean
    private RedisScheduler scheduler;

    @MockBean
    private HttpCalloutService calloutService;

    @Before
    public void setup() {
        given(this.scheduler.getSchedulerKey())
                .willReturn(FuncTests.class.getSimpleName());
        given(this.scheduler.getMaxTimestampForDequeue())
                .willReturn(dequeueTimestamp);
    }

    @After
    public void tearDown() {
        scheduler.clear(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(100));
    }


    @Test
    public void scheduleSomething() throws IOException {
        RedisTrigger reqTrigger = trigger();
        Request req = new Request.Builder()
                .url(scheduleUrl())
                .post(RequestBody.create(mediaType(), gson.toJson(reqTrigger)))
                .build();

        Response response = client.newCall(req).execute();
        try (ResponseBody body = response.body()) {
            RedisTrigger respTrigger = gson.fromJson(body.string(), RedisTrigger.class);
            assertThat(reqTrigger, is(respTrigger));

            // now make sure something was actually scheduled
            verify(scheduler).schedule(eq(reqTrigger));

            // and also dequeue should work
            List<RedisTrigger> scheduled = scheduler.dequeue(reqTrigger.getScheduledTime());
            assertThat(scheduled.size(), is(1));
            assertThat(scheduled, Matchers.contains(reqTrigger));

            // when the queue is processed for realz, we expect a callout
            scheduler.process();
            verify(calloutService).processNoThrow(eq(reqTrigger));
            verify(scheduler).clear(eq(dequeueTimestamp));
        }
    }

    private RedisTrigger trigger() {
        try {
            RedisTrigger trigger = new RedisTrigger(
                    new URL("http://localhost:8080/callback"),
                    "myPayload",
                    dequeueTimestamp);
            return trigger;
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    private String scheduleUrl() {
        return String.format("http://localhost:%d/schedule", localServerPort);
    }

    private MediaType mediaType() {
        return MediaType.parse("application/json");
    }
}
