package scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by pallav.kothari on 1/7/17.
 */
@RestController
public class RestEndpoints {

    @Autowired
    RedisScheduler scheduler;

    @RequestMapping("/")
    String index() {
        return "Hi, try scheduling something via /schedule";
    }

    @RequestMapping(value="/schedule", method = RequestMethod.POST)
    public @ResponseBody
    Webhook schedule(@RequestBody Webhook webhook) {
        String queue = scheduler.schedule(webhook);
        return webhook;
    }

    @RequestMapping(value = "/test-webhook", method = RequestMethod.POST)
    public String testHook(@RequestBody String payload) {
        System.out.println("payload = " + payload);
        return payload;
    }
}
