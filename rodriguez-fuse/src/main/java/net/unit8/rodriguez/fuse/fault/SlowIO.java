package net.unit8.rodriguez.fuse.fault;

import java.util.concurrent.TimeUnit;

public class SlowIO implements FuseFault {
    private long delayMs = 5000;

    public SlowIO() {
    }

    public SlowIO(long delayMs) {
        this.delayMs = delayMs;
    }

    @Override
    public int apply(int normalResult) {
        try {
            TimeUnit.MILLISECONDS.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return normalResult;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }
}
