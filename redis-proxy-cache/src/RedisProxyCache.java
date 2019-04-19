package src;

import java.util.*;

/**
 * Doubly linked list node that is the value of some key in the cache and
 * also a node in the doubly linked list keeping track of usage order
 */
class CacheNode {
    public CacheNode prev;
    public CacheNode next;
    public final String key;
    public String value;
    public long lastUsedTimeMillis;

    public CacheNode(String key, String value) {
        this.key = key;
        this.value = value;
        this.lastUsedTimeMillis = System.currentTimeMillis();
    }
}

/**
 * Redis proxy cache, which stores the given capacity of keys that expire after
 * the given number of milliseconds
 */
public class RedisProxyCache {
    /**
     * The front and back of the recently used list, where the front is the
     * most recently used and the back is the least recently used
     */
    private CacheNode recentlyUsedFront;
    private CacheNode recentlyUsedBack;

    /**
     * The hash map that maps keys to their nodes in the recently used list
     */
    private HashMap<String, CacheNode> cache;

    /**
     * The number of keys the cache can store
     */
    private int capacity;

    /**
     * The number of milliseconds after which a key expires
     */
    private long globalExpiryMillis;

    public RedisProxyCache(int capacity, long globalExpiryMillis) throws IllegalArgumentException {
        if (capacity < 0) {
            throw new IllegalArgumentException("Invalid cache capacity");
        }
        if (globalExpiryMillis < 0) {
            throw new IllegalArgumentException("Global expiry cannot be negative");
        }

        this.cache = new HashMap<>();
        this.recentlyUsedFront = null;
        this.recentlyUsedBack = null;
        this.capacity = capacity;
        this.globalExpiryMillis = globalExpiryMillis;
    }

    /**
     * Set the cache's mapping to this key and value
     */
    public void set(String key, String value) {
        CacheNode getResult = this.cache.get(key);

        // If already in the cache, just update
        if (getResult != null) {
            getResult.value = value;
            moveToFront(getResult);
        }
        // If not already in the cache, we need to ensure we have space
        else {
            // If cache is full, see if we can clear stale entries first
            if (this.cache.size() == this.capacity) {
                clearStaleEntries();
            }
            // If still full, evict the least recently used item
            if (this.cache.size() == this.capacity) {
                evictLRU();
            }

            CacheNode node = new CacheNode(key, value);
            addToFront(node);
        }
    }

    /**
     * Returns the value for the key in the cache
     */
    public String get(String key) {
        CacheNode getResult = this.cache.get(key);

        if (getResult != null) {
            // If key is in the cache but the entry is stale,
            // pretend it doesn't exist and get rid of it
            if (isStale(getResult)) {
                removeNode(getResult);
                return null;
            }
            else {
                moveToFront(getResult);
                return getResult.value;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Helper method that removes the given node from the recently used list
     * and returns it so that it can be used to clearStaleEntries, evictLRU and moveToFront
     */
    private CacheNode removeNode(CacheNode node) {
        this.cache.remove(node.key);

        if (node == recentlyUsedFront && node == recentlyUsedBack) {
            recentlyUsedFront = null;
            recentlyUsedBack = null;
            node.prev = null;
            node.next = null;
            return node;
        }
        if (node == recentlyUsedFront) {
            recentlyUsedFront = node.next;
            recentlyUsedFront.prev = null;
            node.prev = null;
            node.next = null;
            return node;
        }
        if (node == recentlyUsedBack) {
            recentlyUsedBack = node.prev;
            recentlyUsedBack.next = null;
            node.prev = null;
            node.next = null;
            return node;
        }

        CacheNode oldPrev = node.prev;
        CacheNode oldNext = node.next;
        oldPrev.next = oldNext;
        oldNext.prev = oldPrev;
        node.prev = null;
        node.next = null;
        return node;
    }

    /**
     * Helper method that adds the given node to the front of the recently used list 
     * and updates its timestamp to now
     */
    private void addToFront(CacheNode node) {
        this.cache.put(node.key, node);
        node.lastUsedTimeMillis = System.currentTimeMillis();

        if (recentlyUsedFront == null && recentlyUsedBack == null) {
            recentlyUsedFront = node;
            recentlyUsedBack = node;
            node.prev = null;
            node.next = null;
            return;
        }
        recentlyUsedFront.prev = node;
        node.next = recentlyUsedFront;
        node.prev = null;
        recentlyUsedFront = node;
    }

    /**
     * Helper method that moves a given node to the front of the recently used list
     */
    private void moveToFront(CacheNode node) {
        addToFront(removeNode(node));
    }

    /**
     * Helper method that removes the last (least-recently-used) node of the recently
     * used list
     */
    private void evictLRU() {
        removeNode(recentlyUsedBack);
    }

    /**
     * Returns true if the given node is stale (expired)
     */
    private boolean isStale(CacheNode node) {
        return (System.currentTimeMillis() - node.lastUsedTimeMillis) > this.globalExpiryMillis;
    }

    /**
     * Removes all stale nodes from the recently used list
     */
    private void clearStaleEntries() {
        if (recentlyUsedFront == null && recentlyUsedBack == null) {
            return;
        }

        if (recentlyUsedFront == recentlyUsedBack && recentlyUsedFront != null) {
            if (isStale(recentlyUsedFront)) {
                recentlyUsedFront = null;
                recentlyUsedBack = null;
            }
            return;
        }

        // Remove stale nodes from the back
        // This works because moving to front updates timestamps, so the recently
        // used linked list is ordered in increasing order of age from front to back
        while (recentlyUsedBack != null && isStale(recentlyUsedBack)) {
            removeNode(recentlyUsedBack);
        }
    }

    /**
     * Returns true if the cache contains a value that would be returned by
     * get() for this key.
     * This may return false for entries that are still in the cache.
     */
    public boolean containsValidEntry(String key) {
        CacheNode getResult = this.cache.get(key);
        return (getResult != null && !isStale(getResult));
    }

    /**
     * Returns the number of elements stored in the cache
     * Warning: as entries go stale, the value returned loses its meaning.
     * This is only guaranteed to be up to date until the most recent get/set.
     */
    public int size() {
        return this.cache.size();
    }
}