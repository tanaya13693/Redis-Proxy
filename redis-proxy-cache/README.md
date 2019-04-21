# Redis Proxy Cache

## High Level Design Overview

This is a proxy that caches GET requests for a single backing Redis instance. Any GET requests will first pass through the cache to determine whether it needs to call on the backing Redis instance. All SET requests bypass the proxy.

The cache is limited in size and is defined by two parameters, the capacity and the global expiry. The capacity defines how many keys can be stored in the cache, and the global expiry defines how long is allowed to pass before a key is expired, which is thereafter treated as if it weren't in the cache. When the cache is at capacity, items are evicted according to a least-recently-used policy.

Requests to the backing Redis instance can be sent sequentially with a concurrent runner class or with an instance of the proxy. Requests can be sent concurrently only with a concurrent runner class, although they will be handled sequentially. In separate Class JedisUtil I have also added pool connections configuration for Jedis Parallel concurrent processing as part of bonus flow

## KT of Code. What code does

The cache has a doubly linked list field that maintains the order of usage of key-value pairs. The doubly linked list is treated like a queue that moves nodes to the front as they are used and cuts nodes off the back as they are evicted.

The cache is implemented as a HashMap from Strings to CacheNodes, which are the nodes in the doubly linked list described above. The cache thus stores its values purely as key-value pairs.

Concurrent request acceptance is implemented with a thread pool of size one (a single thread executor in the Java Concurrent library).

## Parallel concurrent processing part
Added multiple instances connections through Jedis Pool configuration. This is bonus part for parallel processing for multiple request comming at same time. JedisUtilTest.java has unit test for the same.
I have used poll configurations here so that separate requests do not adversely affect functional behaviour. We can easily set max pool limit.

### Prerequisites

* Docker
* Docker-Compose
* Java 8
* Make

## Instructions for running the proxy and tests

1st Window (Terminal)
1. git clone https://github.com/tanaya13693/Redis-Proxy.git
2. cd Redis-Proxy/redis-proxy-cache/

2nd window 
1. cd Redis-Proxy/redis-proxy-cache/
2. docker-compose up

You should see message = "Ready to accept connections"

Now go to 1st window and run following command
make test 


May be required on windows as this thread should be already running on computer where this can be tested. (In separate terminal window, run `docker daemon` in one terminal window)

You should see the output from JUnit that reports the tests passed.

## Time breakdown
Understanding requirements: ~30 minutes

Setting up system, Redis, and Docker: ~2 hours

Total time on all features of cache: ~3 hours

Total time on unit tests: ~2 hours

Totak time for understanding Jedis Pool connections for parallel concurrent processing: ~1 hour

Documentation: ~1 hour

Revision: ~1 hour

Doing one more hobby project for SpringBoot Redis: ~3 hours
link : 

## Unimplemented requirements
My code only stores strings as values, instead of storing general data structures, mostly because of time issues.
