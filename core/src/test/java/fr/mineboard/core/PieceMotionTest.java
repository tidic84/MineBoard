package fr.mineboard.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PieceMotionTest {
    @Test void capturedPieceDoesNotBecomeTheMovingPieceAnimationOrigin() {
        var from = new TableGame.Piece(8, 4, .2, .141, .8, 0, .112f, "chess", "move");
        var captured = new TableGame.Piece(24, -1, .2, .141, .4, 0, .112f, "chess", "move");
        var to = new TableGame.Piece(24, 4, .2, .141, .4, 0, .112f, "chess", "move");
        var start = PieceMotion.pose(List.of(from, captured), List.of(to), to, 0);
        assertEquals(from.z(), start.z());
    }

    @Test void stationaryCardsDoNotJumpWhenAnotherPlayerActs() {
        var card = new TableGame.Piece(3000, 4, .2, .16, .8, 0, .3f, "discard", "");
        assertEquals(card.y(), PieceMotion.pose(List.of(card), card, .5).y());
    }

    @Test void playedCardTravelsFromItsHandAcrossKinds() {
        var hand = new TableGame.Piece(3000, -1, .2, .16, .8, 0, .3f, "seat", "");
        var discard = new TableGame.Piece(3000, 4, .6, .16, .5, 0, .3f, "discard", "");
        assertEquals(hand.x(), PieceMotion.pose(List.of(hand), discard, 0).x());
    }
    @Test void newPiecesArcFromTheTableCenter() {
        var next = new TableGame.Piece(3, 4, 0.8, 0.18, 0.7, 20, 0.4f, "memory", "flip");
        var mid = PieceMotion.pose(List.of(), next, 0.5);
        assertTrue(mid.x() > 0.5 && mid.x() < 0.8);
        assertTrue(mid.z() > 0.5 && mid.z() < 0.7);
        assertTrue(mid.y() > 0.16);
        var end = PieceMotion.pose(List.of(), next, 1);
        assertEquals(0.8, end.x(), 1e-6);
        assertEquals(0.18, end.y(), 1e-6);
    }

    @Test void existingPiecesSlideTowardTheirNewPose() {
        var from = new TableGame.Piece(1, 1, 0.2, 0.16, 0.2, 0, 0.3f, "chess", "move");
        var to = new TableGame.Piece(1, 1, 0.8, 0.16, 0.8, 0, 0.3f, "chess", "move");
        var mid = PieceMotion.pose(List.of(from), to, 0.5);
        assertTrue(mid.x() > 0.2 && mid.x() < 0.8);
        assertEquals(0.16, mid.y(), 0.001);
    }

    @Test void slidingCardsKeepALittleHop() {
        var from = new TableGame.Piece(2, 3, 0.2, 0.16, 0.2, 0, 0.3f, "tableau", "");
        var to = new TableGame.Piece(2, 3, 0.8, 0.16, 0.8, 0, 0.3f, "tableau", "");
        var mid = PieceMotion.pose(List.of(from), to, 0.5);
        assertTrue(mid.y() > 0.16);
        assertTrue(mid.x() > 0.2 && mid.x() < 0.8);
    }
}
