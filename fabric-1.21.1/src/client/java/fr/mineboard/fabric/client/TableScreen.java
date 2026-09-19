package fr.mineboard.fabric.client;

import fr.mineboard.core.*;
import fr.mineboard.fabric.TableNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Transparent in-world UI. Card picking uses the same fan transforms as rendering. */
public final class TableScreen extends Screen {
    private static final int INK = 0xF0192528, PAPER = 0xFFF0E9D8, MUTED = 0xFF9DABA5, GOLD = 0xFFE4BD71;
    private static final int HELP_HEIGHT = 17, HAND_GAP = 12;
    private final BlockPos pos;
    private TableGame.View view;
    private final List<Control> controls = new ArrayList<>();
    private int selected, offset;
    private TableGame.Piece hoverPiece;
    private boolean overview, sway = true, opened, serverClosed, menuExpanded;
    private float zoom = 0.45f;
    private int focusSeat = -1;
    private double mouseX, mouseY, lastMouseX = -1, lastMouseY = -1;
    private long pendingUntil, messageUntil, dealtAt;
    private String message = "";
    private record Control(int x, int y, int w, String label, String action, boolean enabled) {}
    private record Fan(int index, double x, double y, float angle, float width) {}

    public TableScreen(BlockPos pos, TableGame.View view) {
        super(Text.translatable("mineboard.title")); this.pos = pos.toImmutable(); this.view = view;
    }
    public BlockPos pos() { return pos; }
    public TableGame.View view() { return view; }
    public boolean overview() { return overview; }
    public float zoom() { return zoom; }
    public int focusSeat() {
        int count = view.seats().size();
        if (count == 0) return 0;
        if (focusSeat >= 0 && focusSeat < count) return focusSeat;
        return Math.max(0, view.yourSeat());
    }
    @Override public boolean shouldPause() { return false; }
    @Override protected void init() {
        if (!opened) { TableCamera.begin(client.gameRenderer.getCamera()); opened = true; }
        ensureVisible();
    }
    public void update(TableGame.View next, String error) {
        if (next.phase() == TableGame.Phase.PLAYING && (view.phase() != TableGame.Phase.PLAYING
            || "age".equals(next.event()) || "passed".equals(next.event()))) {
            dealtAt = System.nanoTime();
        }
        if (next.phase() == TableGame.Phase.PLAYING && view.phase() != TableGame.Phase.PLAYING) menuExpanded = false;
        view = next; pendingUntil = 0;
        if (focusSeat < 0 || focusSeat >= view.seats().size()) focusSeat = Math.max(0, view.yourSeat());
        selected = MathHelper.clamp(selected, 0, Math.max(0, view.hand().size() - 1));
        ensureVisible();
        if (!error.isEmpty()) { message = tr("error." + error); messageUntil = System.currentTimeMillis() + 3000; }
        else if (Set.of("skipped", "reversed", "plus2", "matched", "missed", "tied", "age", "check", "passed", "war", "salvage").contains(next.event())) {
            message = tr("event." + next.event()); messageUntil = System.currentTimeMillis() + 2200;
        }
    }
    public void closeFromServer() { serverClosed = true; close(); }
    @Override public void close() { client.setScreen(null); }
    @Override public void removed() {
        if (!serverClosed && client.getNetworkHandler() != null) {
            ClientTransport.send(new TableNetworking.Action(pos, "leave", -1, view.revision()));
        }
        TableCamera.exit();
    }
    @Override public void tick() {
        if (client.player == null || !client.player.isAlive() || client.world == null) closeFromServer();
    }
    private String tr(String key, Object... args) { return Text.translatable("mineboard." + key, args).getString(); }
    private int panelWidth() { return width < 480 ? 128 : 158; }
    private int panelX() { return width - panelWidth() - 12; }
    private int visibleCards() { return Math.max(3, Math.min(9, (width - 48) / 30)); }
    private boolean compactMenu() { return view.phase() != TableGame.Phase.LOBBY && !menuExpanded; }
    private float cardWidth() { return height < 300 ? 34 : 44; }
    private int helpTop() { return height - HELP_HEIGHT; }
    private double handBaseY() {
        int count = Math.min(visibleCards(), view.hand().size());
        double edge = Math.max(0, (count - 1) / 2.0);
        double angle = Math.toRadians(edge * 4);
        // Include rotated corners, the fan's arc, outline and shadow in the clearance.
        double bottom = edge * 3 + Math.sin(angle) * (cardWidth() / 2 + 4)
            + Math.cos(angle) * (cardWidth() * .75 + 5);
        return helpTop() - HAND_GAP - bottom;
    }
    private int handCaptionY() { return (int) (handBaseY() - cardWidth() * .75 - 29); }
    private boolean myTurn() {
        return view.phase() == TableGame.Phase.PLAYING && view.yourSeat() >= 0
            && (view.turn() < 0 || view.yourSeat() == view.turn());
    }
    private boolean canAct() {
        return myTurn() && view.hasTurnAction();
    }
    private boolean pending() { return System.currentTimeMillis() < pendingUntil; }
    private boolean playable(int index) { return view.allows("play", index) || view.allows("salvage", index); }
    private boolean host() { return view.yourSeat() >= 0 && view.yourSeat() == view.host(); }
    private void send(String action, int card) {
        if (pending()) return;
        pendingUntil = System.currentTimeMillis() + (view.hand().isEmpty() ? 250 : 1200);
        ClientTransport.send(new TableNetworking.Action(pos, action, card, view.revision()));
    }
    private void ensureVisible() {
        int visible = visibleCards();
        if (selected < offset) offset = selected;
        if (selected >= offset + visible) offset = selected - visible + 1;
        offset = MathHelper.clamp(offset, 0, Math.max(0, view.hand().size() - visible));
    }
    private List<Fan> fan() {
        List<Fan> result = new ArrayList<>();
        int count = Math.min(visibleCards(), view.hand().size() - offset);
        float w = cardWidth();
        double step = Math.min(w * .79, (width - 48.0 - w) / Math.max(1, count - 1));
        double center = width / 2.0;
        for (int i = 0; i < count; i++) {
            double relative = i - (count - 1) / 2.0;
            result.add(new Fan(i + offset, center + relative * step,
                handBaseY() + Math.abs(relative) * 3 - (selected == i + offset ? 12 : 0), (float) relative * 4, w));
        }
        return result;
    }
    private int cardAt(double x, double y) {
        if (y >= helpTop() - HAND_GAP || overPanel(x, y)) return -1;
        List<Fan> cards = fan();
        // Selected card is drawn last and has hit-test priority, including overlap areas.
        for (Fan card : cards) if (card.index == selected && contains(card, x, y)) return card.index;
        for (int i = cards.size() - 1; i >= 0; i--) if (contains(cards.get(i), x, y)) return cards.get(i).index;
        return -1;
    }
    private String seatCounts() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < view.seats().size(); i++) {
            if (i > 0) text.append(" / ");
            text.append(view.seats().get(i).count());
        }
        return text.append(' ').append(tr(switch (view.countKey()) {
            case "pairs" -> "pairs.unit";
            case "tokens" -> "tokens.unit";
            case "tableau" -> "tableau.unit";
            default -> "cards";
        })).toString();
    }
    private TableGame.Piece pieceAt(double mouseX, double mouseY) {
        if (overPanel(mouseX, mouseY) || mouseY >= helpTop() - HAND_GAP) return null;
        TableGame.Piece best = null;
        double bestDist = Double.MAX_VALUE;
        List<TableGame.Piece> pieces = view.pieces();
        for (int i = pieces.size() - 1; i >= 0; i--) {
            TableGame.Piece piece = pieces.get(i);
            if (piece.action().isEmpty()) continue;
            Vec3d hit = tableHit(mouseX, mouseY, piece.y());
            if (hit == null) continue;
            double localX = hit.x - pos.getX(), localZ = hit.z - pos.getZ();
            if (!Layouts.hits(piece, localX, localZ)) continue;
            double dx = localX - piece.x(), dz = localZ - piece.z();
            double dist = dx * dx + dz * dz;
            if (best == null || piece.y() > best.y() || (piece.y() == best.y() && dist < bestDist)) {
                best = piece; bestDist = dist;
            }
        }
        return best;
    }
    private Vec3d tableHit(double mouseX, double mouseY, double pieceY) {
        if (client == null) return null;
        var camera = client.gameRenderer.getCamera();
        Vec3d eye = camera.getPos();
        float yaw = camera.getYaw(), pitch = camera.getPitch();
        double yawRad = Math.toRadians(yaw), pitchRad = Math.toRadians(pitch);
        double lx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double ly = -Math.sin(pitchRad);
        double lz = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3d look = new Vec3d(lx, ly, lz);
        Vec3d right = look.crossProduct(new Vec3d(0, 1, 0));
        if (right.lengthSquared() < 1e-6) right = new Vec3d(1, 0, 0);
        right = right.normalize();
        Vec3d up = right.crossProduct(look).normalize();
        double fov = Math.toRadians(client.options.getFov().getValue() * 0.5);
        double aspect = width / (double) Math.max(1, height);
        double nx = (mouseX / Math.max(1, width) * 2 - 1) * Math.tan(fov) * aspect;
        double ny = (1 - mouseY / Math.max(1, height) * 2) * Math.tan(fov);
        Vec3d dir = look.add(right.multiply(nx)).add(up.multiply(ny)).normalize();
        if (Math.abs(dir.y) < 1e-5) return null;
        double planeY = pos.getY() + pieceY;
        double t = (planeY - eye.y) / dir.y;
        if (t < 0.05 || t > 32) return null;
        return eye.add(dir.multiply(t));
    }
    private boolean contains(Fan card, double x, double y) {
        double angle = Math.toRadians(-card.angle), dx = x - card.x, dy = y - card.y;
        double localX = dx * Math.cos(angle) - dy * Math.sin(angle);
        double localY = dx * Math.sin(angle) + dy * Math.cos(angle);
        return Math.abs(localX) <= card.width / 2 && Math.abs(localY) <= card.width * .75;
    }
    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.mouseX = mouseX; this.mouseY = mouseY;
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            int hover = cardAt(mouseX, mouseY);
            if (hover >= 0) selected = hover;
            hoverPiece = hover >= 0 ? null : pieceAt(mouseX, mouseY);
            lastMouseX = mouseX; lastMouseY = mouseY;
        }
        controls.clear();
        ctx.fill(12, 12, 140, 43, INK);
        ctx.fill(12, 12, 15, 43, GOLD);
        ctx.drawText(textRenderer, "MINEBOARD", 23, 19, PAPER, false);
        ctx.drawText(textRenderer, tr("game." + view.gameId()), 23, 31, MUTED, false);
        String status = switch (view.phase()) {
            case LOBBY -> tr("status.lobby");
            case PLAYING -> canAct() ? tr("status.your_turn")
                : view.turn() < 0 ? tr("status.waiting") : tr("status.turn", view.seats().get(view.turn()).name());
            case FINISHED -> view.winner() < 0 ? tr("status.tie") : tr("status.winner", view.seats().get(view.winner()).name());
        };
        int labelWidth = textRenderer.getWidth(status);
        int statusY = height < 300 ? 51 : 58;
        ctx.fill(Math.max(12, (panelX() - labelWidth) / 2 - 9), statusY - 6,
            Math.min(panelX() - 4, (panelX() + labelWidth) / 2 + 9), statusY + 15, INK);
        ctx.drawCenteredTextWithShadow(textRenderer, status, panelX() / 2, statusY, canAct() ? GOLD : PAPER);
        if (!view.hand().isEmpty()) {
            List<Fan> cards = fan();
            // Distribution may enter from below; keep it out of the help strip too.
            ctx.enableScissor(0, 0, width, helpTop() - HAND_GAP);
            for (Fan card : cards) if (card.index != selected) drawCard(ctx, card, false);
            for (Fan card : cards) if (card.index == selected) drawCard(ctx, card, true);
            ctx.disableScissor();
            String caption = tr("hand", view.hand().size());
            if (view.hand().size() > visibleCards()) caption += "  " + (offset + 1) + "–" + Math.min(view.hand().size(), offset + visibleCards());
            ctx.drawCenteredTextWithShadow(textRenderer, caption, width / 2, handCaptionY(), PAPER);
        } else if (view.phase() == TableGame.Phase.LOBBY) {
            ctx.drawCenteredTextWithShadow(textRenderer, tr(view.hintKey()), panelX() / 2, height - 66, PAPER);
        }
        int infoIndex = cardAt(mouseX, mouseY);
        if (infoIndex >= 0) {
            List<String> info = CardInfo.describe(view.hand().get(infoIndex), key -> tr(key));
            if (!info.isEmpty()) {
                int infoWidth = Math.max(80, panelX() - 24);
                List<String> wrapped = new ArrayList<>();
                for (String detail : info) {
                    String line = detail;
                    while (!line.isEmpty()) {
                        String part = textRenderer.trimToWidth(line, infoWidth - 12);
                        if (part.isEmpty()) break;
                        if (part.length() < line.length() && part.lastIndexOf(' ') > 0) part = part.substring(0, part.lastIndexOf(' '));
                        wrapped.add(part);
                        line = line.substring(part.length()).stripLeading();
                    }
                }
                info = wrapped;
                int infoY = 79;
                ctx.fill(12, infoY - 5, 12 + infoWidth, infoY + info.size() * 11 + 3, INK);
                for (String line : info) {
                    ctx.drawText(textRenderer, textRenderer.trimToWidth(line, infoWidth - 12), 18, infoY, PAPER, false);
                    infoY += 11;
                }
            }
        }
        if (System.currentTimeMillis() < messageUntil) {
            int messageY = view.hand().isEmpty() ? height - 146 : handCaptionY() - 22;
            ctx.fill(12, messageY - 6, width - 12, messageY + 16, INK);
            ctx.drawCenteredTextWithShadow(textRenderer, textRenderer.trimToWidth(message, width - 40), width / 2, messageY, 0xFFFFBE9E);
        }
        // Above the 3D items: an expanded menu owns both rendering and input in its bounds.
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 600);
        drawPanel(ctx);
        ctx.fill(0, helpTop(), width, height, 0xD0101C1F);
        ctx.drawText(textRenderer, tr(width < 550 ? view.controlsKey() + ".short" : view.controlsKey()), 12, height - 12, MUTED, false);
        ctx.getMatrices().pop();
    }
    private void drawPanel(DrawContext ctx) {
        int x = panelX(), w = panelWidth();
        if (compactMenu()) {
            ctx.fill(x, 12, x + w, compactBottom(), INK);
            ctx.fill(x, 12, x + w, 14, GOLD);
            control(ctx, x + 10, 22, w - 20, tr("menu.open"), "menu", true);
            ctx.drawCenteredTextWithShadow(textRenderer, seatCounts(), x + w / 2, 53, MUTED);
            if (view.phase() == TableGame.Phase.FINISHED) {
                control(ctx, x + 10, 72, w - 20, tr("rematch"), "rematch", host());
            } else {
                turnControls(ctx, x + 10, 72, w - 20, true);
            }
            return;
        }
        ctx.fill(x, 12, x + w, height - 25, INK);
        ctx.fill(x, 12, x + w, 14, GOLD);
        if (view.phase() == TableGame.Phase.LOBBY) ctx.drawText(textRenderer, tr("lobby"), x + 12, 25, PAPER, false);
        else control(ctx, x + 10, 20, w - 20, tr("menu.close"), "menu", true);
        int actionRows = view.phase() == TableGame.Phase.LOBBY ? (host() ? 3 : 1)
            : Math.max(1, (int) view.buttons().stream().filter(b -> !b.equals("rematch")).count());
        int rowHeight = Math.min(39, Math.max(10, (height - 51 - 42 - actionRows * 27) / Math.max(1, view.seats().size())));
        boolean compactRows = rowHeight < 30;
        for (int i = 0; i < view.seats().size(); i++) {
            TableGame.Seat seat = view.seats().get(i);
            int y = 36 + i * rowHeight;
            ctx.fill(x + 10, y, x + w - 10, y + (rowHeight - 1), 0xFF233438);
            ctx.fill(x + 10, y, x + 12, y + (rowHeight - 1), view.phase() == TableGame.Phase.PLAYING && view.turn() == i ? GOLD : 0xFF48605D);
            String name = seat.id() == null ? tr("empty_seat") : seat.name();
            if (view.yourSeat() == i) name += " •";
            if (view.seats().size() > 2 && focusSeat() == i) name += " ◂";
            ctx.drawText(textRenderer, textRenderer.trimToWidth(name, w - 32), x + 19, y + (compactRows ? 1 : 6), PAPER, false);
            if (!compactRows) {
            String detail = seat.id() == null ? tr("available") : view.phase() == TableGame.Phase.LOBBY
                ? tr(seat.ready() ? "ready" : "waiting") : tr(view.countKey(), seat.count());
            ctx.drawText(textRenderer, detail, x + 19, y + 19, seat.ready() ? 0xFF85C6A3 : MUTED, false);
            }
        }
        int y = 36 + view.seats().size() * rowHeight + 6;
        if (view.phase() == TableGame.Phase.LOBBY) {
            if (view.yourSeat() < 0) control(ctx, x + 10, y, w - 20, tr("join"), "join",
                view.seats().stream().anyMatch(s -> s.id() == null));
            else control(ctx, x + 10, y, w - 20, tr(view.seats().get(view.yourSeat()).ready() ? "unready" : "be_ready"), "ready", true);
            y += 27;
            if (host()) {
                control(ctx, x + 10, y, w - 20, tr("game.cycle", tr("game." + view.gameId())), "game", true);
                y += 27;
                control(ctx, x + 10, y, w - 20, tr(view.startKey()), "start", canStart());
            }
        } else if (view.phase() == TableGame.Phase.PLAYING) {
            turnControls(ctx, x + 10, y, w - 20, false);
            if (view.seats().size() > 2 && y + actionRows * 27 <= height - 126) {
                y = height - 126;
                control(ctx, x + 10, y, w - 20, tr("look.neighbors"), "look_next", true);
            }
        } else if (host()) control(ctx, x + 10, y, w - 20, tr("rematch"), "rematch", true);
        // The bottom controls remain reachable at Minecraft's minimum GUI height (240).
        int bottom = height - 101;
        if (bottom < 187) bottom = 187;
        if (height >= 300 && 36 + view.seats().size() * rowHeight + 6 + actionRows * 27 <= bottom) {
            control(ctx, x + 10, bottom, w - 20, tr(overview ? "view.seat" : "view.table"), "view", true);
            control(ctx, x + 10, bottom + 25, w - 20, tr(sway ? "sway.on" : "sway.off"), "sway", true);
        }
        control(ctx, x + 10, height - 51, w - 20, tr("leave"), "leave", true);
    }
    private void turnControls(DrawContext ctx, int x, int y, int w, boolean single) {
        List<String> buttons = new ArrayList<>();
        for (String button : view.buttons()) if (!"rematch".equals(button)) buttons.add(button);
        for (String button : buttons) {
            control(ctx, x, y, w, tr(button), button, buttonReady(button));
            y += 27;
        }
    }
    private boolean buttonReady(String button) {
        if (Set.of("play", "discard", "wonder", "salvage").contains(button)) return view.allows(button, selected);
        return true;
    }
    private boolean canStart() {
        long occupied = view.seats().stream().filter(seat -> seat.id() != null).count();
        return occupied >= view.minSeats() && view.seats().stream().filter(seat -> seat.id() != null).allMatch(TableGame.Seat::ready);
    }
    private void lookNext() {
        int count = Math.max(1, view.seats().size());
        focusSeat = (focusSeat() + 1) % count;
        overview = false;
    }
    private void control(DrawContext ctx, int x, int y, int w, String label, String action, boolean enabled) {
        boolean usable = enabled && (!pending() || Set.of("leave", "view", "sway", "menu", "look_next").contains(action));
        controls.add(new Control(x, y, w, label, action, usable));
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 21;
        ctx.fill(x, y, x + w, y + 21, !usable ? 0xFF283235 : hover ? 0xFF4B6461 : 0xFF354B48);
        ctx.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + 6, usable ? PAPER : 0xFF70807C);
    }
    private void drawCard(DrawContext ctx, Fan card, boolean selected) {
        double age = (System.nanoTime() - dealtAt) / 1e9;
        double deal = MathHelper.clamp((age - (card.index - offset) * .065) / .4, 0, 1);
        double enter = dealtAt == 0 ? 0 : (1 - deal) * 95;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(card.x, card.y + enter, selected ? 80 : 10 + card.index - offset);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(card.angle));
        int half = Math.round(card.width / 2);
        int tall = Math.round(card.width * .75f);
        ctx.fill(-half + 3, -tall + 4, half + 3, tall + 4, 0x65000000);
        if (selected) ctx.fill(-half - 2, -tall - 2, half + 2, tall + 2, playable(card.index) ? GOLD : 0xFF91AAA5);
        float scale = card.width / 8f;
        // Keep GUI depth independent of card size, so the menu can reliably overlay the hand.
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.drawItem(TableRenderer.card(view.hand().get(card.index)), -8, -8);
        ctx.getMatrices().pop();
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button != 0) return super.mouseClicked(x, y, button);
        for (Control c : controls) if (x >= c.x && x < c.x + c.w && y >= c.y && y < c.y + 21) {
            if (c.enabled) switch (c.action) {
                case "leave" -> close();
                case "view" -> overview = !overview;
                case "sway" -> sway = !sway;
                case "menu" -> menuExpanded = !menuExpanded;
                case "look_next" -> lookNext();
                default -> send(c.action, Set.of("play", "discard", "wonder", "salvage").contains(c.action) ? selected : -1);
            }
            return true;
        }
        int index = cardAt(x, y);
        if (index >= 0) {
            selected = index;
            if (view.buttons().contains("salvage")) {
                if (playable(index)) send("salvage", index);
                return true;
            }
            boolean auto = playable(index) && !view.buttons().contains("discard") && !view.buttons().contains("wonder");
            if (auto) send("play", index);
            else if (!playable(index) && view.buttons().stream().noneMatch(name -> name.equals("discard") || name.equals("wonder"))) {
                message = tr(myTurn() ? "error.cannot_play" : "error.not_your_turn"); messageUntil = System.currentTimeMillis() + 1800;
            }
            return true;
        }
        TableGame.Piece piece = pieceAt(x, y);
        if (piece != null) {
            hoverPiece = piece;
            if ("wonder".equals(piece.kind()) && view.seats().size() > 2) {
                focusSeat = piece.id();
                overview = false;
                return true;
            }
            int target = "draw".equals(piece.action()) ? -1 : piece.id();
            if (view.allows(piece.action(), target)) send(piece.action(), target);
            else { message = tr(myTurn() ? ("flip".equals(piece.action()) ? "error.cannot_flip" : "error.cannot_play") : "error.not_your_turn"); messageUntil = System.currentTimeMillis() + 1800; }
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (overHand(x, y)) {
            selected = MathHelper.clamp(selected + (vertical < 0 ? 1 : -1), 0, view.hand().size() - 1);
            ensureVisible();
        } else nudgeZoom(vertical);
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            selected = MathHelper.clamp(selected + (key == GLFW.GLFW_KEY_RIGHT ? 1 : -1), 0, Math.max(0, view.hand().size() - 1));
            ensureVisible(); return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER) {
            if (playable(selected)) { send(view.allows("salvage", selected) ? "salvage" : "play", selected); return true; }
            if (hoverPiece != null && view.allows(hoverPiece.action(), "draw".equals(hoverPiece.action()) ? -1 : hoverPiece.id())) {
                send(hoverPiece.action(), "draw".equals(hoverPiece.action()) ? -1 : hoverPiece.id()); return true;
            }
        }
        if (key == GLFW.GLFW_KEY_P && view.allows("draw")) { send("draw", -1); return true; }
        if (key == GLFW.GLFW_KEY_V) { overview = !overview; return true; }
        if (key == GLFW.GLFW_KEY_M) { sway = !sway; return true; }
        if (key == GLFW.GLFW_KEY_EQUAL || key == GLFW.GLFW_KEY_KP_ADD) { nudgeZoom(1); return true; }
        if (key == GLFW.GLFW_KEY_MINUS || key == GLFW.GLFW_KEY_KP_SUBTRACT) { nudgeZoom(-1); return true; }
        if (key == GLFW.GLFW_KEY_LEFT_BRACKET || key == GLFW.GLFW_KEY_RIGHT_BRACKET) {
            if (view.seats().size() > 2) { lookNext(); return true; }
        }
        if (key == GLFW.GLFW_KEY_TAB && view.phase() != TableGame.Phase.LOBBY) { menuExpanded = !menuExpanded; return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    public double swayX() { return allowSway() ? edge(mouseX / Math.max(1, width) * 2 - 1) : 0; }
    public double swayY() { return allowSway() ? edge(mouseY / Math.max(1, height) * 2 - 1) : 0; }
    private int compactBottom() { return 76 + 27 * Math.max(1, view.buttons().size()); }
    private boolean overPanel(double x, double y) {
        return x >= panelX() && x <= width - 12 && y >= 12 && y < (compactMenu() ? compactBottom() : height - 25);
    }
    private boolean overHand(double x, double y) {
        return !view.hand().isEmpty() && !overPanel(x, y) && y >= handCaptionY() - 8 && y < helpTop() - HAND_GAP;
    }
    private void nudgeZoom(double steps) {
        zoom = MathHelper.clamp(zoom + (float) steps * 0.08f, 0, 1);
    }
    private boolean allowSway() {
        return sway && !overPanel(mouseX, mouseY) && mouseY < (view.hand().isEmpty() ? height - 125 : handCaptionY() - 8);
    }
    private double edge(double normalized) {
        double amount = MathHelper.clamp((Math.abs(normalized) - .55) / .45, 0, 1);
        return Math.copySign(amount * amount, normalized);
    }
}
