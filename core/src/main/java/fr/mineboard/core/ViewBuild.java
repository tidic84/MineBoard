package fr.mineboard.core;

import java.util.List;

/** Mutable view parts filled by a game when composing a recipient-specific snapshot. */
public final class ViewBuild {
    int topCard = -1;
    int deckCount;
    int[] counts = new int[0];
    List<Integer> hand = List.of();
    List<Integer> playable = List.of();
    List<String> buttons = List.of();
    List<TableGame.Piece> pieces = List.of();
    List<TableGame.Move> moves = List.of();
}
