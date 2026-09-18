package fr.mineboard.core;

import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MemoryGameTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2);

    private TableGame lobby(long seed) {
        TableGame game = new TableGame(new Random(seed));
        assertEquals("", game.join(A, "Alice"));
        assertEquals("", game.apply(A, "Alice", "game", -1, 0));
        assertEquals(Games.MEMORY, game.id());
        assertEquals("", game.join(B, "Bob"));
        return game;
    }
    private TableGame playing(long seed) {
        TableGame game = lobby(seed);
        game.ready(A);
        game.ready(B);
        assertEquals("", game.start(A));
        return game;
    }
    private int openCard(TableGame.View view, int index) {
        for (TableGame.Piece piece : view.pieces()) if (piece.id() == index) return piece.card();
        return -1;
    }
    @Test void hostCanCycleGamesInLobbyAndMustReadyAgain() {
        TableGame game = new TableGame(new Random(1));
        game.join(A, "Alice");
        game.ready(A);
        assertEquals(Games.DISCARD, game.id());
        assertEquals("", game.apply(A, "Alice", "game", -1, 0));
        assertEquals(Games.MEMORY, game.id());
        assertFalse(game.view(A).seats().getFirst().ready());
        assertEquals("not_ready", game.start(A));
    }
    @Test void mismatchPassesTurnAndHidesOnNextFlip() {
        TableGame game = playing(3);
        MemoryGame rules = (MemoryGame) game.rules();
        int first = 0;
        int other = -1;
        for (int i = 1; i < MemoryGame.TOTAL; i++) if (rules.cardAt(i) != rules.cardAt(first)) { other = i; break; }
        assertTrue(other > 0);
        long revision = game.view(A).revision();
        assertEquals("", game.apply(A, "Alice", "flip", first, revision));
        assertEquals(rules.cardAt(first), openCard(game.view(A), first));
        assertEquals("", game.apply(A, "Alice", "flip", other, game.view(A).revision()));
        assertEquals(1, game.view(A).turn());
        assertEquals(rules.cardAt(other), openCard(game.view(B), other));
        int third = -1;
        for (int i = 0; i < MemoryGame.TOTAL; i++) if (i != first && i != other) { third = i; break; }
        assertEquals("", game.apply(B, "Bob", "flip", third, game.view(B).revision()));
        assertEquals(-1, openCard(game.view(A), first));
        assertEquals(-1, openCard(game.view(B), other));
        assertEquals(rules.cardAt(third), openCard(game.view(B), third));
    }
    @Test void matchingPairStaysWithTheSamePlayer() {
        TableGame game = playing(4);
        MemoryGame rules = (MemoryGame) game.rules();
        int first = 0;
        int match = -1;
        for (int i = 1; i < MemoryGame.TOTAL; i++) if (rules.cardAt(i) == rules.cardAt(first)) { match = i; break; }
        assertTrue(match > 0);
        final int pair = match;
        assertEquals("", game.apply(A, "Alice", "flip", first, game.view(A).revision()));
        assertEquals("", game.apply(A, "Alice", "flip", pair, game.view(A).revision()));
        assertEquals(0, game.view(A).turn());
        assertEquals(1, game.view(A).seats().getFirst().count());
        assertTrue(game.view(A).pieces().stream().noneMatch(piece -> piece.id() == first || piece.id() == pair));
        assertEquals("matched", game.view(A).event());
    }
    @Test void observersSeeTheSamePublicBoardAndNoHand() {
        TableGame game = playing(5);
        assertEquals("", game.apply(A, "Alice", "flip", 0, game.view(A).revision()));
        TableGame.View observer = game.view(new UUID(0, 9));
        assertTrue(observer.hand().isEmpty());
        assertEquals(openCard(game.view(A), 0), openCard(observer, 0));
        assertTrue(game.view(null).hand().isEmpty());
    }
    @Test void completeRoundsUntilEveryPairIsTaken() {
        for (int seed = 0; seed < 40; seed++) {
            TableGame game = playing(seed);
            MemoryGame rules = (MemoryGame) game.rules();
            int moves = 0;
            while (game.view(A).phase() == TableGame.Phase.PLAYING && moves++ < 200) {
                UUID player = game.view(A).turn() == 0 ? A : B;
                TableGame.View view = game.view(player);
                int first = view.moves().getFirst().target();
                assertEquals("", game.apply(player, "", "flip", first, view.revision()));
                if (game.view(A).phase() != TableGame.Phase.PLAYING) break;
                view = game.view(player);
                int match = -1;
                for (TableGame.Move move : view.moves()) if (rules.cardAt(move.target()) == rules.cardAt(first)) match = move.target();
                assertTrue(match >= 0, "seed " + seed);
                assertEquals("", game.apply(player, "", "flip", match, view.revision()), "seed " + seed);
            }
            assertEquals(TableGame.Phase.FINISHED, game.view(A).phase(), "seed " + seed);
            assertEquals(8, game.view(A).seats().get(0).count() + game.view(B).seats().get(1).count());
        }
    }
}
