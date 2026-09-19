package fr.mineboard.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class LayoutsTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2);

    @Test void pickingUsesTheSameYRotationAsTheRenderedCard() {
        var piece = new TableGame.Piece(0, 0, .5, .16, .5, 45, 1, "memory", "flip");
        double sin = Math.sin(Math.PI / 4), cos = Math.cos(Math.PI / 4);
        double localX = .24, localZ = .36;
        assertTrue(Layouts.hits(piece, .5 + cos * localX + sin * localZ,
            .5 - sin * localX + cos * localZ));
        assertFalse(Layouts.hits(piece, .5 + cos * .27, .5 - sin * .27));
    }

    @Test void rotatedCardsRemainInsideCameraFraming() {
        var piece = new TableGame.Piece(0, 0, 1, .16, .5, 90, 1, "tableau", "");
        assertTrue(Layouts.span(List.of(piece)) >= 1.75);
    }

    @Test void completedDraftCardsDoNotOverlapOrLeaveTheFelt() {
        TableGame game = new TableGame(new Random(2));
        game.join(A, "Alice");
        for (int i = 0; i < 4; i++) game.apply(A, "", "game", -1, 0);
        game.join(B, "Bob"); game.ready(A); game.ready(B); game.start(A);
        for (int i = 0; i < 7; i++) { game.play(A, 0, -1); game.play(B, 0, -1); }
        var pieces = game.view(A).pieces();
        assertEquals(14, pieces.size());
        for (int i = 0; i < pieces.size(); i++) {
            var a = pieces.get(i);
            assertTrue(a.x() - Layouts.cardHalfX(a.scale()) > .03125);
            assertTrue(a.x() + Layouts.cardHalfX(a.scale()) < .96875);
            for (int j = i + 1; j < pieces.size(); j++) {
                var b = pieces.get(j);
                assertFalse(Math.abs(a.x() - b.x()) < Layouts.CARD_X * a.scale()
                    && Math.abs(a.z() - b.z()) < Layouts.CARD_Z * a.scale());
            }
        }
    }

    @Test void memoryCardsStayApartOnARectangularGrid() {
        TableGame game = new TableGame(new Random(1));
        game.join(A, "Alice");
        game.apply(A, "Alice", "game", -1, 0);
        game.join(B, "Bob");
        game.ready(A);
        game.ready(B);
        assertEquals("", game.start(A));
        List<TableGame.Piece> pieces = game.view(A).pieces();
        assertEquals(MemoryGame.TOTAL, pieces.size());
        float scale = Layouts.MEMORY_SCALE;
        double minX = Layouts.cardHalfX(scale) * 2 + 0.001;
        double minZ = Layouts.cardHalfZ(scale) * 2 + 0.001;
        assertTrue(minZ > minX);
        for (int i = 0; i < pieces.size(); i++) {
            TableGame.Piece a = pieces.get(i);
            assertTrue(a.x() - Layouts.cardHalfX(scale) >= 0.04, "left " + i);
            assertTrue(a.x() + Layouts.cardHalfX(scale) <= 0.96, "right " + i);
            assertTrue(a.z() - Layouts.cardHalfZ(scale) >= 0.04, "near " + i);
            assertTrue(a.z() + Layouts.cardHalfZ(scale) <= 0.96, "far " + i);
            for (int j = i + 1; j < pieces.size(); j++) {
                TableGame.Piece b = pieces.get(j);
                boolean overlap = Math.abs(a.x() - b.x()) < minX && Math.abs(a.z() - b.z()) < minZ;
                assertFalse(overlap, i + " overlaps " + j);
            }
        }
    }

    @Test void checkersUsesBoardTokensInsteadOfPlayingCards() {
        TableGame game = new TableGame(new Random(1));
        game.join(A, "Alice");
        for (int i = 0; i < 3; i++) game.apply(A, "Alice", "game", -1, 0);
        assertEquals(Games.CHECKERS, game.id());
        game.join(B, "Bob");
        game.ready(A);
        game.ready(B);
        assertEquals("", game.start(A));
        List<TableGame.Piece> pieces = game.view(A).pieces();
        List<TableGame.Piece> cells = pieces.stream().filter(piece -> piece.kind().equals("cell")).toList();
        List<TableGame.Piece> tokens = pieces.stream().filter(piece -> piece.kind().equals("token")).toList();
        assertEquals(64, cells.size());
        assertEquals(24, tokens.size());
        assertTrue(tokens.stream().allMatch(piece -> Math.abs(piece.card()) == 1 || Math.abs(piece.card()) == 2));
        assertEquals("cell_dark", Layouts.itemModel("cell", 1));
        assertEquals("token_light", Layouts.itemModel("token", 1));
        assertEquals("token_king_dark", Layouts.itemModel("token", -2));
        assertEquals(43, Layouts.customModelData("token", 1));
        assertTrue(Layouts.span(pieces) >= 1);
        for (int i = 0; i < tokens.size(); i++) {
            for (int j = i + 1; j < tokens.size(); j++) {
                TableGame.Piece a = tokens.get(i), b = tokens.get(j);
                double min = Layouts.CHECKER_CELL * 0.6;
                assertFalse(Math.abs(a.x() - b.x()) < min && Math.abs(a.z() - b.z()) < min,
                    "tokens overlap " + a.id() + "/" + b.id());
            }
        }
    }
}
