package fr.mineboard.core;

import java.util.List;

/** Client interpolation of 3D pieces between two public views. */
public final class PieceMotion {
    private PieceMotion() {}

    public static TableGame.Piece pose(List<TableGame.Piece> previous, TableGame.Piece next, double t) {
        return pose(previous, List.of(next), next, t);
    }

    public static TableGame.Piece pose(List<TableGame.Piece> previous, List<TableGame.Piece> current, TableGame.Piece next, double t) {
        t = Math.max(0, Math.min(1, t));
        double ease = 1 - Math.pow(1 - t, 3);
        TableGame.Piece from = find(previous, current, next);
        if (from == null && java.util.Set.of("cell", "wonder").contains(next.kind())) return next;
        double x0 = from == null ? 0.5 : from.x();
        double y0 = from == null ? 0.16 : from.y();
        double z0 = from == null ? 0.5 : from.z();
        float a0 = from == null ? next.angle() : from.angle();
        float s0 = from == null ? next.scale() * 0.4f : from.scale();
        double hopHeight = 0;
        boolean card = !java.util.Set.of("cell", "token", "chess", "wonder", "coin").contains(next.kind());
        if (from == null || from.card() != next.card()) hopHeight = card ? 0.18 : 0.1;
        else if (card && (from.x() != next.x() || from.z() != next.z())) hopHeight = 0.05;
        double hop = hopHeight == 0 ? 0 : Math.sin(t * Math.PI) * hopHeight;
        return new TableGame.Piece(next.id(), next.card(),
            lerp(x0, next.x(), ease), lerp(y0, next.y(), ease) + hop, lerp(z0, next.z(), ease),
            a0 + wrap(next.angle() - a0) * (float) ease,
            (float) lerp(s0, next.scale(), ease), next.kind(), next.action());
    }

    static TableGame.Piece find(List<TableGame.Piece> previous, TableGame.Piece next) {
        return find(previous, List.of(next), next);
    }
    private static TableGame.Piece find(List<TableGame.Piece> previous, List<TableGame.Piece> current, TableGame.Piece next) {
        if (previous == null) return null;
        boolean boardPiece = next.kind().equals("chess") || next.kind().equals("token");
        for (TableGame.Piece piece : previous) {
            if (piece.kind().equals(next.kind()) && piece.id() == next.id()
                && (!boardPiece || Integer.signum(piece.card()) == Integer.signum(next.card()))) return piece;
        }
        if (boardPiece) {
            // IDs are target squares for network actions, not identities. Match the
            // vacated square, including captures, promotion and each half of castling.
            for (TableGame.Piece piece : previous) {
                if (!piece.kind().equals(next.kind()) || Integer.signum(piece.card()) != Integer.signum(next.card())) continue;
                boolean promotion = Math.abs(piece.card()) == 1 && Math.abs(next.card()) > 1;
                if (piece.card() != next.card() && !promotion) continue;
                boolean stayed = current.stream().anyMatch(p -> p.kind().equals(piece.kind()) && p.id() == piece.id() && p.card() == piece.card());
                if (!stayed) return piece;
            }
        } else if (!java.util.Set.of("cell", "wonder", "coin").contains(next.kind())) {
            // Card IDs survive changes between deck, hand and discard pile.
            for (TableGame.Piece piece : previous) if (piece.id() == next.id()) return piece;
        }
        return null;
    }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static float wrap(float degrees) {
        while (degrees > 180) degrees -= 360;
        while (degrees < -180) degrees += 360;
        return degrees;
    }
}
