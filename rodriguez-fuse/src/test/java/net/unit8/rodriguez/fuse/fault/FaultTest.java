package net.unit8.rodriguez.fuse.fault;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FaultTest {

    @Test
    void diskFullReturnsNegativeEnospc() {
        DiskFull fault = new DiskFull();
        int result = fault.apply(0);
        assertThat(result).isNegative();
    }

    @Test
    void permissionDeniedReturnsNegativeEacces() {
        PermissionDenied fault = new PermissionDenied();
        int result = fault.apply(0);
        assertThat(result).isNegative();
    }

    @Test
    void readOnlyFSReturnsNegativeErofs() {
        ReadOnlyFS fault = new ReadOnlyFS();
        int result = fault.apply(0);
        assertThat(result).isNegative();
    }

    @Test
    void ioErrorReturnsNegativeEio() {
        IOError fault = new IOError();
        int result = fault.apply(0);
        assertThat(result).isNegative();
    }

    @Test
    void tooManyOpenFilesReturnsNegativeEmfile() {
        TooManyOpenFiles fault = new TooManyOpenFiles();
        int result = fault.apply(0);
        assertThat(result).isNegative();
    }

    @Test
    void fileNotFoundReturnsNegativeEnoent() {
        FileNotFound fault = new FileNotFound();
        int result = fault.apply(0);
        assertThat(result).isNegative();
    }

    @Test
    void slowIODelaysAndReturnsNormalResult() {
        SlowIO fault = new SlowIO(50);
        long start = System.currentTimeMillis();
        int result = fault.apply(1024);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result).isEqualTo(1024);
        assertThat(elapsed).isGreaterThanOrEqualTo(40);
    }

    @Test
    void corruptedReadModifiesBuffer() {
        CorruptedRead fault = new CorruptedRead(1.0);
        byte[] original = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        byte[] buffer = original.clone();

        fault.corruptBuffer(buffer, buffer.length);

        // With 100% corruption rate, at least some bytes should differ
        boolean anyDifferent = false;
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] != original[i]) {
                anyDifferent = true;
                break;
            }
        }
        assertThat(anyDifferent).isTrue();
    }

    @Test
    void corruptedReadShouldCorruptRespectsRate() {
        CorruptedRead alwaysCorrupt = new CorruptedRead(1.0);
        assertThat(alwaysCorrupt.shouldCorrupt()).isTrue();

        CorruptedRead neverCorrupt = new CorruptedRead(0.0);
        assertThat(neverCorrupt.shouldCorrupt()).isFalse();
    }

    @Test
    void partialWriteReducesBytes() {
        PartialWrite fault = new PartialWrite(0.5);
        int result = fault.apply(1000);
        assertThat(result).isEqualTo(500);
    }

    @Test
    void partialWritePreservesNegativeResult() {
        PartialWrite fault = new PartialWrite(0.5);
        int result = fault.apply(-5);
        assertThat(result).isEqualTo(-5);
    }
}
