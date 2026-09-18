package fr.mineboard.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Shared lobby, seats, turns, and revision. Games never own player identity. */
public final class TableSession {
    private UUID[] players;
    private String[] names;
    private boolean[] ready;
    private TableGame.Phase phase = TableGame.Phase.LOBBY;
    private int turn, winner = -1, lastActor = -1;
    private long revision;
    private String event = "welcome";

    public TableSession(int seats) { resize(seats); }
    public TableGame.Phase phase() { return phase; }
    public int turn() { return turn; }
    public int winner() { return winner; }
    public int lastActor() { return lastActor; }
    public long revision() { return revision; }
    public String event() { return event; }
    public int seatCount() { return players.length; }
    public int seatOf(UUID id) {
        if (id == null) return -1;
        for (int i = 0; i < players.length; i++) if (id.equals(players[i])) return i;
        return -1;
    }
    public int host() {
        for (int i = 0; i < players.length; i++) if (players[i] != null) return i;
        return -2;
    }
    public UUID player(int seat) { return players[seat]; }
    public void resize(int seats) {
        UUID[] nextPlayers = new UUID[seats];
        String[] nextNames = new String[seats];
        boolean[] nextReady = new boolean[seats];
        Arrays.fill(nextNames, "");
        if (players != null) {
            int copy = Math.min(seats, players.length);
            System.arraycopy(players, 0, nextPlayers, 0, copy);
            System.arraycopy(names, 0, nextNames, 0, copy);
            System.arraycopy(ready, 0, nextReady, 0, copy);
        }
        players = nextPlayers;
        names = nextNames;
        ready = nextReady;
    }
    public String join(UUID id, String name) {
        if (seatOf(id) >= 0) return "already_joined";
        if (phase != TableGame.Phase.LOBBY) return "already_started";
        for (int i = 0; i < players.length; i++) if (players[i] == null) {
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
        if (phase != TableGame.Phase.LOBBY) return "already_started";
        ready[seat] = !ready[seat];
        changed("ready", seat);
        return "";
    }
    public boolean allReady() {
        for (int i = 0; i < players.length; i++) if (players[i] == null || !ready[i]) return false;
        return players.length > 0;
    }
    public String beginPlaying(UUID id) {
        if (seatOf(id) != host()) return "host_only";
        if (phase != TableGame.Phase.LOBBY) return "already_started";
        if (!allReady()) return "not_ready";
        turn = 0;
        winner = -1;
        phase = TableGame.Phase.PLAYING;
        changed("started", -1);
        return "";
    }
    public String rematch(UUID id) {
        if (seatOf(id) != host()) return "host_only";
        if (phase != TableGame.Phase.FINISHED) return "not_finished";
        resetLobby();
        changed("rematch", -1);
        return "";
    }
    public boolean leave(UUID id) {
        int seat = seatOf(id);
        if (seat < 0) return false;
        players[seat] = null;
        names[seat] = "";
        resetLobby();
        changed("left", seat);
        return true;
    }
    public String validateTurn(UUID id, long expectedRevision) {
        if (phase != TableGame.Phase.PLAYING) return "not_playing";
        if (turn >= 0 && seatOf(id) != turn) return "not_your_turn";
        if (turn < 0 && seatOf(id) < 0) return "not_your_turn";
        if (expectedRevision >= 0 && revision != expectedRevision) return "stale";
        return "";
    }
    public void passTurn() { turn = (turn + 1) % players.length; }
    public void setTurn(int seat) { turn = seat; }
    public void finish(int winnerSeat, String finishEvent, int actor) {
        phase = TableGame.Phase.FINISHED;
        winner = winnerSeat;
        changed(finishEvent, actor);
    }
    public void changed(String nextEvent, int actor) {
        event = nextEvent;
        lastActor = actor;
        revision++;
    }
    public List<TableGame.Seat> seats(int[] counts) {
        List<TableGame.Seat> list = new ArrayList<>();
        for (int i = 0; i < players.length; i++) {
            int count = counts != null && i < counts.length ? counts[i] : 0;
            list.add(new TableGame.Seat(players[i], names[i], ready[i], count));
        }
        return list;
    }
    private void resetLobby() {
        phase = TableGame.Phase.LOBBY;
        turn = 0;
        winner = -1;
        Arrays.fill(ready, false);
    }
    public void unreadyAll() {
        Arrays.fill(ready, false);
    }
}
