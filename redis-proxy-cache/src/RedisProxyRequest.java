package src;

/**
 * A request to the Redis proxy.
 * Can be submitted to a RedisProxyConcurrentRunner instance concurrently,
 * but they will only be handled sequentially.
 * Override the run method with the content of the request
 */
public class RedisProxyRequest extends Thread {
    private final RedisProxy proxy;

    public RedisProxyRequest(RedisProxy proxy) {
        this.proxy = proxy;
    }
}