package moe.koseirin.nyanruaineo.utils.RedisUtils;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSON;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Redis value 序列化器（fastjson2）。
 * <p>
 * 安全约束：<b>不使用</b> {@code WriteClassName} 与 {@code SupportAutoType}。二者会让 Redis 内
 * 的 JSON 按 {@code @type} 字段实例化任意 classpath 类，构成 fastjson2 autoType 反序列化 RCE 面。
 * 本项目 Redis value 只存放 String / Boolean / JSON 对象（JSONObject/Map），按目标类型
 * {@code clazz} 做类型化反序列化即可，无需写入/还原类名。
 */
public class FastJson2JsonRedisSerializer<T> implements RedisSerializer<T> {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private final Class<T> clazz;

    public FastJson2JsonRedisSerializer(Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    @Override
    public byte @NonNull [] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        return JSON.toJSONString(t).getBytes(DEFAULT_CHARSET);
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String str = new String(bytes, DEFAULT_CHARSET);
        return JSON.parseObject(str, clazz);
    }
}
