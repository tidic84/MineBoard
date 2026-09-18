package fr.mineboard.core;

import java.util.*;

/** Server-owned rules. No Minecraft, rendering, or loader dependencies. */
public final class TableGame {
    public enum Phase { LOBBY, PLAYING, FINISHED }
    public record Seat(UUID id, String name, boolean ready, int count) {}
    /** Only the recipient's hand is exposed; never send a TableGame to a client. */
    public record View(long revision, Phase phase, List<Seat> seats, int yourSeat,
                       int turn, int topCard, int deckCount, List<Integer> hand,
                       int winner, int lastActor, String event) {}

    private final UUID[] players = new UUID[2];
    private final String[] names = {"", ""};
    private final boolean[] ready = new boolean[2];
    private final List<List<Card>> hands = List.of(new ArrayList<>(), new ArrayList<>());
    private final List<Card> draw = new ArrayList<>();
    private final List<Card> discard = new ArrayList<>();
    private final Random random;
    private Phase phase = Phase.LOBBY;
    private int turn, winner = -1, lastActor = -1;
    private long revision;
    private String event = "welcome";

    public TableGame(Random random) { this.random = Objects.requireNonNull(random); }
    public int seatOf(UUID id) {
        for (int i = 0; i < 2; i++) if (id.equals(players[i])) return i;
        return -1;
    }
    public String join(UUID id, String name) {
        if (seatOf(id) >= 0) return "already_joined";
        if (phase != Phase.LOBBY) return "already_started";
        for (int i = 0; i < 2; i++) if (players[i] == null) {
            players[i] = id;
            names[i] = name;
            changed("joined", i);
            return "";
        }
        return "full";
    }
    public String ready(UUID id) {
        int seat = seatOf(id);
        if (seat < 0) return "not_seated";
        if (phase != Phase.LOBBY) return "already_started";
        ready[seat] = !ready[seat];
        changed("ready", seat);
        return "";
    }
    public String start(UUID id) {
        if (seatOf(id) != host()) return "host_only";
        if (phase != Phase.LOBBY) return "already_started";
        if (players[0] == null || players[1] == null || !ready[0] || !ready[1]) return "not_ready";
        draw.clear(); discard.clear(); hands.forEach(List::clear);
        for (int copy = 0; copy < 2; copy++) for (int n = 0; n < 40; n++) draw.add(Card.fromId(n));
        Collections.shuffle(draw, random);
        for (int i = 0; i < 7; i++) for (List<Card> hand : hands) hand.add(take());
        discard.add(take());
        turn = 0; winner = -1; phase = Phase.PLAYING;
        changed("started", -1);
        return "";
    }
    public String play(UUID id, int index, long expectedRevision) {
        String error = validateTurn(id, expectedRevision);
        if (!error.isEmpty()) return error;
        List<Card> hand = hands.get(turn);
        if (index < 0 || index >= hand.size()) return "invalid_card";
        Card card = hand.get(index);
        if (!card.matches(discard.getLast())) return "cannot_play";
        discard.add(hand.remove(index));
        int actor = turn;
        if (hand.isEmpty()) { phase = Phase.FINISHED; winner = actor; }
        else turn = 1 - turn;
        changed("played", actor);
        return "";
    }
    /** Simplified prototype rule: drawing one card always ends the turn. */
    public String draw(UUID id, long expectedRevision) {
        String error = validateTurn(id, expectedRevision);
        if (!error.isEmpty()) return error;
        if (draw.isEmpty() && discard.size() > 1) {
            Card top = discard.removeLast();
            draw.addAll(discard); discard.clear(); discard.add(top);
            Collections.shuffle(draw, random);
        }
        int actor = turn;
        // With all cards in players' hands, passing keeps the round playable.
        // The finite 80-card deck inherently bounds every private hand to 79 cards.
        if (draw.isEmpty()) { turn = 1 - turn; changed("passed", actor); return ""; }
        hands.get(turn).add(take()); turn = 1 - turn;
        changed("drew", actor);
        return "";
    }
    public String rematch(UUID id) {
        if (seatOf(id) != host()) return "host_only";
        if (phase != Phase.FINISHED) return "not_finished";
        reset(); changed("rematch", -1); return "";
    }
    public void leave(UUID id) {
        int seat = seatOf(id);
        if (seat < 0) return;
        players[seat] = null; names[seat] = "";
        reset(); changed("left", seat);
    }
    public View view(UUID recipient) {
        List<Seat> seats = new ArrayList<>();
        for (int i = 0; i < 2; i++) seats.add(new Seat(players[i], names[i], ready[i], hands.get(i).size()));
        int seat = recipient == null ? -1 : seatOf(recipient);
        return new View(revision, phase, List.copyOf(seats), seat, turn,
            discard.isEmpty() ? -1 : discard.getLast().id(), draw.size(),
            seat < 0 ? List.of() : hands.get(seat).stream().map(Card::id).toList(), winner, lastActor, event);
    }
    private int host() { return players[0] != null ? 0 : players[1] != null ? 1 : -2; }
    private Card take() { return draw.removeLast(); }
    private String validateTurn(UUID id, long expectedRevision) {
        if (phase != Phase.PLAYING) return "not_playing";
        if (seatOf(id) != turn) return "not_your_turn";
        if (revision != expectedRevision) return "stale";
        return "";
    }
    private void reset() {
        phase = Phase.LOBBY; turn = 0; winner = -1;
        Arrays.fill(ready, false); hands.forEach(List::clear); draw.clear(); discard.clear();
    }
    private void changed(String event, int actor) { this.event = event; lastActor = actor; revision++; }
}
