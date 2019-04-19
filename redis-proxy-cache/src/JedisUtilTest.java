package src;

import org.junit.Test; 

import redis.clients.jedis.Jedis; 
import redis.clients.jedis.JedisPool; 
import redis.clients.jedis.JedisPoolConfig; 
 
import fj.Effect; 
import fj.F; 
 
import static org.hamcrest.core.Is.is; 
import static org.hamcrest.core.IsSame.sameInstance; 
import static org.junit.Assert.assertThat; 
 
import static src.JedisUtil.using; 
 
/**
 * 
 * @author Tanaya Vadgave
 */ 
public class JedisUtilTest { 
     
    private static class CustomException extends RuntimeException { 
         
        private final Jedis jedis; 
         
        public CustomException(Jedis jedis) { 
            this.jedis = jedis; 
        } 
    } 
 
    @Test 
    public void test_that_connection_is_returned_to_pool() { 
    	System.out.println("Running test that connection is connected to same instance and is eventually returned to pool()");
        JedisPoolConfig config = new JedisPoolConfig(); 
         
        // Ensure that only a single connection is active in pool. 
        //config.setMaxActive(1); 
         
        final JedisPool pool = new JedisPool(config, "localhost", 6379); 
         
        // Use pool with Effect. 
        using(pool)._do(new Effect<Jedis>(){ 
 
            @Override 
            public void e(Jedis connection) { 
                // Do nothing. 
            } 
        }); 
         
        // Ensure that connection was returned, by retrieving the only connection in pool. 
        Jedis connection = pool.getResource(); 
        assertThat(connection.isConnected(), is(true)); 
        pool.returnResource(connection); 
         
        // Use pool with F. 
        Jedis returnedConnection = using(pool)._do(new F<Jedis, Jedis>() { 
 
            @Override 
            public Jedis f(Jedis connection) { 
                return connection; 
            } 
        }); 
         
        // Ensure that connection was returned, by retrieving the only connection in pool. 
        connection = pool.getResource(); 
        assertThat(connection.isConnected(), is(true)); 
        assertThat(connection, is(sameInstance(returnedConnection))); 
        pool.returnResource(connection); 
    } 
     
    @Test 
    public void test_that_connection_is_returned_to_pool_when_exception_occurs() { 
    	System.out.println("Running test that connection is returned to pool when exception occurs()");
        JedisPoolConfig config = new JedisPoolConfig(); 
         
        // Ensure that only a single connection is active in pool. 
        //config.setMaxActive(1); 
         
        final JedisPool pool = new JedisPool(config, "localhost", 6379); 
         
        // Use pool with Effect. 
        Jedis returnedConnection = null; 
        try { 
            using(pool)._do(new Effect<Jedis>(){ 
 
                @Override 
                public void e(Jedis connection) { 
                    throw new CustomException(connection); 
                } 
            }); 
        } catch (CustomException e) { 
            returnedConnection = e.jedis; 
        } 
         
        // Ensure that connection was returned, by retrieving the only connection in pool. 
        Jedis connection = pool.getResource(); 
        assertThat(connection.isConnected(), is(true)); 
        assertThat(connection, is(sameInstance(returnedConnection))); 
        pool.returnResource(connection); 
         
        // Use pool with F. 
        returnedConnection = null; 
        try { 
            using(pool)._do(new F<Jedis, Jedis>() { 
 
                @Override 
                public Jedis f(Jedis connection) { 
                    throw new CustomException(connection); 
                } 
            }); 
        } catch (CustomException e) { 
            returnedConnection = e.jedis; 
        } 
         
        // Ensure that connection was returned, by retrieving the only connection in pool. 
        connection = pool.getResource(); 
        assertThat(connection.isConnected(), is(true)); 
        assertThat(connection, is(sameInstance(returnedConnection))); 
        pool.returnResource(connection); 
    } 
}

