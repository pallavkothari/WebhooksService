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

/**
 * Created by pallav.kothari on 1/8/17.
 */
public class ProcessorTest extends BaseHttpTests {
    @SpyBean
    private Callback callback;

    private CountDownLatch latch;

    @Before
    public void setup() throws IOException {
        latch = new CountDownLatch(3);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Response response = (Response) invocation.getArguments()[1];
                assertThat(response.body().string(), is("foo"));
                latch.countDown();
                return null;
            }
        }).when(callback).onResponse(any(), any());
    }

    @Test
    public void testProcessorIsFiring() throws Exception {
        RedisTrigger trigger = new RedisTrigger(callbackUrl(), "foo", 0, 1, TimeUnit.SECONDS, 3);
        try (ResponseBody body = schedule(trigger).body()) {}
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
