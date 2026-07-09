package net.unit8.rodriguez.fuse;

import jnr.ffi.Memory;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FaultInjectionFS} passthrough correctness, focusing on the
 * positioned read/write implementation and path-traversal containment.
 */
class FaultInjectionFSTest {

    private static final Runtime RUNTIME = Runtime.getSystemRuntime();

    private static Pointer buffer(byte[] data) {
        Pointer p = Memory.allocate(RUNTIME, data.length);
        p.put(0, data, 0, data.length);
        return p;
    }

    @Test
    void positionedWriteThenReadRoundTrips(@TempDir Path backing) {
        FaultInjectionFS fs = new FaultInjectionFS(backing, List.of());

        byte[] payload = "hello world".getBytes();
        int written = fs.write("/file.txt", buffer(payload), payload.length, 0, null);
        assertThat(written).isEqualTo(payload.length);

        Pointer readBuf = Memory.allocate(RUNTIME, payload.length);
        int read = fs.read("/file.txt", readBuf, payload.length, 0, null);
        assertThat(read).isEqualTo(payload.length);

        byte[] out = new byte[payload.length];
        readBuf.get(0, out, 0, payload.length);
        assertThat(out).isEqualTo(payload);
    }

    @Test
    void writeAtOffsetDoesNotRewriteWholeFile(@TempDir Path backing) throws IOException {
        FaultInjectionFS fs = new FaultInjectionFS(backing, List.of());

        byte[] head = "AAAA".getBytes();
        fs.write("/file.txt", buffer(head), head.length, 0, null);

        // Overwrite only bytes [1,3) at offset 1; the rest must be untouched.
        byte[] patch = "bb".getBytes();
        int written = fs.write("/file.txt", buffer(patch), patch.length, 1, null);
        assertThat(written).isEqualTo(patch.length);

        byte[] result = Files.readAllBytes(backing.resolve("file.txt"));
        assertThat(new String(result)).isEqualTo("AbbA");
    }

    @Test
    void writePastEndExtendsFile(@TempDir Path backing) {
        FaultInjectionFS fs = new FaultInjectionFS(backing, List.of());

        byte[] payload = "tail".getBytes();
        long offset = 4096;
        int written = fs.write("/sparse.bin", buffer(payload), payload.length, offset, null);
        assertThat(written).isEqualTo(payload.length);

        Pointer readBuf = Memory.allocate(RUNTIME, payload.length);
        int read = fs.read("/sparse.bin", readBuf, payload.length, offset, null);
        assertThat(read).isEqualTo(payload.length);

        byte[] out = new byte[payload.length];
        readBuf.get(0, out, 0, payload.length);
        assertThat(out).isEqualTo(payload);
    }

    @Test
    void readBeyondEndReturnsZero(@TempDir Path backing) {
        FaultInjectionFS fs = new FaultInjectionFS(backing, List.of());

        byte[] payload = "abc".getBytes();
        fs.write("/file.txt", buffer(payload), payload.length, 0, null);

        Pointer readBuf = Memory.allocate(RUNTIME, 16);
        int read = fs.read("/file.txt", readBuf, 16, 100, null);
        assertThat(read).isZero();
    }

    @Test
    void pathTraversalIsRejected(@TempDir Path backing) {
        FaultInjectionFS fs = new FaultInjectionFS(backing, List.of());

        // "../../etc/passwd" resolves outside the backing directory and must be rejected
        // before the stat struct is ever touched, so passing null is safe here.
        int result = fs.getattr("/../../etc/passwd", null);
        assertThat(result).isNegative();
    }
}
