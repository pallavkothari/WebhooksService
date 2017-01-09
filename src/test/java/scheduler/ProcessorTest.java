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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
        RedisTrigger trigger = new RedisTrigger(callbackUrl(), "foo", 0, 1, TimeUnit.SECONDS, 3);
        try (ResponseBody body = schedule(trigger).body()) {
            RedisTrigger respTrigger = gson.fromJson(body.string(), RedisTrigger.class);
            assertThat(trigger, is(respTrigger));
        }
        assertTrue(processorTestLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void demo() throws Exception {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.SECOND, 5);
        System.out.println("*** demo : Scheduling trigger to fire in 5 seconds at " + cal.getTime());
        RedisTrigger trigger = new RedisTrigger(callbackUrl(), "foo", cal.getTimeInMillis());
        try (ResponseBody body = schedule(trigger).body()) {}
        assertTrue(demoLatch.await(7, TimeUnit.SECONDS));
        System.out.println("*** demo : callback acked at " + new Date());
    }
}
