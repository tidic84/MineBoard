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
}
