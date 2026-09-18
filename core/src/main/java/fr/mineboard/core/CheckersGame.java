package fr.mineboard.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Two-player draughts on dark squares. Captures are mandatory. */
final class CheckersGame implements Game {
    private final int[] board = new int[64];
    private int selected = -1;
    private boolean jumping;

    @Override public String id() { return Games.CHECKERS; }
    @Override public int seats() { return 2; }
    @Override public String countKey() { return "tokens"; }
    @Override public String hintKey() { return "lobby.hint.checkers"; }
    @Override public String controlsKey() { return "controls.checkers"; }
    @Override public String startKey() { return "start.checkers"; }
    @Override public void clear() {
        java.util.Arrays.fill(board, 0);
        selected = -1;
        jumping = false;
    }
    @Override public void start(TableSession session, Random random) {
        clear();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 8; col++) if (((row + col) & 1) == 1) board[sq(row, col)] = 1;
        for (int row = 5; row < 8; row++) for (int col = 0; col < 8; col++) if (((row + col) & 1) == 1) board[sq(row, col)] = -1;
    }
    @Override public String apply(TableSession session, UUID player, String type, int target, long revision) {
        if (!type.equals("move")) return "invalid_action";
        String error = session.validateTurn(player, revision);
        if (!error.isEmpty()) return error;
        if (target < 0 || target > 63) return "invalid_card";
        int seat = session.seatOf(player);
        int side = seat == 0 ? 1 : -1;
        if (selected < 0 || (!jumping && owner(board[target]) == side)) {
            if (owner(board[target]) != side) return "cannot_play";
            selected = target;
            session.changed("select", seat);
            return "";
        }
        if (target == selected && !jumping) {
            selected = -1;
            session.changed("select", seat);
            return "";
        }
        List<int[]> jumps = jumpsFrom(selected, side);
        List<int[]> steps = jumps.isEmpty() && anyJump(side) ? List.of() : stepsFrom(selected, side);
        int[] chosen = null;
        for (int[] move : jumps) if (move[1] == target) chosen = move;
        if (chosen == null) for (int[] move : steps) if (move[1] == target) chosen = move;
        if (chosen == null) return "cannot_play";
        int from = chosen[0], to = chosen[1], mid = chosen[2];
        board[to] = board[from];
        board[from] = 0;
        if (mid >= 0) board[mid] = 0;
        if (side > 0 && row(to) == 7) board[to] = 2;
        if (side < 0 && row(to) == 0) board[to] = -2;
        if (mid >= 0 && !jumpsFrom(to, side).isEmpty()) {
            selected = to;
            jumping = true;
            session.changed("capture", seat);
            return "";
        }
        selected = -1;
        jumping = false;
        if (count(1) == 0 || count(-1) == 0 || !hasMove(-side)) {
            session.finish(count(side) == 0 ? 1 - seat : seat, "played", seat);
            return "";
        }
        session.passTurn();
        session.changed(mid >= 0 ? "capture" : "played", seat);
        return "";
    }
    @Override public void populate(TableSession session, ViewBuild view, UUID recipient) {
        view.counts = new int[] { count(1), count(-1) };
        view.topCard = -1;
        view.deckCount = 0;
        view.hand = List.of();
        view.playable = List.of();
        List<TableGame.Piece> pieces = new ArrayList<>();
        List<TableGame.Move> moves = new ArrayList<>();
        int seat = recipient == null ? -1 : session.seatOf(recipient);
        boolean can = session.phase() == TableGame.Phase.PLAYING && seat == session.turn();
        if (session.phase() != TableGame.Phase.LOBBY) {
            for (int row = 0; row < Layouts.CHECKER_SIZE; row++) for (int col = 0; col < Layouts.CHECKER_SIZE; col++) {
                int square = sq(row, col);
                boolean dark = ((row + col) & 1) == 1;
                double x = Layouts.CHECKER_ORIGIN + col * Layouts.CHECKER_CELL;
                double z = Layouts.CHECKER_ORIGIN + (7 - row) * Layouts.CHECKER_CELL;
                String action = can && dark ? "move" : "";
                pieces.add(new TableGame.Piece(square, dark ? 1 : 0, x, 0.152, z, 0, (float) Layouts.CHECKER_CELL, "cell", action));
                int value = board[square];
                if (value != 0) {
                    boolean lifted = selected == square;
                    pieces.add(new TableGame.Piece(square, value, x, lifted ? 0.195 : 0.168, z, 0,
                        lifted ? Layouts.CHECKER_TOKEN * 1.12f : Layouts.CHECKER_TOKEN, "token", can ? "move" : ""));
                }
                if (can && dark) moves.add(new TableGame.Move("move", square));
            }
        }
        view.pieces = pieces;
        view.moves = moves;
        view.buttons = session.phase() == TableGame.Phase.FINISHED ? List.of("rematch") : List.of();
    }
    private boolean hasMove(int side) {
        for (int i = 0; i < 64; i++) if (owner(board[i]) == side
            && (!jumpsFrom(i, side).isEmpty() || !stepsFrom(i, side).isEmpty())) return true;
        return false;
    }
    private boolean anyJump(int side) {
        for (int i = 0; i < 64; i++) if (owner(board[i]) == side && !jumpsFrom(i, side).isEmpty()) return true;
        return false;
    }
    private List<int[]> stepsFrom(int from, int side) {
        List<int[]> moves = new ArrayList<>();
        int value = board[from];
        if (owner(value) != side) return moves;
        for (int[] dir : dirs(value)) {
            int to = sq(row(from) + dir[0], col(from) + dir[1]);
            if (to >= 0 && board[to] == 0) moves.add(new int[] { from, to, -1 });
        }
        return moves;
    }
    private List<int[]> jumpsFrom(int from, int side) {
        List<int[]> moves = new ArrayList<>();
        int value = board[from];
        if (owner(value) != side) return moves;
        for (int[] dir : dirs(value)) {
            int mid = sq(row(from) + dir[0], col(from) + dir[1]);
            int to = sq(row(from) + dir[0] * 2, col(from) + dir[1] * 2);
            if (mid >= 0 && to >= 0 && owner(board[mid]) == -side && board[to] == 0) moves.add(new int[] { from, to, mid });
        }
        return moves;
    }
    private int[][] dirs(int value) {
        if (value == 2 || value == -2) return new int[][] { {1, -1}, {1, 1}, {-1, -1}, {-1, 1} };
        return value > 0 ? new int[][] { {1, -1}, {1, 1} } : new int[][] { {-1, -1}, {-1, 1} };
    }
    private int count(int side) {
        int total = 0;
        for (int value : board) if (owner(value) == side) total++;
        return total;
    }
    private static int owner(int value) { return Integer.compare(value, 0); }
    private static int row(int square) { return square / 8; }
    private static int col(int square) { return square % 8; }
    private static int sq(int row, int col) {
        return row < 0 || row > 7 || col < 0 || col > 7 ? -1 : row * 8 + col;
    }
}
