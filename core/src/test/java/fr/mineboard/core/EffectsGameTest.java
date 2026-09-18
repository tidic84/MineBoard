package fr.mineboard.core;

import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class EffectsGameTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2);

    private TableGame playing(long seed) {
        TableGame game = new TableGame(new Random(seed));
        game.join(A, "Alice");
        game.apply(A, "Alice", "game", -1, 0);
        game.apply(A, "Alice", "game", -1, 0);
        assertEquals(Games.EFFECTS, game.id());
        game.join(B, "Bob");
        game.ready(A);
        game.ready(B);
        assertEquals("", game.start(A));
        return game;
    }
    private int indexOf(TableGame.View view, int number) {
        for (int i = 0; i < view.hand().size(); i++) {
            Card card = Card.fromId(view.hand().get(i));
            if (card.number() == number && card.matches(Card.fromId(view.topCard()))) return i;
        }
        return -1;
    }
    @Test void zeroSkipsTheOpponent() {
        for (int seed = 0; seed < 80; seed++) {
            TableGame game = playing(seed);
            TableGame.View view = game.view(A);
            int skip = indexOf(view, 0);
            if (skip < 0) continue;
            assertEquals("", game.play(A, skip, view.revision()));
            assertEquals(0, game.view(A).turn());
            assertEquals("skipped", game.view(A).event());
            return;
        }
        fail("no skip card found");
    }
    @Test void twoGivesTheOpponentTwoCardsAndSkipsThem() {
        for (int seed = 0; seed < 80; seed++) {
            TableGame game = playing(seed);
            TableGame.View view = game.view(A);
            int plus = indexOf(view, 2);
            if (plus < 0) continue;
            int before = game.view(B).seats().get(1).count();
            assertEquals("", game.play(A, plus, view.revision()));
            assertEquals(0, game.view(A).turn());
            assertEquals(before + 2, game.view(B).seats().get(1).count());
            assertEquals("plus2", game.view(A).event());
            return;
        }
        fail("no plus-two card found");
    }
}
