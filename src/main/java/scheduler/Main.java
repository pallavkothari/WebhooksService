package scheduler;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

/**
 * Created by pallav.kothari on 1/7/17.
 */
@SpringBootApplication
public class Main {

    @Autowired Processor processor;

    @Bean
    CommandLineRunner init() {
        return args -> {
            processor.init();
        };
    }

    @Bean
    public Callback okhttpCallback() {
        return new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {}
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
