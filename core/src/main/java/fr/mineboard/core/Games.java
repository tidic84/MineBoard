package fr.mineboard.core;

import java.util.List;

public final class Games {
    public static final String DISCARD = "discard";
    public static final String MEMORY = "memory";
    public static final String EFFECTS = "effects";
    public static final String CHECKERS = "checkers";
    public static final String DRAFT = "draft";
    private static final List<String> IDS = List.of(DISCARD, MEMORY, EFFECTS, CHECKERS, DRAFT);

    private Games() {}
    public static List<String> ids() { return IDS; }
    public static Game create(String id) {
        return switch (id) {
            case MEMORY -> new MemoryGame();
            case EFFECTS -> new DiscardGame(true);
            case CHECKERS -> new CheckersGame();
            case DRAFT -> new DraftGame();
            default -> new DiscardGame();
        };
    }
    public static String next(String id) {
        int index = IDS.indexOf(id);
        return IDS.get((index + 1) % IDS.size());
    }
}
