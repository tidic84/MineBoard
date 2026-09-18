package fr.mineboard.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Pair-matching grid. Face-up mismatches stay until the next flip. */
final class MemoryGame implements Game {
    static final int SIZE = 4;
    static final int TOTAL = SIZE * SIZE;
    private int[] cards = new int[0];
    private boolean[] matched = new boolean[0];
    private boolean[] faceUp = new boolean[0];
    private int[] scores = new int[0];
    private int first = -1;
    private int mismatchA = -1, mismatchB = -1;

    @Override public String id() { return Games.MEMORY; }
    @Override public int seats() { return 2; }
    @Override public String countKey() { return "pairs"; }
    @Override public String hintKey() { return "lobby.hint.memory"; }
    @Override public String controlsKey() { return "controls.memory"; }
    @Override public String startKey() { return "start.memory"; }
    @Override public void clear() {
        cards = new int[0];
        matched = new boolean[0];
        faceUp = new boolean[0];
        scores = new int[0];
        first = -1;
        mismatchA = mismatchB = -1;
    }
    @Override public void start(TableSession session, Random random) {
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < 40; i++) pool.add(i);
        Collections.shuffle(pool, random);
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < TOTAL / 2; i++) {
            ids.add(pool.get(i));
            ids.add(pool.get(i));
        }
        Collections.shuffle(ids, random);
        cards = ids.stream().mapToInt(Integer::intValue).toArray();
        matched = new boolean[TOTAL];
        faceUp = new boolean[TOTAL];
        scores = new int[session.seatCount()];
        first = -1;
        mismatchA = mismatchB = -1;
    }
    int cardAt(int index) { return cards[index]; }
    @Override public String apply(TableSession session, UUID player, String type, int target, long revision) {
        if (!type.equals("flip")) return "invalid_action";
        String error = session.validateTurn(player, revision);
        if (!error.isEmpty()) return error;
        if (target < 0 || target >= cards.length || matched[target] || faceUp[target]) return "cannot_flip";
        hideMismatch();
        faceUp[target] = true;
        int actor = session.turn();
        if (first < 0) {
            first = target;
            session.changed("flipped", actor);
            return "";
        }
        int other = first;
        first = -1;
        if (cards[other] == cards[target]) {
            matched[other] = matched[target] = true;
            faceUp[other] = faceUp[target] = false;
            scores[actor]++;
            if (complete()) {
                int winner = scores[0] == scores[1] ? -1 : (scores[0] > scores[1] ? 0 : 1);
                session.finish(winner, winner < 0 ? "tied" : "matched", actor);
            } else session.changed("matched", actor);
            return "";
        }
        mismatchA = other;
        mismatchB = target;
        session.passTurn();
        session.changed("missed", actor);
        return "";
    }
    @Override public void populate(TableSession session, ViewBuild view, UUID recipient) {
        view.counts = scores.length == 0 ? new int[session.seatCount()] : scores.clone();
        view.topCard = -1;
        int remaining = 0;
        List<TableGame.Piece> pieces = new ArrayList<>();
        List<TableGame.Move> moves = new ArrayList<>();
        int seat = recipient == null ? -1 : session.seatOf(recipient);
        boolean canFlip = session.phase() == TableGame.Phase.PLAYING && seat == session.turn();
        float scale = Layouts.MEMORY_SCALE;
        double[] grid = Layouts.grid(SIZE, SIZE, Layouts.CARD_X * scale, Layouts.CARD_Z * scale, Layouts.MEMORY_GAP);
        for (int i = 0; i < cards.length; i++) {
            if (matched[i]) continue;
            remaining++;
            boolean open = faceUp[i];
            double col = i % SIZE, row = i / SIZE;
            double x = grid[0] + col * grid[2];
            double z = grid[1] + row * grid[3];
            String action = canFlip && !open ? "flip" : "";
            pieces.add(new TableGame.Piece(i, open ? cards[i] : -1, x, 0.165, z, 0, scale, "memory", action));
            if (!action.isEmpty()) moves.add(new TableGame.Move("flip", i));
        }
        view.deckCount = remaining;
        view.pieces = pieces;
        view.moves = moves;
        view.playable = List.of();
        view.hand = List.of();
        view.buttons = session.phase() == TableGame.Phase.FINISHED ? List.of("rematch") : List.of();
    }
    private void hideMismatch() {
        if (mismatchA < 0) return;
        if (!matched[mismatchA]) faceUp[mismatchA] = false;
        if (!matched[mismatchB]) faceUp[mismatchB] = false;
        mismatchA = mismatchB = -1;
    }
    private boolean complete() {
        for (boolean done : matched) if (!done) return false;
        return true;
    }
}
