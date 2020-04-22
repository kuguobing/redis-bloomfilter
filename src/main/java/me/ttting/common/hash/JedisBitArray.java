package me.ttting.common.hash;

import com.sun.org.apache.regexp.internal.REUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.util.List;

/**
 * Created by jiangtiteng
 */
public class JedisBitArray implements BitArray {
    @FunctionalInterface
    public interface JedisRunable {
        Object run(Jedis jedis);
    }

    private JedisPool jedisPool;

    private String key;

    private long bitSize;

    public static final long MAX_REDIS_BIT_SIZE = 4294967296L;

    public static final String REDIS_PREFIX = "BLOOM_FILTER_";

    private JedisBitArray() {
    }

    public JedisBitArray(JedisPool jedisPool, String prefix) {
        this.jedisPool = jedisPool;
        this.key = REDIS_PREFIX + key;
    }

    private Object execute(JedisRunable runnable) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return runnable.run(jedis);
        } catch (Exception e) {
            throw e;
        } finally {
            if (jedis != null)
                jedis.close();
        }
    }

    @Override
    public void setBitSize(long bitSize) {
        if (bitSize > MAX_REDIS_BIT_SIZE)
            throw new IllegalArgumentException("Invalid redis bit size, must small than 2 to the 32");

        this.bitSize = bitSize;
    }

    @Override
    public boolean set(long index) {
        boolean result;
        result = (Boolean) execute(jedis -> {
            Boolean setbit = jedis.setbit(key, index, true);
            return !setbit;
        });
        return result;
    }

    @Override
    public boolean get(long index) {
        boolean result;
        result = (Boolean) execute(jedis -> jedis.getbit(key, index));
        return result;
    }

    @Override
    public long bitSize() {
        return this.bitSize;
    }

    @Override
    public List<Boolean> batchGet(List<Long> indexs) {

        return (List<Boolean>) execute(jedis -> {
            Pipeline pipeline = jedis.pipelined();
            indexs.forEach(index -> pipeline.getbit(key, index));
            return pipeline.syncAndReturnAll();
        });
    }

    @Override
    public List<Boolean> batchSet(List<Long> indexs) {
        return (List<Boolean>) execute(jedis -> {
            Pipeline pipeline = jedis.pipelined();
            indexs.forEach(index -> pipeline.setbit(key, index, true));
            return pipeline.syncAndReturnAll();
        });
    }
}
