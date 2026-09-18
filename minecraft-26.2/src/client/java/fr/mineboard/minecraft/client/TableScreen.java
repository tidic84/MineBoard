package fr.mineboard.minecraft.client;

import fr.mineboard.core.*;
import fr.mineboard.minecraft.TableNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
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
    private double mouseX, mouseY, lastMouseX = -1, lastMouseY = -1;
    private long pendingUntil, messageUntil, dealtAt;
    private String message = "";
    private record Control(int x, int y, int w, String label, String action, boolean enabled) {}
    private record Fan(int index, double x, double y, float angle, float width) {}

    public TableScreen(BlockPos pos, TableGame.View view) {
        super(Component.translatable("mineboard.title")); this.pos = pos.immutable(); this.view = view;
    }
    public BlockPos pos() { return pos; }
    public TableGame.View view() { return view; }
    public boolean overview() { return overview; }
    public float zoom() { return zoom; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    @Override public void extractBackground(GuiGraphicsExtractor ctx, int x, int y, float delta) {}
    @Override protected void init() {
        if (!opened) { TableCamera.begin(minecraft.gameRenderer.mainCamera()); opened = true; }
        ensureVisible();
    }
    public void update(TableGame.View next, String error) {
        if (next.phase() == TableGame.Phase.PLAYING && view.phase() != TableGame.Phase.PLAYING) {
            dealtAt = System.nanoTime();
            menuExpanded = false;
        }
        view = next; pendingUntil = 0;
        selected = Mth.clamp(selected, 0, Math.max(0, view.hand().size() - 1));
        ensureVisible();
        if (!error.isEmpty()) { message = tr("error." + error); messageUntil = System.currentTimeMillis() + 3000; }
        else if (Set.of("skipped", "reversed", "plus2", "matched", "missed", "tied").contains(next.event())) {
            message = tr("event." + next.event()); messageUntil = System.currentTimeMillis() + 2200;
        }
    }
    public void closeFromServer() { serverClosed = true; onClose(); }
    @Override public void onClose() { minecraft.gui.setScreen(null); }
    @Override public void removed() {
        if (!serverClosed && minecraft.getConnection() != null) {
            ClientTransport.send(new TableNetworking.Action(pos, "leave", -1, view.revision()));
        }
        TableCamera.exit();
    }
    @Override public void tick() {
        if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.level == null) closeFromServer();
    }
    private String tr(String key, Object... args) { return Component.translatable("mineboard." + key, args).getString(); }
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
        return myTurn() && (view.allows("play") || view.allows("draw") || view.allows("flip") || view.allows("move"));
    }
    private boolean pending() { return System.currentTimeMillis() < pendingUntil; }
    private boolean playable(int index) { return view.allows("play", index); }
    private boolean host() { return view.yourSeat() >= 0 && view.yourSeat() == view.host(); }
    private void send(String action, int card) {
        if (pending()) return;
        pendingUntil = System.currentTimeMillis() + (action.equals("flip") || action.equals("move") ? 250 : 1200);
        ClientTransport.send(new TableNetworking.Action(pos, action, card, view.revision()));
    }
    private void ensureVisible() {
        int visible = visibleCards();
        if (selected < offset) offset = selected;
        if (selected >= offset + visible) offset = selected - visible + 1;
        offset = Mth.clamp(offset, 0, Math.max(0, view.hand().size() - visible));
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
        Vec3 hit = tableHit(mouseX, mouseY);
        if (hit == null) return null;
        double localX = hit.x - pos.getX(), localZ = hit.z - pos.getZ();
        TableGame.Piece best = null;
        double bestDist = Double.MAX_VALUE;
        List<TableGame.Piece> pieces = view.pieces();
        for (int i = pieces.size() - 1; i >= 0; i--) {
            TableGame.Piece piece = pieces.get(i);
            if (piece.action().isEmpty() || !Layouts.hits(piece, localX, localZ)) continue;
            double dx = localX - piece.x(), dz = localZ - piece.z();
            double dist = dx * dx + dz * dz;
            if (dist < bestDist) { best = piece; bestDist = dist; }
        }
        return best;
    }
    private Vec3 tableHit(double mouseX, double mouseY) {
        if (minecraft == null) return null;
        var camera = minecraft.gameRenderer.mainCamera();
        Vec3 eye = camera.position();
        float yaw = camera.yRot(), pitch = camera.xRot();
        double yawRad = Math.toRadians(yaw), pitchRad = Math.toRadians(pitch);
        double lx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double ly = -Math.sin(pitchRad);
        double lz = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3 look = new Vec3(lx, ly, lz);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        double fov = Math.toRadians(70 * 0.5);
        double aspect = width / (double) Math.max(1, height);
        double nx = (mouseX / Math.max(1, width) * 2 - 1) * Math.tan(fov) * aspect;
        double ny = (1 - mouseY / Math.max(1, height) * 2) * Math.tan(fov);
        Vec3 dir = look.add(right.scale(nx)).add(up.scale(ny)).normalize();
        if (Math.abs(dir.y) < 1e-5) return null;
        double planeY = pos.getY() + 0.16;
        double t = (planeY - eye.y) / dir.y;
        if (t < 0.05 || t > 32) return null;
        return eye.add(dir.scale(t));
    }
    private boolean contains(Fan card, double x, double y) {
        double angle = Math.toRadians(-card.angle), dx = x - card.x, dy = y - card.y;
        double localX = dx * Math.cos(angle) - dy * Math.sin(angle);
        double localY = dx * Math.sin(angle) + dy * Math.cos(angle);
        return Math.abs(localX) <= card.width / 2 && Math.abs(localY) <= card.width * .75;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
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
        ctx.text(font, "MINEBOARD", 23, 19, PAPER, false);
        ctx.text(font, tr("game." + view.gameId()), 23, 31, MUTED, false);
        String status = switch (view.phase()) {
            case LOBBY -> tr("status.lobby");
            case PLAYING -> canAct() ? tr("status.your_turn")
                : view.turn() < 0 ? tr("status.waiting") : tr("status.turn", view.seats().get(view.turn()).name());
            case FINISHED -> view.winner() < 0 ? tr("status.tie") : tr("status.winner", view.seats().get(view.winner()).name());
        };
        int labelWidth = font.width(status);
        int statusY = height < 300 ? 51 : 58;
        ctx.fill(Math.max(12, (panelX() - labelWidth) / 2 - 9), statusY - 6,
            Math.min(panelX() - 4, (panelX() + labelWidth) / 2 + 9), statusY + 15, INK);
        ctx.centeredText(font, status, panelX() / 2, statusY, canAct() ? GOLD : PAPER);
        if (!view.hand().isEmpty()) {
            List<Fan> cards = fan();
            // Distribution may enter from below; keep it out of the help strip too.
            ctx.enableScissor(0, 0, width, helpTop() - HAND_GAP);
            for (Fan card : cards) if (card.index != selected) drawCard(ctx, card, false);
            for (Fan card : cards) if (card.index == selected) drawCard(ctx, card, true);
            ctx.disableScissor();
            String caption = tr("hand", view.hand().size());
            if (view.hand().size() > visibleCards()) caption += "  " + (offset + 1) + "–" + Math.min(view.hand().size(), offset + visibleCards());
            ctx.centeredText(font, caption, width / 2, handCaptionY(), PAPER);
        } else if (view.phase() == TableGame.Phase.LOBBY) {
            ctx.centeredText(font, tr(view.hintKey()), panelX() / 2, height - 66, PAPER);
        }
        if (System.currentTimeMillis() < messageUntil) {
            int messageY = view.hand().isEmpty() ? height - 146 : handCaptionY() - 22;
            ctx.fill(12, messageY - 6, width - 12, messageY + 16, INK);
            ctx.centeredText(font, font.plainSubstrByWidth(message, width - 40), width / 2, messageY, 0xFFFFBE9E);
        }
        // Above the 3D items: an expanded menu owns both rendering and input in its bounds.
        ctx.pose().pushMatrix();
        ctx.nextStratum();
        drawPanel(ctx);
        ctx.fill(0, helpTop(), width, height, 0xD0101C1F);
        ctx.text(font, tr(width < 550 ? view.controlsKey() + ".short" : view.controlsKey()), 12, height - 12, MUTED, false);
        ctx.pose().popMatrix();
    }
    private void drawPanel(GuiGraphicsExtractor ctx) {
        int x = panelX(), w = panelWidth();
        if (compactMenu()) {
            ctx.fill(x, 12, x + w, 103, INK);
            ctx.fill(x, 12, x + w, 14, GOLD);
            control(ctx, x + 10, 22, w - 20, tr("menu.open"), "menu", true);
            ctx.centeredText(font, seatCounts(), x + w / 2, 53, MUTED);
            if (view.phase() == TableGame.Phase.FINISHED) {
                control(ctx, x + 10, 72, w - 20, tr("rematch"), "rematch", host());
            } else if (view.allows("draw")) {
                control(ctx, x + 10, 72, w - 20, tr("draw"), "draw", true);
            }
            return;
        }
        ctx.fill(x, 12, x + w, height - 25, INK);
        ctx.fill(x, 12, x + w, 14, GOLD);
        if (view.phase() == TableGame.Phase.LOBBY) ctx.text(font, tr("lobby"), x + 12, 25, PAPER, false);
        else control(ctx, x + 10, 20, w - 20, tr("menu.close"), "menu", true);
        for (int i = 0; i < view.seats().size(); i++) {
            TableGame.Seat seat = view.seats().get(i);
            int y = 47 + i * 39;
            ctx.fill(x + 10, y, x + w - 10, y + 33, 0xFF233438);
            ctx.fill(x + 10, y, x + 12, y + 33, view.phase() == TableGame.Phase.PLAYING && view.turn() == i ? GOLD : 0xFF48605D);
            String name = seat.id() == null ? tr("empty_seat") : seat.name();
            if (view.yourSeat() == i) name += " •";
            ctx.text(font, font.plainSubstrByWidth(name, w - 32), x + 19, y + 6, PAPER, false);
            String detail = seat.id() == null ? tr("available") : view.phase() == TableGame.Phase.LOBBY
                ? tr(seat.ready() ? "ready" : "waiting") : tr(view.countKey(), seat.count());
            ctx.text(font, detail, x + 19, y + 19, seat.ready() ? 0xFF85C6A3 : MUTED, false);
        }
        int y = 47 + view.seats().size() * 39 + 6;
        if (view.phase() == TableGame.Phase.LOBBY) {
            if (view.yourSeat() < 0) control(ctx, x + 10, y, w - 20, tr("join"), "join",
                view.seats().stream().anyMatch(s -> s.id() == null));
            else control(ctx, x + 10, y, w - 20, tr(view.seats().get(view.yourSeat()).ready() ? "unready" : "be_ready"), "ready", true);
            y += 27;
            if (host()) {
                control(ctx, x + 10, y, w - 20, tr("game.cycle", tr("game." + view.gameId())), "game", true);
                y += 27;
                control(ctx, x + 10, y, w - 20, tr(view.startKey()), "start", view.seats().stream().allMatch(s -> s.id() != null && s.ready()));
            }
        } else if (view.phase() == TableGame.Phase.PLAYING) {
            if (view.allows("play")) {
                control(ctx, x + 10, y, w - 20, tr("play"), "play", playable(selected));
                y += 27;
            }
            if (view.allows("draw")) {
                control(ctx, x + 10, y, w - 20, tr("draw"), "draw", true);
            }
        } else if (host()) control(ctx, x + 10, y, w - 20, tr("rematch"), "rematch", true);
        // The bottom controls remain reachable at Minecraft's minimum GUI height (240).
        int bottom = height - 101;
        if (bottom < 187) bottom = 187;
        if (height >= 300) {
            control(ctx, x + 10, bottom, w - 20, tr(overview ? "view.seat" : "view.table"), "view", true);
            control(ctx, x + 10, bottom + 25, w - 20, tr(sway ? "sway.on" : "sway.off"), "sway", true);
        }
        control(ctx, x + 10, height - 51, w - 20, tr("leave"), "leave", true);
    }
    private void control(GuiGraphicsExtractor ctx, int x, int y, int w, String label, String action, boolean enabled) {
        boolean usable = enabled && (!pending() || Set.of("leave", "view", "sway", "menu").contains(action));
        controls.add(new Control(x, y, w, label, action, usable));
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 21;
        ctx.fill(x, y, x + w, y + 21, !usable ? 0xFF283235 : hover ? 0xFF4B6461 : 0xFF354B48);
        ctx.centeredText(font, label, x + w / 2, y + 6, usable ? PAPER : 0xFF70807C);
    }
    private void drawCard(GuiGraphicsExtractor ctx, Fan card, boolean selected) {
        double age = (System.nanoTime() - dealtAt) / 1e9;
        double deal = Mth.clamp((age - (card.index - offset) * .065) / .4, 0, 1);
        double enter = dealtAt == 0 ? 0 : (1 - deal) * 95;
        ctx.pose().pushMatrix();
        ctx.nextStratum();
        ctx.pose().translate((float) card.x, (float) (card.y + enter));
        ctx.pose().rotate((float) Math.toRadians(card.angle));
        int half = Math.round(card.width / 2);
        int tall = Math.round(card.width * .75f);
        ctx.fill(-half + 3, -tall + 4, half + 3, tall + 4, 0x65000000);
        if (selected) ctx.fill(-half - 2, -tall - 2, half + 2, tall + 2, playable(card.index) ? GOLD : 0xFF91AAA5);
        float scale = card.width / 8f;
        // Keep GUI depth independent of card size, so the menu can reliably overlay the hand.
        ctx.pose().scale(scale, scale);
        ctx.item(TableRenderer.card(view.hand().get(card.index)), -8, -8);
        ctx.pose().popMatrix();
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y(); int button = event.button();
        if (button != 0) return super.mouseClicked(event, doubleClick);
        for (Control c : controls) if (x >= c.x && x < c.x + c.w && y >= c.y && y < c.y + 21) {
            if (c.enabled) switch (c.action) {
                case "leave" -> onClose();
                case "view" -> overview = !overview;
                case "sway" -> sway = !sway;
                case "menu" -> menuExpanded = !menuExpanded;
                default -> send(c.action, "play".equals(c.action) ? selected : -1);
            }
            return true;
        }
        int index = cardAt(x, y);
        if (index >= 0) {
            selected = index;
            if (playable(index)) send("play", index);
            else { message = tr(myTurn() ? "error.cannot_play" : "error.not_your_turn"); messageUntil = System.currentTimeMillis() + 1800; }
            return true;
        }
        TableGame.Piece piece = pieceAt(x, y);
        if (piece != null) {
            hoverPiece = piece;
            if (view.allows(piece.action(), piece.id())) send(piece.action(), piece.id());
            else { message = tr(myTurn() ? ("flip".equals(piece.action()) ? "error.cannot_flip" : "error.cannot_play") : "error.not_your_turn"); messageUntil = System.currentTimeMillis() + 1800; }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (overHand(x, y)) {
            selected = Mth.clamp(selected + (vertical < 0 ? 1 : -1), 0, view.hand().size() - 1);
            ensureVisible();
        } else nudgeZoom(vertical);
        return true;
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            selected = Mth.clamp(selected + (key == GLFW.GLFW_KEY_RIGHT ? 1 : -1), 0, Math.max(0, view.hand().size() - 1));
            ensureVisible(); return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER) {
            if (playable(selected)) { send("play", selected); return true; }
            if (hoverPiece != null && view.allows(hoverPiece.action(), hoverPiece.id())) {
                send(hoverPiece.action(), hoverPiece.id()); return true;
            }
        }
        if (key == GLFW.GLFW_KEY_P && view.allows("draw")) { send("draw", -1); return true; }
        if (key == GLFW.GLFW_KEY_V) { overview = !overview; return true; }
        if (key == GLFW.GLFW_KEY_M) { sway = !sway; return true; }
        if (key == GLFW.GLFW_KEY_EQUAL || key == GLFW.GLFW_KEY_KP_ADD) { nudgeZoom(1); return true; }
        if (key == GLFW.GLFW_KEY_MINUS || key == GLFW.GLFW_KEY_KP_SUBTRACT) { nudgeZoom(-1); return true; }
        if (key == GLFW.GLFW_KEY_TAB && view.phase() != TableGame.Phase.LOBBY) { menuExpanded = !menuExpanded; return true; }
        return super.keyPressed(event);
    }
    public double swayX() { return allowSway() ? edge(mouseX / Math.max(1, width) * 2 - 1) : 0; }
    public double swayY() { return allowSway() ? edge(mouseY / Math.max(1, height) * 2 - 1) : 0; }
    private boolean overPanel(double x, double y) {
        return x >= panelX() && x <= width - 12 && y >= 12 && y < (compactMenu() ? 103 : height - 25);
    }
    private boolean overHand(double x, double y) {
        return !view.hand().isEmpty() && !overPanel(x, y) && y >= handCaptionY() - 8 && y < helpTop() - HAND_GAP;
    }
    private void nudgeZoom(double steps) {
        zoom = Mth.clamp(zoom + (float) steps * 0.08f, 0, 1);
    }
    private boolean allowSway() {
        return sway && !overPanel(mouseX, mouseY) && mouseY < (view.hand().isEmpty() ? height - 125 : handCaptionY() - 8);
    }
    private double edge(double normalized) {
        double amount = Mth.clamp((Math.abs(normalized) - .55) / .45, 0, 1);
        return Math.copySign(amount * amount, normalized);
    }
}


