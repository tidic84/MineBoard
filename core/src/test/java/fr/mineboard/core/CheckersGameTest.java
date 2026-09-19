package fr.mineboard.core;

import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CheckersGameTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2);

    private TableGame playing() {
        TableGame game = new TableGame(new Random(1));
        game.join(A, "Alice");
        for (int i = 0; i < 3; i++) game.apply(A, "Alice", "game", -1, 0);
        assertEquals(Games.CHECKERS, game.id());
        game.join(B, "Bob");
        game.ready(A);
        game.ready(B);
        assertEquals("", game.start(A));
        return game;
    }
    @Test void openingMoveAdvancesAMan() {
        TableGame game = playing();
        assertEquals(12, game.view(A).seats().get(0).count());
        assertEquals(12, game.view(B).seats().get(1).count());
        int from = 2 * 8 + 1;
        int to = 3 * 8 + 2;
        assertEquals("", game.apply(A, "Alice", "move", from, game.view(A).revision()));
        assertEquals("", game.apply(A, "Alice", "move", to, game.view(A).revision()));
        assertEquals(1, game.view(A).turn());
        assertEquals(12, game.view(A).seats().get(0).count());
    }
    @Test void opponentCannotMoveOnTheFirstTurn() {
        TableGame game = playing();
        assertEquals("not_your_turn", game.apply(B, "Bob", "move", 5 * 8 + 0, game.view(B).revision()));
    }
    private void move(TableGame game, UUID player, int square) {
        assertEquals("", game.apply(player, "", "move", square, game.view(player).revision()));
    }
    @Test void captureCannotBeReplacedByASimpleStep() {
        TableGame game = playing();
        move(game, A, 17); move(game, A, 26);
        move(game, B, 40); move(game, B, 33);
        move(game, A, 26);
        assertEquals("cannot_play", game.apply(A, "", "move", 35, game.view(A).revision()));
        move(game, A, 40);
        assertEquals(11, game.view(A).seats().get(1).count());
    }
    @Test void clickingSelectedTokenAgainDeselectsIt() {
        TableGame game = playing();
        move(game, A, 17); move(game, A, 17);
        assertEquals("cannot_play", game.apply(A, "", "move", 26, game.view(A).revision()));
    }
    @Test void multipleCaptureCannotBeInterruptedWithAStep() throws Exception {
        TableGame game = playing();
        var field = CheckersGame.class.getDeclaredField("board");
        field.setAccessible(true);
        int[] board = (int[]) field.get(game.rules());
        java.util.Arrays.fill(board, 0);
        board[17] = 1; board[26] = -1; board[44] = -1;
        move(game, A, 17); move(game, A, 35);
        assertEquals(0, game.view(A).turn());
        assertEquals("cannot_play", game.apply(A, "", "move", 42, game.view(A).revision()));
        move(game, A, 53);
        assertEquals(TableGame.Phase.FINISHED, game.view(A).phase());
        assertEquals(0, game.view(A).winner());
    }
}
