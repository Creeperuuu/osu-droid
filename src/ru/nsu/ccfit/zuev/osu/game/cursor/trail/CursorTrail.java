package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;

public class CursorTrail extends Entity {

    // ── Optimized for Smoothness ─────────────────────────────────────────
    private static final int TRAIL_CAPACITY = 2048; // Higher capacity for high-density points
    private static final float MAX_LIFETIME = 0.35f;
    // ─────────────────────────────────────────────────────────────────────

    private final StampSprite stamp;

    private final float[] px = new float[TRAIL_CAPACITY];
    private final float[] py = new float[TRAIL_CAPACITY];
    private final float[] pTime = new float[TRAIL_CAPACITY];

    private int head = 0;
    private int count = 0;

    private boolean spawning = false;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float currentTime = 0f;

    private final float offsetX;
    private final float offsetY;
    private final float baseSize;

    public CursorTrail(TextureRegion trailTex, CursorSprite cursor) {
        stamp = new StampSprite(0, 0, trailTex);
        stamp.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);

        this.baseSize = cursor.baseSize;
        offsetX = -trailTex.getWidth() / 2f;
        offsetY = -trailTex.getHeight() / 2f;
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        spawning = enabled;
        if (!enabled) {
            head = 0;
            count = 0;
            lastX = Float.NaN;
            lastY = Float.NaN;
        }
    }

    public void update(float x, float y, float dt) {
        currentTime += dt;
        if (!spawning) return;

        if (Float.isNaN(lastX)) {
            pushPoint(x, y);
            lastX = x;
            lastY = y;
            return;
        }

        // Drop points much more frequently to eliminate "hexagon" corners
        fillPath(lastX, lastY, x, y);

        lastX = x;
        lastY = y;
    }

    private void pushPoint(float x, float y) {
        px[head] = x;
        py[head] = y;
        pTime[head] = currentTime;

        head = (head + 1) % TRAIL_CAPACITY;
        if (count < TRAIL_CAPACITY) count++;
    }

    /**
     * High-density path filling. This ensures that even during fast spins, points are placed close
     * enough together that they form a perfect curve.
     */
    private void fillPath(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Drop a point roughly every 1.5 pixels regardless of speed.
        // This is the "sweet spot" for perfect circles without lag.
        float stepSize = 1.5f;
        int steps = (int) (distance / stepSize);

        if (steps <= 0) {
            pushPoint(x2, y2);
            return;
        }

        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            pushPoint(x1 + dx * t, y1 + dy * t);
        }
    }

    @Override
    protected void onManagedDraw(GL10 pGL, Camera pCamera) {
        if (count == 0) return;

        // Prune old points
        int activeCount = 0;
        for (int i = 0; i < count; i++) {
            int idx = (head - 1 - i);
            if (idx < 0) idx += TRAIL_CAPACITY;
            if (currentTime - pTime[idx] > MAX_LIFETIME) break;
            activeCount++;
        }
        count = activeCount;

        // Draw oldest to newest
        for (int i = count - 1; i >= 0; i--) {
            int idx = (head - 1 - i);
            if (idx < 0) idx += TRAIL_CAPACITY;

            float age = currentTime - pTime[idx];
            float ratio = Math.max(0f, 1f - (age / MAX_LIFETIME));

            stamp.setPosition(px[idx] + offsetX, py[idx] + offsetY);

            // Linear scaling and fading as requested
            stamp.setAlpha(ratio);
            stamp.setScale(baseSize * ratio);

            stamp.drawNow(pGL, pCamera);
        }
    }

    private static class StampSprite extends Sprite {
        public StampSprite(float pX, float pY, TextureRegion pTextureRegion) {
            super(pX, pY, pTextureRegion);
        }

        public void drawNow(GL10 pGL, Camera pCamera) {
            super.onManagedDraw(pGL, pCamera);
        }
    }
}
