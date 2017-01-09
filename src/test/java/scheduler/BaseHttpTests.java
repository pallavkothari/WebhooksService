package scheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by pallav.kothari on 1/8/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseHttpTests {

    protected static Gson gson = new GsonBuilder().serializeNulls().create();
    private static OkHttpClient client = new OkHttpClient();
    @LocalServerPort private int localServerPort;

    private String scheduleUrl() {
        return String.format("http://localhost:%d/schedule", localServerPort);
    }

    protected Response schedule(RedisTrigger reqTrigger) throws IOException {
        Request req = new Request.Builder()
                .url(scheduleUrl())
                .post(RequestBody.create(MediaType.parse("application/json"), gson.toJson(reqTrigger)))
                .build();

        return client.newCall(req).execute();
    }

    protected URL callbackUrl() {
        try {
            return new URL(String.format("http://localhost:%d/callback", localServerPort));
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }
}
