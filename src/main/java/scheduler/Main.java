package scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
