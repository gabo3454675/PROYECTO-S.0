package avilaos.util;

public final class IDGenerator {
    private static long nextId = 1;

    private IDGenerator() {
    }

    public static synchronized long getNextId() {
        return nextId++;
    }

    public static synchronized void reset() {
        nextId = 1;
    }

    public static synchronized long getCurrentId() {
        return nextId - 1;
    }
}