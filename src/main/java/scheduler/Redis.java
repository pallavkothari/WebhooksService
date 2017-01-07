package scheduler;

import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Created by pallav.kothari on 12/6/16.
 */
@Component
public class Redis {
    private JedisPool pool;

    public Redis() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestOnCreate(true);
        try {
            pool = new JedisPool(poolConfig, new URI(redisUrl()));
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
        try (Jedis jedis = pool.getResource()) {
            jedis.set("foo", "bar");
            jedis.expire("foo", 1);
            System.out.println("Redis pool initialized.");
        }
    }

    /**
     * please call in a try-with-resources
     */
    public Jedis borrow() {
        return pool.getResource();
    }

    private String redisUrl() {
        return Optional.ofNullable(System.getenv("REDIS_URL")).orElse("redis://localhost:6379");
    }
}
