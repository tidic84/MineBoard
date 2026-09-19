package fr.mineboard.core;

import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ChessGameTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2);

    private int[] position(TableGame game, int... squarePiecePairs) throws Exception {
        var field = ChessGame.class.getDeclaredField("board");
        field.setAccessible(true);
        int[] board = (int[]) field.get(game.rules());
        java.util.Arrays.fill(board, 0);
        for (int i = 0; i < squarePiecePairs.length; i += 2) board[squarePiecePairs[i]] = squarePiecePairs[i + 1];
        return board;
    }

    private String move(TableGame game, UUID who, int from, int to) {
        assertEquals("", game.apply(who, "", "move", from, game.view(who).revision()));
        return game.apply(who, "", "move", to, game.view(who).revision());
    }

    @Test void cannotCastleThroughAnEmptySquareAttackedByAPawn() throws Exception {
        TableGame game = playing();
        position(game, 4, 6, 7, 4, 60, -6, 14, -1);
        assertEquals("cannot_play", move(game, A, 4, 6));
    }

    @Test void cannotCastleWithoutARook() throws Exception {
        TableGame game = playing();
        position(game, 4, 6, 60, -6, 48, -1);
        assertEquals("cannot_play", move(game, A, 4, 6));
    }

    @Test void kingsTwoSquaresApartDoNotRecurseOrAllowAdjacentMove() throws Exception {
        TableGame game = playing();
        position(game, 24, 6, 26, -6, 8, 1);
        assertEquals("cannot_play", move(game, A, 24, 25));
    }

    @Test void opposingKnightsAreNotAutomaticallyADeadPosition() throws Exception {
        TableGame game = playing();
        position(game, 4, 6, 60, -6, 1, 2, 57, -2);
        assertEquals("", move(game, A, 1, 18));
        assertEquals(TableGame.Phase.PLAYING, game.view(A).phase());
    }

    @Test void mateTakesPriorityOverTheHalfmoveDraw() throws Exception {
        TableGame game = playing();
        position(game, 45, 6, 63, -6, 30, 5);
        var field = ChessGame.class.getDeclaredField("halfmove");
        field.setAccessible(true);
        field.setInt(game.rules(), 99);
        assertEquals("", move(game, A, 30, 54));
        assertEquals(0, game.view(A).winner());
    }

    @Test void repetitionIgnoresAnEnPassantSquareNobodyCanCapture() throws Exception {
        TableGame game = playing();
        position(game, 4, 6, 60, -6, 1, 2, 57, -2, 24, 1);
        var ep = ChessGame.class.getDeclaredField("ep");
        ep.setAccessible(true);
        var key = ChessGame.class.getDeclaredMethod("key", int.class);
        key.setAccessible(true);
        Object without = key.invoke(game.rules(), -1);
        ep.setInt(game.rules(), 16);
        assertEquals(without, key.invoke(game.rules(), -1));
    }

    @Test void whiteHasALightSquareOnTheRight() {
        assertEquals(0, playing().view(A).pieces().stream()
            .filter(p -> p.kind().equals("cell") && p.id() == 7).findFirst().orElseThrow().card());
    }

    private TableGame playing() {
        TableGame game = new TableGame(new Random(1));
        game.join(A, "Alice");
        for (int i = 0; i < 6; i++) game.apply(A, "", "game", -1, 0);
        assertEquals(Games.CHESS, game.id());
        game.join(B, "Bob");
        game.ready(A); game.ready(B);
        assertEquals("", game.start(A));
        return game;
    }

    @Test void knightsAndPawnsMoveAndIllegalStepsAreRejected() {
        TableGame game = playing();
        assertEquals("", game.apply(A, "", "move", 1, game.view(A).revision()));
        assertEquals("", game.apply(A, "", "move", 18, game.view(A).revision()));
        assertEquals(1, game.view(A).turn());
        assertEquals("cannot_play", game.apply(B, "", "move", 18, game.view(B).revision()));
        assertEquals("", game.apply(B, "", "move", 62, game.view(B).revision()));
        assertEquals("", game.apply(B, "", "move", 45, game.view(B).revision()));
        assertEquals(0, game.view(A).turn());
        long tokens = game.view(A).pieces().stream().filter(piece -> piece.kind().equals("chess")).count();
        assertEquals(32, tokens);
    }

    @Test void checkmateEndsTheGame() {
        TableGame game = playing();
        ChessGame rules = (ChessGame) game.rules();
        try {
            var board = ChessGame.class.getDeclaredField("board");
            board.setAccessible(true);
            int[] squares = (int[]) board.get(rules);
            java.util.Arrays.fill(squares, 0);
            squares[45] = 6;
            squares[63] = -6;
            squares[30] = 5;
            assertEquals("", game.apply(A, "", "move", 30, game.view(A).revision()));
            assertEquals("", game.apply(A, "", "move", 54, game.view(A).revision()));
            assertEquals(TableGame.Phase.FINISHED, game.view(A).phase());
            assertEquals(0, game.view(A).winner());
        } catch (ReflectiveOperationException e) {
            fail(e);
        }
    }

    @Test void stalemateIsADraw() {
        TableGame game = playing();
        ChessGame rules = (ChessGame) game.rules();
        try {
            var board = ChessGame.class.getDeclaredField("board");
            board.setAccessible(true);
            int[] squares = (int[]) board.get(rules);
            java.util.Arrays.fill(squares, 0);
            squares[42] = 6;
            squares[33] = 5;
            squares[56] = -6;
            assertEquals("", game.apply(A, "", "move", 33, game.view(A).revision()));
            assertEquals("", game.apply(A, "", "move", 41, game.view(A).revision()));
            assertEquals(TableGame.Phase.FINISHED, game.view(A).phase());
            assertEquals(-1, game.view(A).winner());
        } catch (ReflectiveOperationException e) {
            fail(e);
        }
    }

    @Test void kingsideCastleMovesTheRook() {
        TableGame game = playing();
        ChessGame rules = (ChessGame) game.rules();
        try {
            var board = ChessGame.class.getDeclaredField("board");
            board.setAccessible(true);
            int[] squares = (int[]) board.get(rules);
            java.util.Arrays.fill(squares, 0);
            squares[4] = 6;
            squares[7] = 4;
            squares[60] = -6;
            assertEquals("", game.apply(A, "", "move", 4, game.view(A).revision()));
            assertEquals("", game.apply(A, "", "move", 6, game.view(A).revision()));
            assertEquals(6, squares[6]);
            assertEquals(4, squares[5]);
            assertEquals(0, squares[7]);
        } catch (ReflectiveOperationException e) {
            fail(e);
        }
    }

    @Test void enPassantCapturesThePassedPawn() {
        TableGame game = playing();
        ChessGame rules = (ChessGame) game.rules();
        try {
            var board = ChessGame.class.getDeclaredField("board");
            board.setAccessible(true);
            int[] squares = (int[]) board.get(rules);
            java.util.Arrays.fill(squares, 0);
            squares[4] = 6;
            squares[60] = -6;
            squares[11] = 1;
            squares[28] = -1;
            assertEquals("", game.apply(A, "", "move", 11, game.view(A).revision()));
            assertEquals("", game.apply(A, "", "move", 27, game.view(A).revision()));
            assertEquals("", game.apply(B, "", "move", 28, game.view(B).revision()));
            assertEquals("", game.apply(B, "", "move", 19, game.view(B).revision()));
            assertEquals(-1, squares[19]);
            assertEquals(0, squares[27]);
        } catch (ReflectiveOperationException e) {
            fail(e);
        }
    }

    @Test void pawnPromotesThroughButtons() {
        TableGame game = playing();
        ChessGame rules = (ChessGame) game.rules();
        try {
            var board = ChessGame.class.getDeclaredField("board");
            board.setAccessible(true);
            int[] squares = (int[]) board.get(rules);
            java.util.Arrays.fill(squares, 0);
            squares[4] = 6;
            squares[60] = -6;
            squares[48] = 1;
            assertEquals("", game.apply(A, "", "move", 48, game.view(A).revision()));
            assertEquals("", game.apply(A, "", "move", 56, game.view(A).revision()));
            assertTrue(game.view(A).buttons().contains("promote_queen"));
            assertEquals("", game.apply(A, "", "promote_queen", -1, game.view(A).revision()));
            assertEquals(5, squares[56]);
        } catch (ReflectiveOperationException e) {
            fail(e);
        }
    }
}
