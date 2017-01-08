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
    public @ResponseBody RedisTrigger schedule(@RequestBody RedisTrigger trigger) {
        String queue = scheduler.schedule(trigger);
        return trigger;
    }

    @RequestMapping(value = "/callback", method = RequestMethod.POST)
    public void callback(@RequestBody String payload) {
    }
}
