package src;

import java.util.concurrent.*;

/**
 * Class used to run concurrent requests for the Redis proxy.
 * Requests can be accepted concurrently, but they will be handled sequentially.
 * Submit requests
 */
public class RedisProxyConcurrentRunner {
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    public RedisProxyConcurrentRunner() { }

    /**
     * Executes a RedisProxyRequest instance
     */
    public void execute(RedisProxyRequest request) {
        service.execute(request);
    }
}
        
