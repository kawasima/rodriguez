package net.unit8.rodriguez.fuse.fault;

import java.util.Random;

public class CorruptedRead implements FuseFault {
    private double corruptionRate = 0.1;
    private final Random random = new Random();

    public CorruptedRead() {
    }

    public CorruptedRead(double corruptionRate) {
        this.corruptionRate = corruptionRate;
    }

    @Override
    public int apply(int normalResult) {
        // normalResult is the number of bytes read; corruption is applied in FaultInjectionFS.read()
        return normalResult;
    }

    public boolean shouldCorrupt() {
        return random.nextDouble() < corruptionRate;
    }

    public void corruptBuffer(byte[] buf, int length) {
        int bytesToCorrupt = Math.max(1, (int) (length * corruptionRate));
        for (int i = 0; i < bytesToCorrupt; i++) {
            int pos = random.nextInt(length);
            buf[pos] = (byte) random.nextInt(256);
        }
    }

    public double getCorruptionRate() {
        return corruptionRate;
    }

    public void setCorruptionRate(double corruptionRate) {
        this.corruptionRate = corruptionRate;
    }
}
