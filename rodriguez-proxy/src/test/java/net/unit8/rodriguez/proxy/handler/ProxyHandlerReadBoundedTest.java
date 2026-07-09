package net.unit8.rodriguez.proxy.handler;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link ProxyHandler#readBounded} enforces the byte limit while reading,
 * independently of any advertised Content-Length. This covers the chunked/unknown-length
 * request path that previously bypassed the size check.
 */
class ProxyHandlerReadBoundedTest {

    @Test
    void returnsBodyWhenWithinLimit() throws IOException {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] result = ProxyHandler.readBounded(new ByteArrayInputStream(data), 100);
        assertThat(result).isEqualTo(data);
    }

    @Test
    void returnsBodyAtExactLimit() throws IOException {
        byte[] data = new byte[64];
        byte[] result = ProxyHandler.readBounded(new ByteArrayInputStream(data), 64);
        assertThat(result).hasSize(64);
    }

    @Test
    void abortsWhenExceedingLimit() throws IOException {
        byte[] data = new byte[65];
        byte[] result = ProxyHandler.readBounded(new ByteArrayInputStream(data), 64);
        assertThat(result).isNull();
    }

    @Test
    void abortsUnknownLengthStreamWithoutBufferingEverything() throws IOException {
        // A stream that would produce far more than the limit; readBounded must stop early.
        long limit = 1024;
        InputStream endless = new InputStream() {
            @Override
            public int read() {
                return 0;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                return len; // always "fills" the buffer, i.e. never-ending body
            }
        };
        byte[] result = ProxyHandler.readBounded(endless, limit);
        assertThat(result).isNull();
    }
}
