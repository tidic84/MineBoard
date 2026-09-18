package fr.mineboard.core;

import java.util.List;

/**
 * Footprints of table pieces. Card item models occupy 8×12 sixteenths
 * (0.5 × 0.75 blocks at scale 1), not a square.
 */
public final class Layouts {
    public static final double CARD_X = 8 / 16.0;
    public static final double CARD_Z = 12 / 16.0;
    public static final float MEMORY_SCALE = 0.23f;
    public static final double MEMORY_GAP = 0.022;
    public static final int CHECKER_SIZE = 8;
    public static final double CHECKER_CELL = 0.112;
    public static final double CHECKER_ORIGIN = (1 - CHECKER_SIZE * CHECKER_CELL) / 2 + CHECKER_CELL / 2;
    public static final float CHECKER_TOKEN = (float) CHECKER_CELL;
    private static final double TOKEN_MODEL = 10 / 16.0;

    private Layouts() {}

    public static double cardHalfX(float scale) { return CARD_X * scale * 0.5; }
    public static double cardHalfZ(float scale) { return CARD_Z * scale * 0.5; }

    /** Centers of a centered axis-aligned grid: startX, startZ, stepX, stepZ. */
    public static double[] grid(int cols, int rows, double pieceX, double pieceZ, double gap) {
        double totalX = cols * pieceX + Math.max(0, cols - 1) * gap;
        double totalZ = rows * pieceZ + Math.max(0, rows - 1) * gap;
        return new double[] {
            (1 - totalX) / 2 + pieceX / 2,
            (1 - totalZ) / 2 + pieceZ / 2,
            pieceX + gap,
            pieceZ + gap
        };
    }

    public static boolean hits(TableGame.Piece piece, double x, double z) {
        double dx = x - piece.x(), dz = z - piece.z();
        double rad = Math.toRadians(piece.angle());
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double localX = dx * cos + dz * sin;
        double localZ = -dx * sin + dz * cos;
        double halfX;
        double halfZ;
        if ("cell".equals(piece.kind())) {
            halfX = halfZ = piece.scale() * 0.48;
        } else if ("token".equals(piece.kind())) {
            halfX = halfZ = piece.scale() * TOKEN_MODEL * 0.5;
        } else {
            halfX = cardHalfX(piece.scale());
            halfZ = cardHalfZ(piece.scale());
        }
        return Math.abs(localX) <= halfX && Math.abs(localZ) <= halfZ;
    }

    /** Smallest square, in blocks, that frames every piece around the table center. */
    public static double span(List<TableGame.Piece> pieces) {
        double extent = 0.5;
        for (TableGame.Piece piece : pieces) {
            double halfX;
            double halfZ;
            if ("cell".equals(piece.kind()) || "token".equals(piece.kind())) {
                halfX = halfZ = piece.scale() * 0.5;
            } else {
                halfX = cardHalfX(piece.scale());
                halfZ = cardHalfZ(piece.scale());
            }
            extent = Math.max(extent, Math.max(Math.abs(piece.x() - 0.5) + halfX, Math.abs(piece.z() - 0.5) + halfZ));
        }
        return Math.max(1, extent * 2 + 0.08);
    }

    public static int customModelData(String kind, int card) {
        return switch (kind) {
            case "cell" -> card == 0 ? 41 : 42;
            case "token" -> switch (card) {
                case -1 -> 44;
                case 2 -> 45;
                case -2 -> 46;
                default -> 43;
            };
            default -> Math.max(0, Math.min(40, card + 1));
        };
    }

    public static String itemModel(String kind, int card) {
        return switch (kind) {
            case "cell" -> card == 0 ? "cell_light" : "cell_dark";
            case "token" -> switch (card) {
                case -1 -> "token_dark";
                case 2 -> "token_king_light";
                case -2 -> "token_king_dark";
                default -> "token_light";
            };
            default -> card < 0 ? "card" : "card_" + card;
        };
    }
}
