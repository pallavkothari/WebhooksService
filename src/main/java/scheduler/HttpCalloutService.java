package scheduler;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by pallav.kothari on 1/6/17.
 */
@Service
public class HttpCalloutService {
    private static final OkHttpClient client = new OkHttpClient.Builder().build();
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "content-type";
    private static MediaType mediaType = MediaType.parse(APPLICATION_JSON);

    private final Callback callback;

    @Autowired
    public HttpCalloutService(Callback callback) {
        this.callback = callback;
    }

    public void processNoThrow(RedisTrigger redisTrigger) {
        try {
            Request request = new Request.Builder()
                    .url(redisTrigger.getCallback())
                    .post(RequestBody.create(mediaType, redisTrigger.getPayload()))
                    .addHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .build();
            client.newCall(request).enqueue(callback);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
