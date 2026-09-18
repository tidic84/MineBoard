package fr.mineboard.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TableGameTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), OBSERVER = new UUID(0, 3);
    private TableGame lobby(long seed) {
        TableGame game = new TableGame(new Random(seed));
        assertEquals("", game.join(A, "Alice")); assertEquals("", game.join(B, "Bob"));
        return game;
    }
    private TableGame playing(long seed) {
        TableGame game = lobby(seed);
        game.ready(A); game.ready(B); assertEquals("", game.start(A)); return game;
    }
    @Test void lobbyRequiresTwoReadyPlayersAndHost() {
        TableGame game = lobby(1);
        assertEquals("full", game.join(OBSERVER, "Charlie"));
        assertEquals("host_only", game.start(B));
        assertEquals("not_ready", game.start(A));
        game.ready(A); assertEquals("not_ready", game.start(A));
        game.ready(B); assertEquals("", game.start(A));
        assertEquals("already_started", game.ready(A));
    }
    @Test void observersAndPublicChunkStateNeverContainPrivateHands() {
        TableGame game = playing(2);
        assertEquals(7, game.view(A).hand().size());
        assertEquals(7, game.view(B).hand().size());
        assertTrue(game.view(OBSERVER).hand().isEmpty());
        assertTrue(game.view(null).hand().isEmpty());
        assertEquals(-1, game.view(null).yourSeat());
        assertEquals(65, game.view(A).deckCount());
        assertThrows(UnsupportedOperationException.class, () -> game.view(A).hand().clear());
    }
    @Test void staleAndUnauthorizedInputsCannotChangeGame() {
        TableGame game = playing(3);
        TableGame.View before = game.view(A);
        assertEquals("not_your_turn", game.draw(B, before.revision()));
        assertEquals("not_your_turn", game.play(OBSERVER, 0, before.revision()));
        assertEquals("stale", game.play(A, 0, before.revision() - 1));
        assertEquals("invalid_card", game.play(A, -1, before.revision()));
        assertEquals("invalid_card", game.play(A, Integer.MAX_VALUE, before.revision()));
        assertEquals(before, game.view(A));
    }
    @Test void illegalCardIsRejectedAndLegalPlayChangesTurn() {
        TableGame game = playing(4);
        TableGame.View view = game.view(A);
        int playable = -1;
        for (int i = 0; i < view.hand().size(); i++) {
            if (Card.fromId(view.hand().get(i)).matches(Card.fromId(view.topCard()))) playable = i;
            else assertEquals("cannot_play", game.play(A, i, view.revision()));
        }
        assertTrue(playable >= 0);
        assertEquals("", game.play(A, playable, view.revision()));
        assertEquals(view.hand().get(playable), game.view(B).topCard());
        assertEquals(1, game.view(B).turn());
        assertEquals(6, game.view(A).hand().size());
    }
    @Test void drawEndsTurnAndDuplicatePacketIsRejected() {
        TableGame game = playing(5);
        long revision = game.view(A).revision();
        assertEquals("", game.draw(A, revision));
        assertEquals(8, game.view(A).hand().size());
        assertEquals(1, game.view(A).turn());
        assertEquals("not_your_turn", game.draw(A, revision));
    }
    @Test void departureResetsRoundAndPromotesRemainingPlayer() {
        TableGame game = playing(6);
        game.leave(A);
        assertEquals(TableGame.Phase.LOBBY, game.view(B).phase());
        assertTrue(game.view(B).hand().isEmpty());
        assertFalse(game.view(B).seats().get(1).ready());
        assertEquals("not_ready", game.start(B));
        assertEquals("", game.join(OBSERVER, "Charlie"));
    }
    @Test void exhaustedDeckAllowsPassingInsteadOfDeadlocking() {
        TableGame game = playing(7);
        for (int i = 0; i < 65; i++) {
            UUID player = game.view(A).turn() == 0 ? A : B;
            assertEquals("", game.draw(player, game.view(player).revision()));
        }
        assertEquals(0, game.view(A).deckCount());
        int previousTurn = game.view(A).turn();
        UUID player = previousTurn == 0 ? A : B;
        assertEquals("", game.draw(player, game.view(player).revision()));
        assertEquals(1 - previousTurn, game.view(A).turn());
        assertEquals("passed", game.view(A).event());
        assertEquals(79, game.view(A).seats().stream().mapToInt(TableGame.Seat::count).sum());
    }
    @Test void recyclingKeepsTopCardAndConservesTheDeck() {
        TableGame game = playing(4);
        TableGame.View initial = game.view(A);
        int index = -1;
        for (int i = 0; i < initial.hand().size(); i++) {
            if (Card.fromId(initial.hand().get(i)).matches(Card.fromId(initial.topCard()))) { index = i; break; }
        }
        assertTrue(index >= 0);
        assertEquals("", game.play(A, index, initial.revision()));
        int top = game.view(A).topCard();
        for (int i = 0; i < 66; i++) {
            UUID player = game.view(A).turn() == 0 ? A : B;
            assertEquals("", game.draw(player, game.view(player).revision()));
        }
        assertEquals(top, game.view(A).topCard());
        assertEquals(0, game.view(A).deckCount());
        assertEquals(79, game.view(A).seats().stream().mapToInt(TableGame.Seat::count).sum());
        assertEquals("drew", game.view(A).event());
    }
    @Test void spectatorDepartureDoesNotCancelTheRound() {
        TableGame game = playing(8);
        TableGame.View before = game.view(A);
        game.leave(OBSERVER);
        assertEquals(before, game.view(A));
    }
    @Test void completeRoundsAcrossManyShufflesAndRematch() {
        for (int seed = 0; seed < 100; seed++) {
            TableGame game = playing(seed);
            int moves = 0;
            while (game.view(A).phase() == TableGame.Phase.PLAYING && moves++ < 3000) {
                UUID player = game.view(A).turn() == 0 ? A : B;
                TableGame.View view = game.view(player);
                int playable = -1;
                for (int i = 0; i < view.hand().size(); i++) if (Card.fromId(view.hand().get(i)).matches(Card.fromId(view.topCard()))) { playable = i; break; }
                String error = playable < 0 ? game.draw(player, view.revision()) : game.play(player, playable, view.revision());
                assertEquals("", error, "seed " + seed + ", move " + moves);
                assertTrue(game.view(null).hand().isEmpty());
            }
            assertEquals(TableGame.Phase.FINISHED, game.view(A).phase(), "seed " + seed);
            assertEquals(0, game.view(A).seats().get(game.view(A).winner()).count());
            assertEquals("", game.rematch(A));
            assertEquals(TableGame.Phase.LOBBY, game.view(A).phase());
        }
    }
    @Test void cardIdsAndMatchingAreValidated() {
        for (int i = 0; i < 40; i++) assertEquals(i, Card.fromId(i).id());
        assertThrows(IllegalArgumentException.class, () -> Card.fromId(-1));
        assertThrows(IllegalArgumentException.class, () -> Card.fromId(40));
        assertTrue(new Card(0, 5).matches(new Card(1, 5)));
        assertFalse(new Card(0, 5).matches(new Card(1, 6)));
    }
    @Test void viewReportsTurnActionsWithoutNamingAGame() {
        TableGame discard = playing(1);
        assertTrue(discard.view(A).hasTurnAction());
        assertTrue(discard.view(A).buttons().contains("draw"));
        assertTrue(discard.view(A).allows("draw") || !discard.view(A).playable().isEmpty());
        TableGame memory = new TableGame(new Random(1));
        memory.join(A, "Alice");
        memory.apply(A, "Alice", "game", -1, 0);
        memory.join(B, "Bob");
        memory.ready(A);
        memory.ready(B);
        assertEquals("", memory.start(A));
        assertTrue(memory.view(A).hasTurnAction());
        assertTrue(memory.view(A).buttons().isEmpty());
        assertFalse(memory.view(A).allows("draw"));
        assertTrue(memory.view(A).allows("flip"));
    }
}
