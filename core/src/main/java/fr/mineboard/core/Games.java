package fr.mineboard.core;

import java.util.List;

public final class Games {
    public static final String DISCARD = "discard";
    public static final String MEMORY = "memory";
    public static final String EFFECTS = "effects";
    public static final String CHECKERS = "checkers";
    public static final String DRAFT = "draft";
    public static final String WONDERS = "wonders";
    public static final String CHESS = "chess";
    private static final List<String> IDS = List.of(DISCARD, MEMORY, EFFECTS, CHECKERS, DRAFT, WONDERS, CHESS);

    private Games() {}
    public static List<String> ids() { return IDS; }
    public static Game create(String id) {
        return switch (id) {
            case MEMORY -> new MemoryGame();
            case EFFECTS -> new DiscardGame(true);
            case CHECKERS -> new CheckersGame();
            case DRAFT -> new DraftGame();
            case WONDERS -> new AgesGame();
            case CHESS -> new ChessGame();
            default -> new DiscardGame();
        };
    }
    public static String next(String id) {
        int index = IDS.indexOf(id);
        return IDS.get((index + 1) % IDS.size());
    }
}
