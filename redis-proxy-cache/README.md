# Redis Proxy Cache

## High Level Design Overview

Clients interface to the Redis proxy through HTTP, with the Redis “GET” command. Proxy caches GET requests for a single backing Redis instance. We first try to get from cache and then route to backing instance if key is unavailable and then later warm up the cache for future requests in order to save response time. All SET requests bypass the proxy and go to backing instance.

The two parameters, the capacity and the global expiry are configurable through our application. When the cache is at capacity, items are evicted according to a least-recently-used policy. I used my own data structure to achieve this. We can easliy make use of redis.conf for the same.

Requests can be sent concurrently only with a concurrent runner class, although they will be handled sequentially as first part of implementation. 

Parallel processing through Jedis pool connections:
In separate Class JedisUtil I have also added pool connections configuration for Jedis Parallel concurrent processing as part of bonus flow

## KT of Code. What code does

Used Jedis client library to achieve interaction with Redis. We have other options as well like Lettuse and Reddison in java for same but most vesatile use case I found was in Jedis.

The cache has a list that maintains the order of usage of key-value pairs front and back of the list. The list is treated like a min heap datastructure and cuts nodes off the back as they are evicted.

The cache is implemented as a HashMap. The cache thus stores its values purely as key-value pairs.

I have used thread pool for concurrent request to cache proxy in my code.

# Parallel concurrent processing part
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

Make sure docker desktop is running on your local laptop/Desktop
May be required on windows as this thread should be already running on computer where this can be tested. (In separate terminal window, run `docker daemon` in one terminal window)

You should see the output from JUnit that reports the tests passed.

## Time breakdown
Understanding requirements: ~30 minutes

Setting up system, Redis, and Docker: ~2 hours

Developing all features of cache: ~3 hours

Time on unit tests: ~2 hours

Totak time for understanding Jedis Pool connections for parallel concurrent processing: ~1 hour

Documentation: ~1 hour

Validations: ~1 hour

Doing one more hobby project for SpringBoot Redis: ~3 hours
link : https://github.com/tanaya13693/SprintBoot-Redis-Proxy
tested endpoints of CRUD operations through postman.


