package net.unit8.rodriguez.fuse.fault;

public class PartialWrite implements FuseFault {
    private double writeRatio = 0.5;

    public PartialWrite() {
    }

    public PartialWrite(double writeRatio) {
        this.writeRatio = writeRatio;
    }

    @Override
    public int apply(int normalResult) {
        if (normalResult > 0) {
            return Math.max(1, (int) (normalResult * writeRatio));
        }
        return normalResult;
    }

    public double getWriteRatio() {
        return writeRatio;
    }

    public void setWriteRatio(double writeRatio) {
        this.writeRatio = writeRatio;
    }
}
