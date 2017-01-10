package scheduler;

import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Created by pallav.kothari on 1/8/17.
 */
public class ProcessorTest extends BaseHttpTests {
    @SpyBean
    private Callback callback;

    @SpyBean
    private RedisScheduler scheduler;

    private CountDownLatch processorTestLatch = new CountDownLatch(3);
    private CountDownLatch demoLatch = new CountDownLatch(1);

    @Before
    public void setup() throws IOException {
        when(scheduler.getSchedulerKey()).thenReturn(ProcessorTest.class.getSimpleName());
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Response response = (Response) invocation.getArguments()[1];
                assertThat(response.body().string(), is("foo"));
                processorTestLatch.countDown();
                demoLatch.countDown();
                return null;
            }
        }).when(callback).onResponse(any(), any());
    }

    @Test
    public void testProcessorIsFiring() throws Exception {
        Webhook webhook = new Webhook(testWebhookUrl(), "foo", 1, TimeUnit.SECONDS, 3);
        try (ResponseBody body = schedule(webhook).body()) {
            Webhook respWebhook = gson.fromJson(body.string(), Webhook.class);
            assertThat(webhook, is(respWebhook));
        }
        assertTrue(processorTestLatch.await(5, TimeUnit.SECONDS));
    }
}
