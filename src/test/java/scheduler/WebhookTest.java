package scheduler;

import org.junit.Test;

import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by pallav.kothari on 3/7/17.
 */
public class WebhookTest {
    @Test
    public void testToString() throws Exception {
        Webhook w1 = new Webhook(new URL("http://google.com"), "foo=bar", 0);
        assertThat(w1.toString(), is("Webhook{url=http://google.com, payload=foo=bar, scheduledTime=0, isRecurring=false, delay=0, timeUnit=null, numRecurrences=1}"));
    }

}