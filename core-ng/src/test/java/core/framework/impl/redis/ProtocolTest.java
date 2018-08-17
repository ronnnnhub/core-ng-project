package core.framework.impl.redis;

import core.framework.util.Strings;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static core.framework.impl.redis.Protocol.Command.SET;
import static core.framework.impl.redis.RedisEncodings.decode;
import static core.framework.impl.redis.RedisEncodings.encode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author neo
 */
class ProtocolTest {
    @Test
    void write() throws IOException {
        var stream = new ByteArrayOutputStream();
        Protocol.write(new RedisOutputStream(stream, 8192), SET, encode("k1"), encode("v1"));
        assertThat(decode(stream.toByteArray())).isEqualTo("*3\r\n$3\r\nSET\r\n$2\r\nk1\r\n$2\r\nv1\r\n");
    }

    @Test
    void readError() {
        var stream = new ByteArrayInputStream(Strings.bytes("-error-message\r\n"));
        assertThatThrownBy(() -> Protocol.read(new RedisInputStream(stream)))
                .isInstanceOf(RedisException.class)
                .hasMessage("error-message");
    }

    @Test
    void readArray() throws IOException {
        var stream = new ByteArrayInputStream(Strings.bytes("*3\r\n$1\r\n1\r\n$1\r\n2\r\n$1\r\n3\r\n"));
        Object[] response = (Object[]) Protocol.read(new RedisInputStream(stream));
        assertThat(response).containsExactly(encode("1"), encode("2"), encode("3"));
    }

    @Test
    void readSimpleString() throws IOException {
        var stream = new ByteArrayInputStream(Strings.bytes("+OK\r\n"));
        String response = (String) Protocol.read(new RedisInputStream(stream));
        assertThat(response).isEqualTo("OK");
    }

    @Test
    void readLong() throws IOException {
        var stream = new ByteArrayInputStream(Strings.bytes(":10\r\n"));
        long response = (Long) Protocol.read(new RedisInputStream(stream));
        assertThat(response).isEqualTo(10);
    }

    @Test
    void readNullString() throws IOException {
        var stream = new ByteArrayInputStream(Strings.bytes("$-1\r\n"));
        byte[] response = (byte[]) Protocol.read(new RedisInputStream(stream));
        assertThat(response).isNull();
    }

    @Test
    void readEmptyString() throws IOException {
        var stream = new ByteArrayInputStream(Strings.bytes("$0\r\n\r\n"));
        byte[] response = (byte[]) Protocol.read(new RedisInputStream(stream));
        assertThat(decode(response)).isEmpty();
    }
}
