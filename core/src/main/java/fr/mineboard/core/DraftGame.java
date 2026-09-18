package fr.mineboard.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Two-player simultaneous draft. Highest number-sum wins. */
final class DraftGame implements Game {
    private final List<List<Card>> hands = new ArrayList<>();
    private final List<List<Card>> tableaus = new ArrayList<>();
    private final int[] pick = { -1, -1 };

    @Override public String id() { return Games.DRAFT; }
    @Override public int seats() { return 2; }
    @Override public String countKey() { return "tableau"; }
    @Override public String hintKey() { return "lobby.hint.draft"; }
    @Override public String controlsKey() { return "controls.draft"; }
    @Override public String startKey() { return "start.draft"; }
    @Override public int startingTurn() { return -1; }
    @Override public void clear() {
        hands.clear();
        tableaus.clear();
        pick[0] = pick[1] = -1;
    }
    @Override public void start(TableSession session, Random random) {
        clear();
        List<Card> deck = new ArrayList<>();
        for (int n = 0; n < 40; n++) deck.add(Card.fromId(n));
        Collections.shuffle(deck, random);
        for (int i = 0; i < 2; i++) {
            hands.add(new ArrayList<>(deck.subList(i * 7, i * 7 + 7)));
            tableaus.add(new ArrayList<>());
        }
    }
    @Override public String apply(TableSession session, UUID player, String type, int target, long revision) {
        if (!type.equals("play")) return "invalid_action";
        String error = session.validateTurn(player, -1);
        if (!error.isEmpty()) return error;
        int seat = session.seatOf(player);
        if (pick[seat] >= 0) return "already_started";
        if (target < 0 || target >= hands.get(seat).size()) return "invalid_card";
        pick[seat] = target;
        if (pick[0] < 0 || pick[1] < 0) {
            session.changed("picked", seat);
            return "";
        }
        for (int i = 0; i < 2; i++) tableaus.get(i).add(hands.get(i).remove(pick[i]));
        pick[0] = pick[1] = -1;
        if (hands.get(0).isEmpty()) {
            int score0 = score(0), score1 = score(1);
            int winner = score0 == score1 ? -1 : (score0 > score1 ? 0 : 1);
            session.finish(winner, winner < 0 ? "tied" : "played", seat);
            return "";
        }
        List<Card> pass = hands.get(0);
        hands.set(0, hands.get(1));
        hands.set(1, pass);
        session.changed("passed", seat);
        return "";
    }
    @Override public void populate(TableSession session, ViewBuild view, UUID recipient) {
        view.counts = new int[] { tableaus.isEmpty() ? 0 : tableaus.get(0).size(), tableaus.size() < 2 ? 0 : tableaus.get(1).size() };
        view.topCard = -1;
        view.deckCount = 0;
        int seat = recipient == null ? -1 : session.seatOf(recipient);
        view.hand = seat < 0 ? List.of() : hands.get(seat).stream().map(Card::id).toList();
        List<Integer> playable = new ArrayList<>();
        List<TableGame.Move> moves = new ArrayList<>();
        List<String> buttons = new ArrayList<>();
        if (session.phase() == TableGame.Phase.PLAYING && seat >= 0 && pick[seat] < 0) {
            for (int i = 0; i < view.hand.size(); i++) {
                playable.add(i);
                moves.add(new TableGame.Move("play", i));
            }
            buttons.add("play");
        }
        List<TableGame.Piece> pieces = new ArrayList<>();
        if (session.phase() != TableGame.Phase.LOBBY) {
            for (int s = 0; s < tableaus.size(); s++) {
                List<Card> row = tableaus.get(s);
                for (int i = 0; i < row.size(); i++) {
                    pieces.add(new TableGame.Piece(-1, row.get(i).id(),
                        0.22 + i * 0.09, 0.165, s == 0 ? 0.78 : 0.22, s * 180, 0.24f, "tableau", ""));
                }
            }
        }
        view.playable = playable;
        view.moves = moves;
        view.buttons = session.phase() == TableGame.Phase.FINISHED ? List.of("rematch") : buttons;
        view.pieces = pieces;
    }
    private int score(int seat) {
        int total = 0;
        for (Card card : tableaus.get(seat)) total += card.number();
        return total;
    }
}
