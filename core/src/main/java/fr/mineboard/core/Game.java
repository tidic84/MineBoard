package fr.mineboard.core;

import java.util.Random;
import java.util.UUID;

/** Rules for one game. No Minecraft types. The session owns lobby, seats, and revision. */
public interface Game {
    String id();
    int seats();
    default int minSeats() { return seats(); }
    default int maxSeats() { return seats(); }
    String countKey();
    String hintKey();
    String controlsKey();
    String startKey();
    default int startingTurn() { return 0; }
    /** Playable area in blocks around the table origin. Camera frames at least this span. */
    default double boardSpan() { return 1; }
    void clear();
    void start(TableSession session, Random random);
    String apply(TableSession session, UUID player, String type, int target, long revision);
    void populate(TableSession session, ViewBuild view, UUID recipient);
}
