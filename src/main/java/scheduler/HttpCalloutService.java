package scheduler;

import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by pallav.kothari on 1/6/17.
 */
@Service
public class HttpCalloutService {
    private static final OkHttpClient client = new OkHttpClient.Builder().build();
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "content-type";
    private static final Callback RESPONSE_CALLBACK = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            // log / metric?
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            // just read the response fully to releaese resources
            try (ResponseBody body = response.body()) {
                if (response.isSuccessful()) {
                    // log / metric?
                }
            }
        }
    };
    static MediaType mediaType = MediaType.parse(APPLICATION_JSON);

    public void processNoThrow(RedisTrigger redisTrigger) {
        try {
            Request request = new Request.Builder()
                    .url(redisTrigger.getCallback())
                    .post(RequestBody.create(mediaType, redisTrigger.getPayload()))
                    .addHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .build();
            client.newCall(request).enqueue(RESPONSE_CALLBACK);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
