package fr.mineboard.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Orthodox chess. Pawns promote through explicit piece buttons. */
final class ChessGame implements Game {
    private final int[] board = new int[64];
    private final List<Long> history = new ArrayList<>();
    private int selected = -1, ep = -1, promote = -1, halfmove;

    @Override public String id() { return Games.CHESS; }
    @Override public int seats() { return 2; }
    @Override public String countKey() { return "tokens"; }
    @Override public String hintKey() { return "lobby.hint.chess"; }
    @Override public String controlsKey() { return "controls.chess"; }
    @Override public String startKey() { return "start.chess"; }
    @Override public void clear() {
        java.util.Arrays.fill(board, 0);
        selected = ep = promote = -1;
        halfmove = 0;
        history.clear();
        whiteCastleK = whiteCastleQ = blackCastleK = blackCastleQ = true;
    }
    private boolean whiteCastleK = true, whiteCastleQ = true, blackCastleK = true, blackCastleQ = true;

    @Override public void start(TableSession session, Random random) {
        clear();
        int[] back = { 4, 2, 3, 5, 6, 3, 2, 4 };
        for (int col = 0; col < 8; col++) {
            board[sq(0, col)] = back[col];
            board[sq(1, col)] = 1;
            board[sq(6, col)] = -1;
            board[sq(7, col)] = -back[col];
        }
        history.add(key(1));
    }
    @Override public String apply(TableSession session, UUID player, String type, int target, long revision) {
        int seat = session.seatOf(player);
        int side = seat == 0 ? 1 : -1;
        if (List.of("promote_queen", "promote_rook", "promote_bishop", "promote_knight").contains(type)) {
            String error = session.validateTurn(player, revision);
            if (!error.isEmpty()) return error;
            if (promote < 0) return "cannot_play";
            int piece = switch (type) {
                case "promote_rook" -> 4;
                case "promote_bishop" -> 3;
                case "promote_knight" -> 2;
                default -> 5;
            };
            board[promote] = piece * side;
            promote = -1;
            halfmove = 0;
            return finishMove(session, seat, side);
        }
        if (!type.equals("move")) return "invalid_action";
        String error = session.validateTurn(player, revision);
        if (!error.isEmpty()) return error;
        if (promote >= 0) return "cannot_play";
        if (target < 0 || target > 63) return "invalid_card";
        if (target == selected) {
            selected = -1;
            session.changed("select", seat);
            return "";
        }
        if (selected < 0 || side(board[target]) == side) {
            if (side(board[target]) != side) return "cannot_play";
            selected = target;
            session.changed("select", seat);
            return "";
        }
        if (!legal(selected, target, side, true)) return "cannot_play";
        boolean pawn = Math.abs(board[selected]) == 1;
        boolean capture = board[target] != 0 || (pawn && target == ep);
        play(selected, target, side);
        selected = -1;
        halfmove = pawn || capture ? 0 : halfmove + 1;
        int row = target / 8;
        if (Math.abs(board[target]) == 1 && (row == 7 || row == 0)) {
            promote = target;
            session.changed("promote", seat);
            return "";
        }
        return finishMove(session, seat, side);
    }
    @Override public void populate(TableSession session, ViewBuild view, UUID recipient) {
        int[] counts = { 0, 0 };
        for (int piece : board) {
            if (piece > 0) counts[0]++;
            if (piece < 0) counts[1]++;
        }
        view.counts = counts;
        view.topCard = -1;
        view.deckCount = 0;
        view.hand = List.of();
        view.playable = List.of();
        int seat = recipient == null ? -1 : session.seatOf(recipient);
        boolean can = session.phase() == TableGame.Phase.PLAYING && seat == session.turn();
        List<TableGame.Piece> pieces = new ArrayList<>();
        List<TableGame.Move> moves = new ArrayList<>();
        List<String> buttons = new ArrayList<>();
        for (int square = 0; square < 64; square++) {
            int row = square / 8, col = square % 8;
            double x = Layouts.CHECKER_ORIGIN + col * Layouts.CHECKER_CELL;
            double z = Layouts.CHECKER_ORIGIN + (7 - row) * Layouts.CHECKER_CELL;
            boolean dark = ((row + col) & 1) == 0;
            String action = can && promote < 0 ? "move" : "";
            pieces.add(new TableGame.Piece(square, dark ? 1 : 0, x, 0.132, z, 0, (float) Layouts.CHECKER_CELL, "cell", action));
            int value = board[square];
            if (value != 0) {
                boolean lifted = selected == square;
                pieces.add(new TableGame.Piece(square, value, x, lifted ? 0.175 : 0.141, z, value > 0 ? 0 : 180,
                    lifted ? Layouts.CHECKER_TOKEN * 1.12f : Layouts.CHECKER_TOKEN, "chess", can ? "move" : ""));
            }
            if (can && promote < 0) moves.add(new TableGame.Move("move", square));
        }
        if (can && promote >= 0) {
            buttons.add("promote_queen");
            buttons.add("promote_rook");
            buttons.add("promote_bishop");
            buttons.add("promote_knight");
            moves.add(new TableGame.Move("promote_queen", -1));
            moves.add(new TableGame.Move("promote_rook", -1));
            moves.add(new TableGame.Move("promote_bishop", -1));
            moves.add(new TableGame.Move("promote_knight", -1));
        }
        if (session.phase() == TableGame.Phase.FINISHED) buttons.add("rematch");
        view.pieces = pieces;
        view.moves = moves;
        view.buttons = buttons;
    }

    private String finishMove(TableSession session, int seat, int side) {
        int enemy = -side;
        history.add(key(enemy));
        if (!anyLegal(enemy)) {
            boolean mate = inCheck(enemy);
            session.finish(mate ? seat : -1, mate ? "played" : "tied", seat);
            return "";
        }
        if (halfmove >= 100 || repeated() || deadPosition()) {
            session.finish(-1, "tied", seat);
            return "";
        }
        session.passTurn();
        session.changed(inCheck(enemy) ? "check" : "moved", seat);
        return "";
    }
    private void play(int from, int to, int side) {
        int piece = board[from];
        int nextEp = -1;
        if (Math.abs(piece) == 1 && to == ep && col(from) != col(to) && board[to] == 0) {
            board[to - side * 8] = 0;
        }
        if (Math.abs(piece) == 6 && Math.abs(col(to) - col(from)) == 2) {
            if (col(to) == 6) { board[sq(row(from), 5)] = board[sq(row(from), 7)]; board[sq(row(from), 7)] = 0; }
            else { board[sq(row(from), 3)] = board[sq(row(from), 0)]; board[sq(row(from), 0)] = 0; }
        }
        if (Math.abs(piece) == 1 && Math.abs(row(to) - row(from)) == 2) nextEp = from + side * 8;
        if (piece == 6) whiteCastleK = whiteCastleQ = false;
        if (piece == -6) blackCastleK = blackCastleQ = false;
        if (from == 0 || to == 0) whiteCastleQ = false;
        if (from == 7 || to == 7) whiteCastleK = false;
        if (from == 56 || to == 56) blackCastleQ = false;
        if (from == 63 || to == 63) blackCastleK = false;
        board[to] = piece;
        board[from] = 0;
        ep = nextEp;
    }
    private boolean anyLegal(int side) {
        for (int from = 0; from < 64; from++) if (side(board[from]) == side) {
            for (int to = 0; to < 64; to++) if (legal(from, to, side, true)) return true;
        }
        return false;
    }
    private boolean legal(int from, int to, int side, boolean check) {
        int piece = board[from];
        if (side(piece) != side || from == to || side(board[to]) == side || Math.abs(board[to]) == 6) return false;
        if (!pseudo(from, to, side)) return false;
        if (!check) return true;
        int[] copy = board.clone();
        int savedEp = ep;
        boolean wk = whiteCastleK, wq = whiteCastleQ, bk = blackCastleK, bq = blackCastleQ;
        play(from, to, side);
        boolean ok = !inCheck(side);
        System.arraycopy(copy, 0, board, 0, 64);
        ep = savedEp;
        whiteCastleK = wk; whiteCastleQ = wq; blackCastleK = bk; blackCastleQ = bq;
        return ok;
    }
    private boolean pseudo(int from, int to, int side) {
        int piece = Math.abs(board[from]);
        int dr = row(to) - row(from), dc = col(to) - col(from);
        return switch (piece) {
            case 1 -> pawn(from, to, side, dr, dc);
            case 2 -> Math.abs(dr) * Math.abs(dc) == 2 && Math.abs(dr) + Math.abs(dc) == 3;
            case 3 -> Math.abs(dr) == Math.abs(dc) && clear(from, to);
            case 4 -> (dr == 0 || dc == 0) && clear(from, to);
            case 5 -> ((dr == 0 || dc == 0) || Math.abs(dr) == Math.abs(dc)) && clear(from, to);
            case 6 -> king(from, to, side, dr, dc);
            default -> false;
        };
    }
    private boolean pawn(int from, int to, int side, int dr, int dc) {
        int step = side;
        if (dc == 0 && board[to] == 0) {
            if (dr == step) return true;
            int start = side > 0 ? 1 : 6;
            return dr == 2 * step && row(from) == start && board[from + 8 * step] == 0;
        }
        if (Math.abs(dc) == 1 && dr == step) return side(board[to]) == -side || to == ep;
        return false;
    }
    private boolean king(int from, int to, int side, int dr, int dc) {
        if (Math.max(Math.abs(dr), Math.abs(dc)) == 1) return true;
        if (dr != 0 || Math.abs(dc) != 2 || inCheck(side)) return false;
        if (from != (side > 0 ? 4 : 60)) return false;
        if (board[sq(row(from), dc > 0 ? 7 : 0)] != 4 * side) return false;
        if (side > 0 && dc == 2 && whiteCastleK && board[5] == 0 && board[6] == 0 && !attacked(5, -1) && !attacked(6, -1)) return true;
        if (side > 0 && dc == -2 && whiteCastleQ && board[1] == 0 && board[2] == 0 && board[3] == 0 && !attacked(2, -1) && !attacked(3, -1)) return true;
        if (side < 0 && dc == 2 && blackCastleK && board[61] == 0 && board[62] == 0 && !attacked(61, 1) && !attacked(62, 1)) return true;
        return side < 0 && dc == -2 && blackCastleQ && board[57] == 0 && board[58] == 0 && board[59] == 0 && !attacked(58, 1) && !attacked(59, 1);
    }
    private boolean clear(int from, int to) {
        int dr = Integer.signum(row(to) - row(from)), dc = Integer.signum(col(to) - col(from));
        int r = row(from) + dr, c = col(from) + dc;
        while (r != row(to) || c != col(to)) {
            if (board[sq(r, c)] != 0) return false;
            r += dr; c += dc;
        }
        return true;
    }
    private boolean inCheck(int side) {
        int king = -1;
        for (int i = 0; i < 64; i++) if (board[i] == 6 * side) king = i;
        return king >= 0 && attacked(king, -side);
    }
    private boolean attacked(int square, int by) {
        // Attacks do not use pawn movement or castling: empty transit squares
        // are still attacked by pawns, and kings only attack adjacent squares.
        for (int from = 0; from < 64; from++) if (side(board[from]) == by && from != square) {
            int dr = row(square) - row(from), dc = col(square) - col(from);
            boolean attacks = switch (Math.abs(board[from])) {
                case 1 -> dr == by && Math.abs(dc) == 1;
                case 6 -> Math.max(Math.abs(dr), Math.abs(dc)) == 1;
                default -> pseudo(from, square, by);
            };
            if (attacks) return true;
        }
        return false;
    }
    private boolean repeated() {
        long now = history.get(history.size() - 1);
        int count = 0;
        for (long key : history) if (key == now && ++count >= 3) return true;
        return false;
    }
    private boolean deadPosition() {
        int minors = 0, knights = 0, bishopColor = -1;
        boolean sameBishopColor = true;
        for (int square = 0; square < 64; square++) {
            int piece = board[square];
            int abs = Math.abs(piece);
            if (abs == 0 || abs == 6) continue;
            if (abs == 1 || abs == 4 || abs == 5) return false;
            minors++;
            if (abs == 2) knights++;
            else {
                int color = (row(square) + col(square)) & 1;
                if (bishopColor >= 0 && bishopColor != color) sameBishopColor = false;
                bishopColor = color;
            }
        }
        return minors <= 1 || (knights == 0 && sameBishopColor);
    }
    private long key(int side) {
        long hash = side + 2;
        for (int piece : board) hash = hash * 31 + piece + 7;
        hash = hash * 31 + (whiteCastleK ? 1 : 0) + (whiteCastleQ ? 2 : 0) + (blackCastleK ? 4 : 0) + (blackCastleQ ? 8 : 0);
        int relevantEp = -1;
        if (ep >= 0) {
            for (int from = 0; from < 64; from++) {
                if (board[from] == side && col(from) != col(ep) && legal(from, ep, side, true)) {
                    relevantEp = ep;
                    break;
                }
            }
        }
        return hash * 31 + relevantEp + 1;
    }
    private static int side(int piece) { return Integer.signum(piece); }
    private static int row(int square) { return square / 8; }
    private static int col(int square) { return square % 8; }
    private static int sq(int row, int col) { return row * 8 + col; }
}
