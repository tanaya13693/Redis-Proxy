package src;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.*;

/**
 * A proxy for Redis that takes a capacity (keys to store) and a global expiry
 * for the items stored.
 */
public class RedisProxy {
    private final Jedis jedis;
    private final RedisProxyCache cache;

    public RedisProxy(
        String backingRedisAddr,
        int backingRedisPort,
        String backingRedisPass,
        int cacheCapacity,
        long globalExpiryMillis) {

        this.cache = new RedisProxyCache(cacheCapacity, globalExpiryMillis);

        this.jedis = new Jedis(backingRedisAddr, backingRedisPort);
        try {
            jedis.auth(backingRedisPass);
        }
        catch (JedisDataException e) { }
        jedis.connect();
    }

    /**
     * Set the Redis mapping to this key and value, bypassing cache
     */
    public void set(String key, String value) {
        jedis.set(key,value);
    }

    /**
     * Returns the value for the key in the Redis instance,
     * checking cache first and adding to cache if not in the cache
     */
    public String get(String key) {
        String cachedValue = this.cache.get(key);
        if (cachedValue != null) {
            // Value found in cache, moved to front
            return cachedValue;
        }
        else {
            // Value not in cache
            String value = jedis.get(key);
            if (value != null) {
                // Value is in redis but not cache, readding
                this.cache.set(key,value);
            }
            return value;
        }
    }

    /**
     * Passthroughs for methods in cache; see comments in cache class
     */
    public int cacheSize() {
        return this.cache.size();
    }
    public boolean cacheContainsValidEntry(String key) {
        return this.cache.containsValidEntry(key);
    }

    /*
     * Passthroughs for Jedis methods
     */
    /**
     * Returns "PONG" on successfully pinging the Redis instance
     */
    public String ping() {
        return jedis.ping();
    }

    /**
     * Deletes all key-value pairs stored in the backing Redis instance
     */
    public String flushDB() {
        return jedis.flushDB();
    }
}
