package fr.mineboard.core;

import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AgesGameTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3), D = new UUID(0, 4);

    private int payment(AgesGame rules, int cost) throws Exception {
        var method = AgesGame.class.getDeclaredMethod("pay", int.class, int.class, int.class, boolean.class);
        method.setAccessible(true);
        return (int) method.invoke(rules, 0, cost, 0, false);
    }

    @SuppressWarnings("unchecked")
    private void giveCity(AgesGame rules, int seat, String key) throws Exception {
        var card = java.util.Arrays.stream(AgesCard.ALL).filter(c -> c.key.equals(key)).findFirst().orElseThrow();
        var held = Class.forName("fr.mineboard.core.AgesGame$Held").getDeclaredConstructor(AgesCard.class, int.class);
        held.setAccessible(true);
        ((java.util.List<java.util.List<Object>>) field("cities").get(rules)).get(seat).add(held.newInstance(card, 9000));
    }

    @Test void oneNeighborResourceCannotBePurchasedTwice() throws Exception {
        AgesGame rules = (AgesGame) cities(11).rules();
        field("wonder").set(rules, new int[] {5, 3, 4}); // wood, stone, clay
        field("coins").set(rules, new int[] {30, 3, 3});
        assertEquals(2, payment(rules, AgesCard.pack(AgesCard.S, 1)));
        assertEquals(-1, payment(rules, AgesCard.pack(AgesCard.S, 2)));
    }

    @Test void neighborChoiceProductionCannotSupplyBothAlternatives() throws Exception {
        AgesGame rules = (AgesGame) cities(12).rules();
        field("wonder").set(rules, new int[] {5, 5, 5});
        field("coins").set(rules, new int[] {30, 3, 3});
        giveCity(rules, 1, "pit"); // one clay OR one ore
        assertEquals(2, payment(rules, AgesCard.pack(AgesCard.C, 1)));
        assertEquals(-1, payment(rules, AgesCard.pack(AgesCard.C, 1) | AgesCard.pack(AgesCard.O, 1)));
    }

    @Test void choosingACardDoesNotRevealWhichPublicCardBackWasSelected() {
        TableGame game = cities(13);
        var before = game.view(null).pieces();
        assertEquals("", game.apply(A, "", "discard", 3, game.view(A).revision()));
        assertEquals(before, game.view(null).pieces());
    }

    @Test @SuppressWarnings("unchecked") void salvageBuildsAnUnaffordableCardForFree() throws Exception {
        TableGame game = cities(14);
        AgesGame rules = (AgesGame) game.rules();
        field("coins").set(rules, new int[] {0, 0, 0});
        field("salvageSeat").setInt(rules, 0);
        var card = java.util.Arrays.stream(AgesCard.ALL).filter(c -> c.key.equals("palace")).findFirst().orElseThrow();
        var held = Class.forName("fr.mineboard.core.AgesGame$Held").getDeclaredConstructor(AgesCard.class, int.class);
        held.setAccessible(true);
        ((java.util.List<Object>) field("discarded").get(rules)).add(held.newInstance(card, 9100));
        assertTrue(game.view(A).allows("salvage", 0));
        assertEquals("invalid_card", game.apply(A, "", "salvage", 99, game.view(A).revision()));
        assertEquals("", game.apply(A, "", "salvage", 0, game.view(A).revision()));
        assertEquals(1, ((java.util.List<java.util.List<?>>) field("cities").get(rules)).getFirst().size());
        assertEquals(0, game.view(A).seats().getFirst().count());
    }

    @Test void completeGamesFinishForEverySupportedPlayerCount() {
        for (int count = 3; count <= 7; count++) {
            TableGame game = new TableGame(new Random(count));
            game.join(A, "Alice");
            for (int i = 0; i < 5; i++) game.apply(A, "", "game", -1, 0);
            for (int i = 1; i < count; i++) game.join(new UUID(0, i + 1), "Player" + i);
            for (int i = 0; i < count; i++) game.ready(new UUID(0, i + 1));
            assertEquals("", game.start(A));
            for (int round = 0; round < 18; round++) {
                long revision = game.view(A).revision();
                for (int i = 0; i < count; i++) {
                    UUID player = new UUID(0, i + 1);
                    var view = game.view(player);
                    String action = view.playable().isEmpty() ? "discard" : "play";
                    int index = view.playable().isEmpty() ? 0 : view.playable().getFirst();
                    assertEquals("", game.apply(player, "", action, index, revision));
                }
            }
            assertEquals(TableGame.Phase.FINISHED, game.view(A).phase(), "players=" + count);
        }
    }

    private TableGame cities(long seed) {
        TableGame game = new TableGame(new Random(seed));
        game.join(A, "Alice");
        for (int i = 0; i < 5; i++) game.apply(A, "", "game", -1, 0);
        assertEquals(Games.WONDERS, game.id());
        game.join(B, "Bob");
        game.join(C, "Cara");
        game.ready(A); game.ready(B); game.ready(C);
        assertEquals("", game.start(A));
        return game;
    }

    @Test void threePlayersCanStartButTwoCannot() {
        TableGame game = new TableGame(new Random(1));
        game.join(A, "Alice");
        for (int i = 0; i < 5; i++) game.apply(A, "", "game", -1, 0);
        game.join(B, "Bob");
        game.ready(A); game.ready(B);
        assertEquals("not_ready", game.start(A));
        game.join(C, "Cara");
        game.ready(C);
        assertEquals("", game.start(A));
        assertEquals(3, game.view(A).seats().size());
        assertEquals(7, game.view(A).hand().size());
        assertEquals(3, game.view(A).seats().get(0).count());
    }

    @Test void discardingACardPaysThreeCoinsAndKeepsHandsPrivate() {
        TableGame game = cities(2);
        long revision = game.view(A).revision();
        assertEquals("", game.apply(A, "", "discard", 0, revision));
        assertEquals("", game.apply(B, "", "discard", 0, revision));
        assertEquals("", game.apply(C, "", "discard", 0, revision));
        assertEquals(6, game.view(A).hand().size());
        assertEquals(6, game.view(A).seats().get(0).count());
        assertTrue(game.view(null).hand().isEmpty());
        assertTrue(game.view(D).hand().isEmpty());
    }

    @Test void warTokensChangeAfterAnAgeWhenMilitaryDiffers() {
        TableGame game = cities(3);
        AgesGame rules = (AgesGame) game.rules();
        try {
            java.lang.reflect.Field shields = field("shields");
            int[] values = (int[]) shields.get(rules);
            values[0] = 4;
            values[1] = 0;
            values[2] = 0;
            field("age").setInt(rules, 1);
            var method = AgesGame.class.getDeclaredMethod("resolveWar", int.class);
            method.setAccessible(true);
            method.invoke(rules, 3);
            int[] war = (int[]) field("war").get(rules);
            assertEquals(2, war[0]);
            assertEquals(-1, war[1]);
            assertEquals(-1, war[2]);
        } catch (ReflectiveOperationException e) {
            fail(e);
        }
    }

    @Test void neighborBoardsAreLaidOutAroundTheTable() {
        TableGame game = cities(4);
        var pieces = game.view(A).pieces();
        long wonders = pieces.stream().filter(piece -> piece.kind().equals("wonder")).count();
        assertEquals(3, wonders);
        assertTrue(Layouts.span(pieces) >= 2);
        double[] left = Layouts.seatCenter(1, 3);
        double[] self = Layouts.seatCenter(0, 3);
        assertTrue(Math.abs(left[0] - self[0]) > 0.2 || Math.abs(left[1] - self[1]) > 0.2);
        long hands = pieces.stream().filter(piece -> piece.kind().equals("seat")).count();
        assertTrue(hands >= 3 * 6);
    }

    @Test void aFullAgeDraftConsumesSevenCardsEach() {
        TableGame game = cities(5);
        for (int round = 0; round < 6; round++) {
            long revision = game.view(A).revision();
            assertEquals("", game.apply(A, "", "discard", 0, revision));
            assertEquals("", game.apply(B, "", "discard", 0, revision));
            assertEquals("", game.apply(C, "", "discard", 0, revision));
        }
        assertEquals(2, game.view(A).topCard());
        assertEquals(7, game.view(A).hand().size());
        assertEquals(24, game.view(A).seats().get(0).count());
    }

    @Test void ageDecksMatchPlayerCountsAndGuildsFillAgeThree() {
        assertEquals(10, AgesCard.guilds());
        for (int n = 3; n <= 7; n++) {
            assertEquals(n * 7, AgesCard.nonGuild(1, n), "age1 " + n);
            assertEquals(n * 7, AgesCard.nonGuild(2, n), "age2 " + n);
            assertEquals(n * 7, AgesCard.nonGuild(3, n) + n + 2, "age3 " + n);
        }
    }

    @Test void scienceUsesSquaresPlusSevenPerSet() {
        TableGame game = cities(6);
        AgesGame rules = (AgesGame) game.rules();
        try {
            var method = AgesGame.class.getDeclaredMethod("scienceOf", int[].class);
            method.setAccessible(true);
            assertEquals(10, (int) method.invoke(rules, (Object) new int[] { 1, 1, 1 }));
            assertEquals(13, (int) method.invoke(rules, (Object) new int[] { 2, 1, 1 }));
        } catch (ReflectiveOperationException e) {
            fail(e);
        }
    }

    private static java.lang.reflect.Field field(String name) throws NoSuchFieldException {
        var field = AgesGame.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
