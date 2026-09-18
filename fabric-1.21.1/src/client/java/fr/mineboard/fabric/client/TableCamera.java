package fr.mineboard.fabric.client;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.*;

/** A local camera rig; never moves or teleports the player's server entity. */
public final class TableCamera {
    public record Pose(Vec3d position, float yaw, float pitch) {}
    private static Pose displayed, from;
    private static long transition, lastFrame;
    private static int lastSeat = -99;
    private static boolean lastOverview, exiting;
    private static double swayX, swayY;
    private static final double DURATION = 0.75;

    public static void begin(Camera camera) {
        displayed = new Pose(camera.getPos(), camera.getYaw(), camera.getPitch());
        from = displayed; transition = System.nanoTime(); lastFrame = transition;
        lastSeat = -99; exiting = false; swayX = swayY = 0;
    }
    public static boolean active() { return displayed != null; }
    public static void exit() {
        if (displayed != null && !exiting) { from = displayed; transition = System.nanoTime(); exiting = true; }
    }
    public static void reset() { displayed = from = null; exiting = false; lastSeat = -99; }
    public static Pose frame(Camera vanilla, TableScreen screen) {
        if (displayed == null) return null;
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - lastFrame) / 1e9); lastFrame = now;
        Pose target;
        if (screen == null || exiting) {
            exit(); target = new Pose(vanilla.getPos(), vanilla.getYaw(), vanilla.getPitch());
        } else {
            int seat = screen.view().yourSeat();
            boolean overview = screen.overview() || seat < 0;
            if (lastSeat != seat || lastOverview != overview) {
                from = displayed; transition = now; lastSeat = seat; lastOverview = overview;
            }
            double smoothing = 1 - Math.exp(-dt * 8);
            swayX += (screen.swayX() - swayX) * smoothing;
            swayY += (screen.swayY() - swayY) * smoothing;
            BlockPos pos = screen.pos();
            Vec3d center = new Vec3d(pos.getX() + .5, pos.getY() + .16, pos.getZ() + .5);
            Vec3d eye = overview ? center.add(0, 2.5, 1.0)
                : center.add(0, 1.04, seat == 0 ? 1.55 : -1.55);
            Vec3d direction = center.subtract(eye);
            float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalLength()));
            target = new Pose(eye, yaw + (float) swayX * 4, pitch + (float) swayY * 2.5f);
        }
        double t = Math.min(1, (now - transition) / (DURATION * 1e9));
        double ease = t * t * (3 - 2 * t);
        displayed = new Pose(from.position().lerp(target.position(), ease),
            from.yaw() + MathHelper.wrapDegrees(target.yaw() - from.yaw()) * (float) ease,
            MathHelper.lerp((float) ease, from.pitch(), target.pitch()));
        if (exiting && t >= 1) { reset(); return null; }
        return displayed;
    }
}
