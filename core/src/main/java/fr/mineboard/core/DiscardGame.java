package fr.mineboard.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Color-and-number discard. Optional effects: 0 skip, 1 reverse, 2 plus-two. */
final class DiscardGame implements Game {
    private final boolean effects;
    private final List<List<Card>> hands = new ArrayList<>();
    private final List<Card> draw = new ArrayList<>();
    private final List<Card> discard = new ArrayList<>();
    private Random random;

    DiscardGame() { this(false); }
    DiscardGame(boolean effects) { this.effects = effects; }
    @Override public String id() { return effects ? Games.EFFECTS : Games.DISCARD; }
    @Override public int seats() { return 2; }
    @Override public String countKey() { return "hand"; }
    @Override public String hintKey() { return effects ? "lobby.hint.effects" : "lobby.hint"; }
    @Override public String controlsKey() { return "controls"; }
    @Override public String startKey() { return "start"; }
    @Override public void clear() {
        hands.clear();
        draw.clear();
        discard.clear();
    }
    @Override public void start(TableSession session, Random random) {
        this.random = random;
        clear();
        for (int i = 0; i < session.seatCount(); i++) hands.add(new ArrayList<>());
        for (int copy = 0; copy < 2; copy++) for (int n = 0; n < 40; n++) draw.add(Card.fromId(n));
        Collections.shuffle(draw, random);
        for (int i = 0; i < 7; i++) for (List<Card> hand : hands) hand.add(take());
        discard.add(take());
    }
    @Override public String apply(TableSession session, UUID player, String type, int target, long revision) {
        return switch (type) {
            case "play" -> play(session, player, target, revision);
            case "draw" -> draw(session, player, revision);
            default -> "invalid_action";
        };
    }
    @Override public void populate(TableSession session, ViewBuild view, UUID recipient) {
        int[] counts = new int[session.seatCount()];
        for (int i = 0; i < counts.length; i++) counts[i] = i < hands.size() ? hands.get(i).size() : 0;
        view.counts = counts;
        view.topCard = discard.isEmpty() ? -1 : discard.getLast().id();
        view.deckCount = draw.size();
        int seat = recipient == null ? -1 : session.seatOf(recipient);
        view.hand = seat < 0 || seat >= hands.size() ? List.of() : hands.get(seat).stream().map(Card::id).toList();
        List<TableGame.Piece> pieces = new ArrayList<>();
        List<TableGame.Move> moves = new ArrayList<>();
        List<Integer> playable = new ArrayList<>();
        List<String> buttons = new ArrayList<>();
        if (session.phase() != TableGame.Phase.LOBBY) {
            for (int i = 0; i < 5; i++) {
                String action = i == 4 && session.phase() == TableGame.Phase.PLAYING ? "draw" : "";
                pieces.add(new TableGame.Piece(-1, -1, .32, .16 + i * .008, .5, 0, .46f, "draw", action));
            }
            if (view.topCard >= 0) {
                pieces.add(new TableGame.Piece(-1, view.topCard, .67, .18, .5, 8, .46f, "discard", ""));
            }
            for (int s = 0; s < session.seatCount(); s++) {
                int count = Math.min(7, counts[s]);
                for (int i = 0; i < count; i++) {
                    pieces.add(new TableGame.Piece(-1, -1, .5 + (i - (count - 1) / 2.0) * .06,
                        .16 + i * .002, s == 0 ? .87 : .13, (i - (count - 1) / 2f) * 5 + s * 180, .27f, "seat", ""));
                }
            }
        }
        if (session.phase() == TableGame.Phase.PLAYING && seat == session.turn()) {
            Card top = discard.isEmpty() ? null : discard.getLast();
            if (top != null) {
                List<Card> hand = hands.get(seat);
                for (int i = 0; i < hand.size(); i++) if (hand.get(i).matches(top)) {
                    playable.add(i);
                    moves.add(new TableGame.Move("play", i));
                }
            }
            if (!playable.isEmpty()) buttons.add("play");
            moves.add(new TableGame.Move("draw", -1));
            buttons.add("draw");
        }
        if (session.phase() == TableGame.Phase.FINISHED) buttons.add("rematch");
        view.pieces = pieces;
        view.moves = moves;
        view.playable = playable;
        view.buttons = buttons;
    }
    private String play(TableSession session, UUID id, int index, long expectedRevision) {
        String error = session.validateTurn(id, expectedRevision);
        if (!error.isEmpty()) return error;
        List<Card> hand = hands.get(session.turn());
        if (index < 0 || index >= hand.size()) return "invalid_card";
        Card card = hand.get(index);
        if (!card.matches(discard.getLast())) return "cannot_play";
        discard.add(hand.remove(index));
        int actor = session.turn();
        if (hand.isEmpty()) session.finish(actor, "played", actor);
        else {
            String event = "played";
            int passes = 1;
            if (effects && card.number() <= 2) {
                int next = (actor + 1) % session.seatCount();
                if (card.number() == 2) give(next, 2);
                passes = 2;
                event = card.number() == 0 ? "skipped" : card.number() == 1 ? "reversed" : "plus2";
            }
            for (int i = 0; i < passes; i++) session.passTurn();
            session.changed(event, actor);
        }
        return "";
    }
    private String draw(TableSession session, UUID id, long expectedRevision) {
        String error = session.validateTurn(id, expectedRevision);
        if (!error.isEmpty()) return error;
        refill();
        int actor = session.turn();
        if (draw.isEmpty()) {
            session.passTurn();
            session.changed("passed", actor);
            return "";
        }
        hands.get(session.turn()).add(take());
        session.passTurn();
        session.changed("drew", actor);
        return "";
    }
    private void give(int seat, int amount) {
        for (int i = 0; i < amount; i++) {
            refill();
            if (draw.isEmpty()) return;
            hands.get(seat).add(take());
        }
    }
    private void refill() {
        if (!draw.isEmpty() || discard.size() <= 1) return;
        Card top = discard.removeLast();
        draw.addAll(discard);
        discard.clear();
        discard.add(top);
        Collections.shuffle(draw, random);
    }
    private Card take() { return draw.removeLast(); }
}
