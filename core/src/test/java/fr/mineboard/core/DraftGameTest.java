package fr.mineboard.core;

import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DraftGameTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2);

    private TableGame playing() {
        TableGame game = new TableGame(new Random(2));
        game.join(A, "Alice");
        for (int i = 0; i < 4; i++) game.apply(A, "Alice", "game", -1, 0);
        assertEquals(Games.DRAFT, game.id());
        game.join(B, "Bob");
        game.ready(A);
        game.ready(B);
        assertEquals("", game.start(A));
        assertEquals(-1, game.view(A).turn());
        return game;
    }
    @Test void bothPlayersPickThenHandsArePassed() {
        TableGame game = playing();
        assertEquals(7, game.view(A).hand().size());
        assertEquals("", game.apply(A, "Alice", "play", 0, game.view(A).revision()));
        assertEquals(7, game.view(A).hand().size());
        assertTrue(game.view(A).playable().isEmpty());
        assertEquals("", game.apply(B, "Bob", "play", 0, game.view(B).revision()));
        assertEquals(6, game.view(A).hand().size());
        assertEquals(1, game.view(A).seats().get(0).count());
        assertEquals(1, game.view(B).seats().get(1).count());
    }
    @Test void pendingChoiceDoesNotRevealTheSelectedCardIdentity() {
        TableGame game = playing();
        var before = game.view(null).pieces();
        assertEquals("", game.apply(A, "", "play", 3, game.view(A).revision()));
        assertEquals(before, game.view(null).pieces());
    }
    @Test void sevenPicksFinishTheRound() {
        TableGame game = playing();
        for (int round = 0; round < 7; round++) {
            assertEquals("", game.apply(A, "Alice", "play", 0, -1));
            assertEquals("", game.apply(B, "Bob", "play", 0, -1));
        }
        assertEquals(TableGame.Phase.FINISHED, game.view(A).phase());
        assertEquals(7, game.view(A).seats().get(0).count());
    }
    @Test void lobbyAndCancelledRoundExposeEmptyHands() {
        TableGame game = playing();
        game.leave(B);
        assertEquals(TableGame.Phase.LOBBY, game.view(A).phase());
        assertTrue(game.view(A).hand().isEmpty());
        assertTrue(game.view(null).hand().isEmpty());
        game.join(B, "Bob");
        assertTrue(game.view(B).hand().isEmpty());
    }
    @Test void switchingToDraftIsSafeBeforeDealing() {
        TableGame game = new TableGame(new Random(2));
        game.join(A, "Alice");
        for (int i = 0; i < 4; i++) game.apply(A, "", "game", -1, 0);
        assertEquals(Games.DRAFT, game.id());
        assertTrue(game.view(A).hand().isEmpty());
    }
    @Test void simultaneousPicksAllowSameRevisionButRejectPreviousHand() {
        TableGame game = playing();
        long revision = game.view(A).revision();
        assertEquals("", game.play(A, 0, revision));
        assertEquals("", game.play(B, 0, revision));
        assertEquals("stale", game.play(A, 0, revision));
        assertEquals(6, game.view(A).hand().size());
        assertEquals("", game.play(A, 0, game.view(A).revision()));
    }
}
