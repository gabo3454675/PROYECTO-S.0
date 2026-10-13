package avilaos.model.interfaces;

import avilaos.metrics.MetricsSnapshot;

public interface MetricsListener {
    void onMetricsUpdate(MetricsSnapshot snapshot);
}