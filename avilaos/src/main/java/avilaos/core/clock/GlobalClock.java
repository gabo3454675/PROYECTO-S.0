package avilaos.core.clock;

import avilaos.model.interfaces.ClockListener;
import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;

public final class GlobalClock {
    private static GlobalClock instance;
    private final List<ClockListener> listeners;
    private int cycleDurationMs;
    private long currentCycle;
    private volatile boolean running;
    private Thread clockThread;

    private GlobalClock() {
        this.listeners = new LinkedList<>();
        this.cycleDurationMs = 1000;
        this.currentCycle = 0;
        this.running = false;
    }

    public static synchronized GlobalClock getInstance() {
        if (instance == null) {
            instance = new GlobalClock();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.stop();
            instance = new GlobalClock();
        }
    }

    public void addListener(ClockListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ClockListener listener) {
        listeners.remove(listener);
    }

    public void start() {
        if (running) return;
        running = true;
        clockThread = new Thread(this::runClock, "GlobalClock-Thread");
        clockThread.start();
    }

    public void stop() {
        running = false;
        if (clockThread != null) {
            clockThread.interrupt();
            try {
                clockThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void runClock() {
        while (running) {
            long startTime = System.currentTimeMillis();
            tick();
            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = cycleDurationMs - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void tick() {
        currentCycle++;
        for (ClockListener listener : listeners) {
            try {
                listener.onTick(currentCycle);
            } catch (Exception e) {
                System.err.println("Error en listener clock: " + e.getMessage());
            }
        }
    }

    public void setCycleDurationMs(int ms) {
        if (ms <= 0) throw new IllegalArgumentException("Duración ciclo > 0");
        this.cycleDurationMs = ms;
    }

    public int getCycleDurationMs() {
        return cycleDurationMs;
    }

    public long getCurrentCycle() {
        return currentCycle;
    }

    public boolean isRunning() {
        return running;
    }

    public void reset() {
        stop();
        currentCycle = 0;
    }
}