package fr.mineboard.core;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/** Server-owned table. Rules live in Game implementations; never send a TableGame to a client. */
public final class TableGame {
    public enum Phase { LOBBY, PLAYING, FINISHED }
    public record Seat(UUID id, String name, boolean ready, int count) {}
    public record Piece(int id, int card, double x, double y, double z, float angle, float scale, String kind, String action) {}
    public record Move(String type, int target) {}
    /** Only the recipient's hand is exposed. */
    public record View(long revision, Phase phase, List<Seat> seats, int yourSeat,
                       int turn, int topCard, int deckCount, List<Integer> hand,
                       int winner, int lastActor, String event, String gameId, String countKey,
                       String hintKey, String controlsKey, String startKey, int host, double boardSpan,
                       List<Integer> playable, List<String> buttons, List<Piece> pieces, List<Move> moves) {
        public String gameId() { return gameId == null || gameId.isEmpty() ? Games.DISCARD : gameId; }
        public String countKey() { return countKey == null || countKey.isEmpty() ? "hand" : countKey; }
        public String hintKey() { return hintKey == null || hintKey.isEmpty() ? "lobby.hint" : hintKey; }
        public String controlsKey() { return controlsKey == null || controlsKey.isEmpty() ? "controls" : controlsKey; }
        public String startKey() { return startKey == null || startKey.isEmpty() ? "start" : startKey; }
        public double boardSpan() { return boardSpan <= 0 ? 1 : boardSpan; }
        public List<Integer> playable() { return playable == null ? List.of() : playable; }
        public List<String> buttons() { return buttons == null ? List.of() : buttons; }
        public List<Piece> pieces() { return pieces == null ? List.of() : pieces; }
        public List<Move> moves() { return moves == null ? List.of() : moves; }
        public boolean allows(String type, int target) {
            for (Move move : moves()) if (move.type().equals(type) && move.target() == target) return true;
            return type.equals("draw") && buttons().contains("draw") && target < 0;
        }
        public boolean allows(String type) {
            if (buttons().contains(type)) return true;
            for (Move move : moves()) if (move.type().equals(type)) return true;
            return false;
        }
    }

    private final Random random;
    private final TableSession session;
    private Game game;

    public TableGame(Random random) {
        this.random = Objects.requireNonNull(random);
        this.game = Games.create(Games.DISCARD);
        this.session = new TableSession(game.seats());
    }
    public String id() { return game.id(); }
    public int seatOf(UUID id) { return session.seatOf(id); }
    public String join(UUID id, String name) { return session.join(id, name); }
    public String ready(UUID id) { return session.ready(id); }
    public String start(UUID id) {
        if (session.seatOf(id) != session.host()) return "host_only";
        if (session.phase() != Phase.LOBBY) return "already_started";
        if (!session.allReady()) return "not_ready";
        game.start(session, random);
        String error = session.beginPlaying(id);
        if (error.isEmpty()) session.setTurn(game.startingTurn());
        return error;
    }
    public String play(UUID id, int index, long expectedRevision) {
        return apply(id, "", "play", index, expectedRevision);
    }
    public String draw(UUID id, long expectedRevision) {
        return apply(id, "", "draw", -1, expectedRevision);
    }
    public String rematch(UUID id) {
        String error = session.rematch(id);
        if (error.isEmpty()) game.clear();
        return error;
    }
    public void leave(UUID id) {
        if (session.leave(id)) game.clear();
    }
    public String apply(UUID id, String name, String type, int target, long revision) {
        return switch (type) {
            case "join" -> session.join(id, name);
            case "ready" -> session.ready(id);
            case "start" -> start(id);
            case "rematch" -> rematch(id);
            case "game" -> cycleGame(id);
            default -> game.apply(session, id, type, target, revision);
        };
    }
    public View view(UUID recipient) {
        ViewBuild build = new ViewBuild();
        game.populate(session, build, recipient);
        int seat = recipient == null ? -1 : session.seatOf(recipient);
        return new View(session.revision(), session.phase(), List.copyOf(session.seats(build.counts)), seat,
            session.turn(), build.topCard, build.deckCount, List.copyOf(build.hand),
            session.winner(), session.lastActor(), session.event(), game.id(), game.countKey(),
            game.hintKey(), game.controlsKey(), game.startKey(), session.host(), game.boardSpan(),
            List.copyOf(build.playable), List.copyOf(build.buttons), List.copyOf(build.pieces), List.copyOf(build.moves));
    }
    Game rules() { return game; }
    private String cycleGame(UUID id) {
        if (session.phase() != Phase.LOBBY) return "already_started";
        if (session.seatOf(id) != session.host()) return "host_only";
        game.clear();
        game = Games.create(Games.next(game.id()));
        session.resize(game.seats());
        session.unreadyAll();
        session.changed("game", session.host());
        return "";
    }
}
