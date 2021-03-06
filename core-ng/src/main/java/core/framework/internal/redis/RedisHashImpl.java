package core.framework.internal.redis;

import core.framework.internal.log.filter.ArrayLogParam;
import core.framework.internal.log.filter.MapLogParam;
import core.framework.internal.resource.PoolItem;
import core.framework.log.ActionLogContext;
import core.framework.redis.RedisHash;
import core.framework.util.Maps;
import core.framework.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import static core.framework.internal.redis.Protocol.Command.HDEL;
import static core.framework.internal.redis.Protocol.Command.HGET;
import static core.framework.internal.redis.Protocol.Command.HGETALL;
import static core.framework.internal.redis.Protocol.Command.HINCRBY;
import static core.framework.internal.redis.Protocol.Command.HMSET;
import static core.framework.internal.redis.Protocol.Command.HSET;
import static core.framework.internal.redis.RedisEncodings.decode;
import static core.framework.internal.redis.RedisEncodings.encode;

/**
 * @author neo
 */
public final class RedisHashImpl implements RedisHash {
    private final Logger logger = LoggerFactory.getLogger(RedisHashImpl.class);
    private final RedisImpl redis;

    RedisHashImpl(RedisImpl redis) {
        this.redis = redis;
    }

    @Override
    public String get(String key, String field) {
        var watch = new StopWatch();
        String value = null;
        PoolItem<RedisConnection> item = redis.pool.borrowItem();
        try {
            RedisConnection connection = item.resource;
            connection.writeKeyArgumentCommand(HGET, key, encode(field));
            value = decode(connection.readBlobString());
            return value;
        } catch (IOException e) {
            item.broken = true;
            throw new UncheckedIOException(e);
        } finally {
            redis.pool.returnItem(item);
            long elapsed = watch.elapsed();
            ActionLogContext.track("redis", elapsed, 1, 0);
            logger.debug("hget, key={}, field={}, returnedValue={}, elapsed={}", key, field, value, elapsed);
            redis.checkSlowOperation(elapsed);
        }
    }

    @Override
    public Map<String, String> getAll(String key) {
        var watch = new StopWatch();
        PoolItem<RedisConnection> item = redis.pool.borrowItem();
        Map<String, String> values = null;
        try {
            RedisConnection connection = item.resource;
            connection.writeKeyCommand(HGETALL, key);
            Object[] response = connection.readArray();
            if (response.length % 2 != 0) throw new IOException("unexpected length of array, length=" + response.length);
            values = Maps.newHashMapWithExpectedSize(response.length / 2);
            for (int i = 0; i < response.length; i += 2) {
                values.put(decode((byte[]) response[i]), decode((byte[]) response[i + 1]));
            }
            return values;
        } catch (IOException e) {
            item.broken = true;
            throw new UncheckedIOException(e);
        } finally {
            redis.pool.returnItem(item);
            long elapsed = watch.elapsed();
            ActionLogContext.track("redis", elapsed, values == null ? 0 : values.size(), 0);
            logger.debug("hgetAll, key={}, returnedValues={}, elapsed={}", key, values, elapsed);
            redis.checkSlowOperation(elapsed);
        }
    }

    @Override
    public void set(String key, String field, String value) {
        var watch = new StopWatch();
        PoolItem<RedisConnection> item = redis.pool.borrowItem();
        try {
            RedisConnection connection = item.resource;
            connection.writeArray(4);
            connection.writeBlobString(HSET);
            connection.writeBlobString(encode(key));
            connection.writeBlobString(encode(field));
            connection.writeBlobString(encode(value));
            connection.flush();
            connection.readLong();
        } catch (IOException e) {
            item.broken = true;
            throw new UncheckedIOException(e);
        } finally {
            redis.pool.returnItem(item);
            long elapsed = watch.elapsed();
            ActionLogContext.track("redis", elapsed, 0, 1);
            logger.debug("hset, key={}, field={}, value={}, elapsed={}", key, field, value, elapsed);
            redis.checkSlowOperation(elapsed);
        }
    }

    @Override
    public void multiSet(String key, Map<String, String> values) {
        var watch = new StopWatch();
        if (values.isEmpty()) throw new Error("values must not be empty");
        PoolItem<RedisConnection> item = redis.pool.borrowItem();
        try {
            RedisConnection connection = item.resource;
            connection.writeArray(2 + values.size() * 2);
            connection.writeBlobString(HMSET);
            connection.writeBlobString(encode(key));
            for (Map.Entry<String, String> entry : values.entrySet()) {
                connection.writeBlobString(encode(entry.getKey()));
                connection.writeBlobString(encode(entry.getValue()));
            }
            connection.flush();
            connection.readSimpleString();
        } catch (IOException e) {
            item.broken = true;
            throw new UncheckedIOException(e);
        } finally {
            redis.pool.returnItem(item);
            long elapsed = watch.elapsed();
            int size = values.size();
            ActionLogContext.track("redis", elapsed, 0, size);
            logger.debug("hmset, key={}, values={}, size={}, elapsed={}", key, new MapLogParam(values), size, elapsed);
            redis.checkSlowOperation(elapsed);
        }
    }

    @Override
    public long increaseBy(String key, String field, long increment) {
        var watch = new StopWatch();
        long value = 0;
        PoolItem<RedisConnection> item = redis.pool.borrowItem();
        try {
            RedisConnection connection = item.resource;
            connection.writeKeyArgumentsCommand(HINCRBY, key, field, String.valueOf(increment));
            value = connection.readLong();
            return value;
        } catch (IOException e) {
            item.broken = true;
            throw new UncheckedIOException(e);
        } finally {
            redis.pool.returnItem(item);
            long elapsed = watch.elapsed();
            ActionLogContext.track("redis", elapsed, 0, 1);
            logger.debug("hincrby, key={}, field={}, increment={}, returnedValue={}, elapsed={}", key, field, increment, value, elapsed);
            redis.checkSlowOperation(elapsed);
        }
    }

    @Override
    public long del(String key, String... fields) {
        var watch = new StopWatch();
        if (fields.length == 0) throw new Error("fields must not be empty");
        long deletedFields = 0;
        PoolItem<RedisConnection> item = redis.pool.borrowItem();
        try {
            RedisConnection connection = item.resource;
            connection.writeKeyArgumentsCommand(HDEL, key, fields);
            deletedFields = connection.readLong();
            return deletedFields;
        } catch (IOException e) {
            item.broken = true;
            throw new UncheckedIOException(e);
        } finally {
            redis.pool.returnItem(item);
            long elapsed = watch.elapsed();
            ActionLogContext.track("redis", elapsed, 0, (int) deletedFields);
            logger.debug("hdel, key={}, fields={}, size={}, deletedFields={}, elapsed={}", key, new ArrayLogParam(fields), fields.length, deletedFields, elapsed);
            redis.checkSlowOperation(elapsed);
        }
    }
}
