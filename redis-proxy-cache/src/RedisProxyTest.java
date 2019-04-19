package src;

import org.junit.*;
import static org.junit.Assert.*;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.*;

/**
 * Unit tests for the Redis proxy.
 */
public class RedisProxyTest {
    /**
     * Tests whether the proxy can connect to Redis
     */
    @Test
    public void testProxyCanConnect() {
        System.out.println("Running testProxyCanConnect");
        // connecting to default port 6379
        RedisProxy proxy = new RedisProxy("localhost", 6379, "testPassword", 0, 0);
        assertEquals(proxy.ping(), "PONG");
        proxy.flushDB();
    }

    /**
     * Test simple get/set cases
     */
    @Test
    public void testSimpleGetSet() {
        System.out.println("Running testSimpleGetSet");

        RedisProxy proxy = new RedisProxy("localhost", 6379, "testPassword", 10, 10000);

        assertFalse(proxy.cacheContainsValidEntry("newEntry"));
        proxy.set("newEntry","value");
        // warming the cache
        proxy.get("newEntry");
        //assert key present in cache
        assertTrue(proxy.cacheContainsValidEntry("newEntry"));
        //assert value is correct
        assertEquals(proxy.get("newEntry"), "value");
        proxy.flushDB();
    }

    /**
     * Test updating a key that is already present in the backing Redis
     */
    @Test
    public void testUpdateKey() {
        System.out.println("Running testUpdateKey");

        RedisProxy proxy = new RedisProxy("localhost", 6379, "testPassword", 10, 10000);

        assertFalse(proxy.cacheContainsValidEntry("a"));
        proxy.set("a","1");
        proxy.set("a","2");
        // assert updated value is present in cache
        assertEquals(proxy.get("a"), "2");
        assertEquals(proxy.cacheSize(), 1);
        proxy.flushDB();
    }

    /**
     * Test that state of the alive requests is correct when a request expires
     */
    @Test
    public void testSimpleTimeout() throws InterruptedException {
        System.out.println("Running testSimpleTimeout");

        RedisProxy proxy = new RedisProxy("localhost", 6379, "testPassword", 10, 100);

        assertFalse(proxy.cacheContainsValidEntry("a"));
        proxy.set("a","1");
        proxy.get("a");
        assertEquals(proxy.cacheSize(), 1);
        Thread.sleep(200);
        assertFalse(proxy.cacheContainsValidEntry("a"));
        proxy.flushDB();
    }

    /**
     * Test that state of the alive requests is correct when several earlier 
     * requests expire
     */
    @Test
    public void testDifferentTimeouts() throws InterruptedException {
        System.out.println("Running testDifferentTimeouts");

        RedisProxy proxy = new RedisProxy("localhost", 6379, "testPassword", 4, 100);

        proxy.set("a","1");
        proxy.get("a");
        proxy.set("b","2");
        proxy.get("b");
        proxy.set("c","3");
        proxy.get("c");
        proxy.set("d","4");
        proxy.get("d");
        assertEquals(proxy.cacheSize(), 4);
        // Cache should contain a,b,c,d (all 0 ms old)

        Thread.sleep(70);
        proxy.set("a","5");
        proxy.get("a");
        proxy.set("b","6");
        proxy.get("b");
        assertEquals(proxy.cacheSize(), 4);
        // Cache should contain a,b (0 ms old), c,d (70 ms old)

        Thread.sleep(70);
        proxy.set("e","5");
        proxy.get("e");
        proxy.set("f","6");
        proxy.get("f");
        assertEquals(proxy.cacheSize(), 4);
        // Cache should contain a,b (70 ms old), e,f (0 ms old) [c,d expired (140 ms old)]

        assertTrue(proxy.cacheContainsValidEntry("a"));
        assertTrue(proxy.cacheContainsValidEntry("b"));
        assertFalse(proxy.cacheContainsValidEntry("c"));
        assertFalse(proxy.cacheContainsValidEntry("d"));
        assertTrue(proxy.cacheContainsValidEntry("e"));
        assertTrue(proxy.cacheContainsValidEntry("f"));
        assertEquals(proxy.cacheSize(), 4);
        proxy.flushDB();
    }

    /**
     * Test that an exception is thrown when constructing with an invalid cache size
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCacheSize() {
        RedisProxy proxy = new RedisProxy("localhost", 6379, "testPassword", -1, 10000);
    }

    /**
     * Test that an exception is thrown when constructing with an invalid expiry
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidExpiry() {
        RedisProxy proxy = new RedisProxy("localhost", 6379, "testPassword", 10, -1);
    }

    /**
     * Test that we can still access items in the backing Redis regardless of the
     * presence in the cache
     */
    @Test
    public void testOverflowCache() {
        System.out.println("Running testOverflowCache");

        RedisProxy proxy = new RedisProxy("localhost", 6379, "testPassword", 1, 10000);
        
        assertFalse(proxy.cacheContainsValidEntry("a"));
        assertFalse(proxy.cacheContainsValidEntry("b"));
        proxy.set("a","1");
        proxy.get("a");
        assertTrue(proxy.cacheContainsValidEntry("a"));
        assertEquals(proxy.cacheSize(), 1);
        proxy.set("b","2");
        proxy.get("b");
        assertTrue(proxy.cacheContainsValidEntry("b"));
        assertFalse(proxy.cacheContainsValidEntry("a"));
        assertEquals(proxy.cacheSize(), 1);

        // Cache is full but we can still get items stored earlier
        assertEquals(proxy.get("a"), "1");
        assertEquals(proxy.get("b"), "2");
        proxy.flushDB();
    }

    /**
     * Test LRU eviction order
     */
    @Test
    public void testEvictionOrder() {
        System.out.println("Running testEvictionOrder for LRU Algorithm");

        RedisProxy proxy = new RedisProxy("localhost", 6379, "testPassword", 4, 10000);

        proxy.set("a","1");
        proxy.get("a");
        proxy.set("b","2");
        proxy.get("b");
        proxy.set("c","3");
        proxy.get("c");
        proxy.set("d","4");
        proxy.get("d");
        proxy.set("e","5");
        proxy.get("e");
        // Cache should contain FRONT e d c b BACK
        assertTrue(proxy.cacheContainsValidEntry("e"));
        assertTrue(proxy.cacheContainsValidEntry("d"));
        assertTrue(proxy.cacheContainsValidEntry("c"));
        assertTrue(proxy.cacheContainsValidEntry("b"));
        assertFalse(proxy.cacheContainsValidEntry("a"));
        assertEquals(proxy.cacheSize(), 4);

        proxy.get("c");
        proxy.set("c","10");
        proxy.get("d");
        proxy.get("b");
        // Cache should contain FRONT b d c e BACK
        proxy.get("a");
        proxy.set("f","12");
        proxy.get("f");
        // Cache should contain FRONT f a b d BACK
        assertTrue(proxy.cacheContainsValidEntry("f"));
        assertTrue(proxy.cacheContainsValidEntry("a"));
        assertTrue(proxy.cacheContainsValidEntry("b"));
        assertTrue(proxy.cacheContainsValidEntry("d"));
        assertFalse(proxy.cacheContainsValidEntry("e"));
        assertFalse(proxy.cacheContainsValidEntry("c"));
        assertEquals(proxy.cacheSize(), 4);

        proxy.flushDB();
    }

    /**
     * Test that concurrent clients can use the proxy, but their requests will be
     * handled sequentially
     */
    @Test
    public void testConcurrentClients() throws InterruptedException {
        System.out.println("Running testConcurrentClients");

        RedisProxy proxy = new RedisProxy("localhost", 6379, "testPassword", 4, 100);

        RedisProxyRequest request1 = new RedisProxyRequest(proxy) {
            @Override
            public void run() {
                proxy.set("a","1");
                proxy.set("b","2");
                proxy.set("c","3");
            }
        };
        RedisProxyRequest request2 = new RedisProxyRequest(proxy) {
            @Override
            public void run() {
                proxy.set("a","4");
                proxy.set("b","5");
                proxy.set("c","6");
            }
        };
        RedisProxyRequest request3 = new RedisProxyRequest(proxy) {
            @Override
            public void run() {
                proxy.set("a","7");
                proxy.set("b","8");
                proxy.set("c","9");
            }
        };

        RedisProxyConcurrentRunner runner = new RedisProxyConcurrentRunner();
        runner.execute(request1);
        runner.execute(request2);
        runner.execute(request3);
        Thread.sleep(500);
        assertEquals(proxy.get("a"),"7");
        assertEquals(proxy.get("b"),"8");
        assertEquals(proxy.get("c"),"9");

        runner.execute(request3);
        runner.execute(request2);
        runner.execute(request1);
        Thread.sleep(500);
        assertEquals(proxy.get("a"),"1");
        assertEquals(proxy.get("b"),"2");
        assertEquals(proxy.get("c"),"3");

        proxy.flushDB();
    }
}
