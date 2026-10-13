package avilaos.core.hardware;

public class RAM {
    private final long totalSize;
    private long usedSize;

    public RAM(long totalSizeMB) {
        if (totalSizeMB <= 0) throw new IllegalArgumentException("RAM size > 0");
        this.totalSize = totalSizeMB * 1024 * 1024;
        this.usedSize = 0;
    }

    public boolean allocate(long sizeBytes) {
        if (sizeBytes <= 0) return false;
        if (usedSize + sizeBytes <= totalSize) {
            usedSize += sizeBytes;
            return true;
        }
        return false;
    }

    public void free(long sizeBytes) {
        if (sizeBytes > 0) {
            usedSize = Math.max(0, usedSize - sizeBytes);
        }
    }

    public boolean canAllocate(long sizeBytes) {
        return sizeBytes > 0 && usedSize + sizeBytes <= totalSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getUsedSize() {
        return usedSize;
    }

    public long getFreeSize() {
        return totalSize - usedSize;
    }

    public double getUsagePercentage() {
        return totalSize > 0 ? (double) usedSize / totalSize * 100.0 : 0.0;
    }

    public void setUsedSize(long usedSize) {
        this.usedSize = Math.max(0, Math.min(usedSize, totalSize));
    }
}