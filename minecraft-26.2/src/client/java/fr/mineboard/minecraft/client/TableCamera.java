package fr.mineboard.minecraft.client;

import fr.mineboard.core.Layouts;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

/** A local camera rig; never moves or teleports the player's server entity. */
public final class TableCamera {
    public record Pose(Vec3 position, float yaw, float pitch) {}
    private static Pose displayed, from;
    private static long transition, lastFrame;
    private static int lastSeat = -99;
    private static boolean lastOverview, exiting;
    private static double swayX, swayY, zoom;
    private static final double DURATION = 0.75;

    public static void begin(Camera camera) {
        displayed = new Pose(camera.position(), camera.yRot(), camera.xRot());
        from = displayed; transition = System.nanoTime(); lastFrame = transition;
        lastSeat = -99; exiting = false; swayX = swayY = 0; zoom = -1;
    }
    public static boolean active() { return displayed != null; }
    public static void exit() {
        if (displayed != null && !exiting) { from = displayed; transition = System.nanoTime(); exiting = true; }
    }
    public static void reset() { displayed = from = null; exiting = false; lastSeat = -99; zoom = -1; }
    public static Pose frame(Camera vanilla, TableScreen screen) {
        if (displayed == null) return null;
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - lastFrame) / 1e9); lastFrame = now;
        Pose target;
        if (screen == null || exiting) {
            exit(); target = new Pose(vanilla.position(), vanilla.yRot(), vanilla.xRot());
        } else {
            int seat = screen.focusSeat();
            boolean overview = screen.overview() || screen.view().yourSeat() < 0;
            int count = Math.max(1, screen.view().seats().size());
            if (lastSeat != seat || lastOverview != overview) {
                from = displayed; transition = now; lastSeat = seat; lastOverview = overview;
            }
            double smoothing = 1 - Math.exp(-dt * 8);
            swayX += (screen.swayX() - swayX) * smoothing;
            swayY += (screen.swayY() - swayY) * smoothing;
            if (zoom < 0) zoom = screen.zoom();
            else zoom += (screen.zoom() - zoom) * smoothing;
            BlockPos pos = screen.pos();
            double span = Math.max(screen.view().boardSpan(), Layouts.span(screen.view().pieces()));
            Vec3 center = new Vec3(pos.getX() + .5, pos.getY() + .16, pos.getZ() + .5);
            Vec3 lookAt = center;
            Vec3 eye;
            if (overview) {
                eye = center.add(0, lerp(2.5, 0.92, zoom) * span, lerp(1.0, 0.22, zoom) * span);
            } else if (count <= 2) {
                double height = lerp(1.04, 0.42, zoom) * span;
                double back = lerp(1.55, 0.68, zoom) * span;
                eye = center.add(0, height, seat == 0 ? back : -back);
            } else {
                double[] local = Layouts.seatCenter(seat, count);
                lookAt = new Vec3(pos.getX() + local[0], pos.getY() + .16, pos.getZ() + local[1]);
                double ang = Math.toRadians(Layouts.seatYaw(seat, count));
                double height = lerp(1.05, 0.48, zoom);
                double back = lerp(1.2, 0.58, zoom);
                eye = lookAt.add(Math.sin(ang) * back, height, Math.cos(ang) * back);
            }
            Vec3 direction = lookAt.subtract(eye);
            float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance()));
            target = new Pose(eye, yaw + (float) swayX * 4, pitch + (float) swayY * 2.5f);
        }
        double t = Math.min(1, (now - transition) / (DURATION * 1e9));
        double ease = t * t * (3 - 2 * t);
        displayed = new Pose(from.position().lerp(target.position(), ease),
            from.yaw() + Mth.wrapDegrees(target.yaw() - from.yaw()) * (float) ease,
            Mth.lerp((float) ease, from.pitch(), target.pitch()));
        if (exiting && t >= 1) { reset(); return null; }
        return displayed;
    }
    private static double lerp(double far, double near, double amount) { return far + (near - far) * amount; }
}
