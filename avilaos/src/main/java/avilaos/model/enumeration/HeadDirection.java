package avilaos.model.enumeration;

/**
 * Dirección de movimiento del cabezal del disco.
 */
public enum HeadDirection {
    UP("Hacia bloques mayores", "↑", 1),
    DOWN("Hacia bloques menores", "↓", -1);

    private final String displayName;
    private final String symbol;
    private final int step;

    HeadDirection(String displayName, String symbol, int step) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.step = step;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    /** +1 si el cabezal avanza hacia bloques mayores, -1 si retrocede. */
    public int getStep() {
        return step;
    }

    public HeadDirection opposite() {
        return this == UP ? DOWN : UP;
    }
}
