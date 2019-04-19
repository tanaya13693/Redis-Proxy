# Redis Proxy Cache

## Design Overview

This is a proxy that caches GET requests for a single backing Redis instance. Any GET requests will first pass through the cache to determine whether it needs to call on the backing Redis instance. All SET requests bypass the proxy.

The cache is limited in size and is defined by two parameters, the capacity and the global expiry. The capacity defines how many keys can be stored in the cache, and the global expiry defines how long is allowed to pass before a key is expired, which is thereafter treated as if it weren't in the cache. When the cache is at capacity, items are evicted according to a least-recently-used policy.

Requests to the backing Redis instance can be sent sequentially with a concurrent runner class or with an instance of the proxy. Requests can be sent concurrently only with a concurrent runner class, although they will be handled sequentially.

## KT of Code

The cache has a doubly linked list field that maintains the order of usage of key-value pairs. The doubly linked list is treated like a queue that moves nodes to the front as they are used and cuts nodes off the back as they are evicted.

The cache is implemented as a HashMap from Strings to CacheNodes, which are the nodes in the doubly linked list described above. The cache thus stores its values purely as key-value pairs.

Concurrent request acceptance is implemented with a thread pool of size one (a single thread executor in the Java Concurrent library).

Added multiple instances connections through Jedis Pool configuration. This is bonus part for multiple instance connections. JedisUtilTest.java has unit test for the same.

## Instructions for running the proxy and tests

### Prerequisites

* Docker
* Docker-Compose
* Java 8
* Make

### Running code

All commands should be run from the project's root directory.

May be required on windows as this thread should be already running on computer where this can be tested. (In separate terminal window, run `docker daemon` in one terminal window)

In one terminal window, run `docker-compose up`.

In 2nd terminal window, run `make test`.

You should see the output from JUnit that reports the tests passed.


