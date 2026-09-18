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
    private boolean overview, sway = true, opened, serverClosed, menuExpanded;
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
    @Override public boolean shouldPause() { return false; }
    @Override protected void init() {
        if (!opened) { TableCamera.begin(client.gameRenderer.getCamera()); opened = true; }
        ensureVisible();
    }
    public void update(TableGame.View next, String error) {
        if (next.phase() == TableGame.Phase.PLAYING && view.phase() != TableGame.Phase.PLAYING) {
            dealtAt = System.nanoTime();
            menuExpanded = false;
        }
        view = next; pendingUntil = 0;
        selected = MathHelper.clamp(selected, 0, Math.max(0, view.hand().size() - 1));
        ensureVisible();
        if (!error.isEmpty()) { message = tr("error." + error); messageUntil = System.currentTimeMillis() + 3000; }
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
    private boolean myTurn() { return view.phase() == TableGame.Phase.PLAYING && view.yourSeat() == view.turn(); }
    private boolean pending() { return System.currentTimeMillis() < pendingUntil; }
    private boolean playable(int index) {
        return myTurn() && index >= 0 && index < view.hand().size() && view.topCard() >= 0
            && Card.fromId(view.hand().get(index)).matches(Card.fromId(view.topCard()));
    }
    private boolean host() {
        int host = view.seats().getFirst().id() != null ? 0 : 1;
        return view.yourSeat() == host;
    }
    private void send(String action, int card) {
        if (pending()) return;
        pendingUntil = System.currentTimeMillis() + 1200;
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
            lastMouseX = mouseX; lastMouseY = mouseY;
        }
        controls.clear();
        ctx.fill(12, 12, 140, 43, INK);
        ctx.fill(12, 12, 15, 43, GOLD);
        ctx.drawText(textRenderer, "MINEBOARD", 23, 19, PAPER, false);
        ctx.drawText(textRenderer, tr("edition"), 23, 31, MUTED, false);
        String status = switch (view.phase()) {
            case LOBBY -> tr("status.lobby");
            case PLAYING -> myTurn() ? tr("status.your_turn") : tr("status.turn", view.seats().get(view.turn()).name());
            case FINISHED -> tr("status.winner", view.seats().get(view.winner()).name());
        };
        int labelWidth = textRenderer.getWidth(status);
        int statusY = height < 300 ? 51 : 58;
        ctx.fill(Math.max(12, (panelX() - labelWidth) / 2 - 9), statusY - 6,
            Math.min(panelX() - 4, (panelX() + labelWidth) / 2 + 9), statusY + 15, INK);
        ctx.drawCenteredTextWithShadow(textRenderer, status, panelX() / 2, statusY, myTurn() ? GOLD : PAPER);
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
            ctx.drawCenteredTextWithShadow(textRenderer, tr("lobby.hint"), panelX() / 2, height - 66, PAPER);
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
        ctx.drawText(textRenderer, tr(width < 550 ? "controls.short" : "controls"), 12, height - 12, MUTED, false);
        ctx.getMatrices().pop();
    }
    private void drawPanel(DrawContext ctx) {
        int x = panelX(), w = panelWidth();
        if (compactMenu()) {
            ctx.fill(x, 12, x + w, 103, INK);
            ctx.fill(x, 12, x + w, 14, GOLD);
            control(ctx, x + 10, 22, w - 20, tr("menu.open"), "menu", true);
            String counts = view.seats().get(0).count() + " / " + view.seats().get(1).count() + " " + tr("cards");
            ctx.drawCenteredTextWithShadow(textRenderer, counts, x + w / 2, 53, MUTED);
            if (view.phase() == TableGame.Phase.FINISHED) {
                control(ctx, x + 10, 72, w - 20, tr("rematch"), "rematch", host());
            } else {
                control(ctx, x + 10, 72, w - 20, tr("draw"), "draw", myTurn());
            }
            return;
        }
        ctx.fill(x, 12, x + w, height - 25, INK);
        ctx.fill(x, 12, x + w, 14, GOLD);
        if (view.phase() == TableGame.Phase.LOBBY) ctx.drawText(textRenderer, tr("lobby"), x + 12, 25, PAPER, false);
        else control(ctx, x + 10, 20, w - 20, tr("menu.close"), "menu", true);
        for (int i = 0; i < 2; i++) {
            TableGame.Seat seat = view.seats().get(i);
            int y = 47 + i * 39;
            ctx.fill(x + 10, y, x + w - 10, y + 33, 0xFF233438);
            ctx.fill(x + 10, y, x + 12, y + 33, view.phase() == TableGame.Phase.PLAYING && view.turn() == i ? GOLD : 0xFF48605D);
            String name = seat.id() == null ? tr("empty_seat") : seat.name();
            if (view.yourSeat() == i) name += " •";
            ctx.drawText(textRenderer, textRenderer.trimToWidth(name, w - 32), x + 19, y + 6, PAPER, false);
            String detail = seat.id() == null ? tr("available") : view.phase() == TableGame.Phase.LOBBY
                ? tr(seat.ready() ? "ready" : "waiting") : tr("hand", seat.count());
            ctx.drawText(textRenderer, detail, x + 19, y + 19, seat.ready() ? 0xFF85C6A3 : MUTED, false);
        }
        int y = 131;
        if (view.phase() == TableGame.Phase.LOBBY) {
            if (view.yourSeat() < 0) control(ctx, x + 10, y, w - 20, tr("join"), "join",
                view.seats().stream().anyMatch(s -> s.id() == null));
            else control(ctx, x + 10, y, w - 20, tr(view.seats().get(view.yourSeat()).ready() ? "unready" : "be_ready"), "ready", true);
            y += 27;
            if (host()) control(ctx, x + 10, y, w - 20, tr("start"), "start", view.seats().stream().allMatch(s -> s.id() != null && s.ready()));
        } else if (view.phase() == TableGame.Phase.PLAYING) {
            control(ctx, x + 10, y, w - 20, tr("play"), "play", playable(selected));
            y += 27;
            control(ctx, x + 10, y, w - 20, tr("draw"), "draw", myTurn());
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
    private void control(DrawContext ctx, int x, int y, int w, String label, String action, boolean enabled) {
        boolean usable = enabled && (!pending() || Set.of("leave", "view", "sway", "menu").contains(action));
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
                default -> send(c.action, selected);
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
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!view.hand().isEmpty()) { selected = MathHelper.clamp(selected + (vertical < 0 ? 1 : -1), 0, view.hand().size() - 1); ensureVisible(); }
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            selected = MathHelper.clamp(selected + (key == GLFW.GLFW_KEY_RIGHT ? 1 : -1), 0, Math.max(0, view.hand().size() - 1));
            ensureVisible(); return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER && playable(selected)) { send("play", selected); return true; }
        if (key == GLFW.GLFW_KEY_P && myTurn()) { send("draw", -1); return true; }
        if (key == GLFW.GLFW_KEY_V) { overview = !overview; return true; }
        if (key == GLFW.GLFW_KEY_M) { sway = !sway; return true; }
        if (key == GLFW.GLFW_KEY_TAB && view.phase() != TableGame.Phase.LOBBY) { menuExpanded = !menuExpanded; return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    public double swayX() { return allowSway() ? edge(mouseX / Math.max(1, width) * 2 - 1) : 0; }
    public double swayY() { return allowSway() ? edge(mouseY / Math.max(1, height) * 2 - 1) : 0; }
    private boolean overPanel(double x, double y) {
        return x >= panelX() && x <= width - 12 && y >= 12 && y < (compactMenu() ? 103 : height - 25);
    }
    private boolean allowSway() {
        return sway && !overPanel(mouseX, mouseY) && mouseY < (view.hand().isEmpty() ? height - 125 : handCaptionY() - 8);
    }
    private double edge(double normalized) {
        double amount = MathHelper.clamp((Math.abs(normalized) - .55) / .45, 0, 1);
        return Math.copySign(amount * amount, normalized);
    }
}
