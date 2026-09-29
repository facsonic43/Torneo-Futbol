package model.match;

public enum Formation {
    FOUR_FOUR_TWO("4-4-2", 4, 4, 2),
    FOUR_THREE_THREE("4-3-3", 4, 3, 3),
    FOUR_FIVE_ONE("4-5-1", 4, 5, 1),
    THREE_FIVE_TWO("3-5-2", 3, 5, 2),
    FOUR_ONE_TWO_ONE_TWO("4-1-2-1-2", 4, 4, 2),
    FOUR_TWO_THREE_ONE("4-2-3-1", 4, 5, 1),
    FOUR_FOUR_ONE_ONE("4-4-1-1", 4, 5, 1),
    THREE_FOUR_THREE("3-4-3", 3, 4, 3),
    FIVE_FOUR_ONE("5-4-1", 5, 4, 1),
    THREE_FIVE_ONE_ONE("3-5-1-1", 3, 6, 1),
    FOUR_ONE_FOUR_ONE("4-1-4-1", 4, 5, 1),
    FOUR_THREE_TWO_ONE("4-3-2-1", 4, 5, 1),
    FOUR_ONE_TWO_THREE("4-1-2-3", 4, 3, 3),
    FIVE_THREE_TWO("5-3-2", 5, 3, 2),
    FIVE_TWO_THREE("5-2-3", 5, 2, 3),
    THREE_FOUR_ONE_TWO("3-4-1-2", 3, 5, 2),
    FOUR_TWO_FOUR("4-2-4", 4, 2, 4);


    private final String label;
    private final int defenders;
    private final int midfielders;
    private final int forwards;

    Formation(String label, int defenders, int midfielders, int forwards) {
        this.label = label;
        this.defenders = defenders;
        this.midfielders = midfielders;
        this.forwards = forwards;
    }

    public String getLabel() {
        return label;
    }

    public int getDefenders() {
        return defenders;
    }

    public int getMidfielders() {
        return midfielders;
    }

    public int getForwards() {
        return forwards;
    }
}