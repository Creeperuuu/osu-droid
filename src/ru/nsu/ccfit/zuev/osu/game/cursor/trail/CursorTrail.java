package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;

public class CursorTrail extends Entity {

    // ── Tune these ────────────────────────────────────────────────────────
    private static final int TRAIL_CAPACITY = 128; // Increased for a longer trail
    private static final int SAMPLE_SIZE = 3; // Distance between texture stamps
    // ─────────────────────────────────────────────────────────────────────

    private final Sprite[] sprites;

    // Ring buffer of recorded positions
    private final float[] px = new float[TRAIL_CAPACITY + 1];
    private final float[] py = new float[TRAIL_CAPACITY + 1];
    private int head = 0;
    private int count = 0;

    private boolean spawning = false;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float currentFPS = 60f;

    private final float offsetX;
    private final float offsetY;

    public CursorTrail(TextureRegion trailTex, CursorSprite cursor) {
        sprites = new Sprite[TRAIL_CAPACITY];

        // Offset to center the texture on the coordinates
        offsetX = -trailTex.getWidth() / 2f;
        offsetY = -trailTex.getHeight() / 2f;

        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            Sprite s = new Sprite(0, 0, trailTex);
            // Additive blending merges overlapping textures smoothly
            s.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
            s.setAlpha(0f);
            s.setScale(cursor.baseSize); // Match cursor sizing
            sprites[i] = s;

            // Attach as children to THIS entity to simplify scene management
            attachChild(s);
        }
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        spawning = enabled;
        if (!enabled) {
            head = 0;
            count = 0;
            lastX = Float.NaN;
            lastY = Float.NaN;
            for (Sprite s : sprites) s.setAlpha(0f);
        }
    }

    public void update(float x, float y, float dt) {
        if (dt > 0) currentFPS = 1f / dt;
        if (!spawning) return;

        if (Float.isNaN(lastX)) {
            pushPoint(x, y);
            lastX = x;
            lastY = y;
            return;
        }

        // Fill all points along the path using Bresenham
        addCursorPoints((int) lastX, (int) lastY, (int) x, (int) y);
        lastX = x;
        lastY = y;

        // Remove from tail proportional to size
        if (count > 2) {
            float FPSmod = Math.max(currentFPS, 1f) / 60f;
            // Changed from 6f to 12f: Makes the trail decay slower (longer trail)
            int removeCount = Math.min((int) (count / (12f * FPSmod)) + 1, count - 2);
            count -= removeCount;
        }

        refreshSprites();
    }

    private void pushPoint(float x, float y) {
        px[head % (TRAIL_CAPACITY + 1)] = x;
        py[head % (TRAIL_CAPACITY + 1)] = y;
        head++;
        if (count < TRAIL_CAPACITY + 1) count++;
    }

    private void refreshSprites() {
        int segments = Math.min(count - 1, TRAIL_CAPACITY);
        int start = head - count; // oldest point offset

        float alphaStep = segments > 0 ? 1f / segments : 0f;

        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            Sprite s = sprites[i];

            // Hide unused sprites
            if (i >= segments) {
                if (s.getAlpha() != 0f) s.setAlpha(0f);
                continue;
            }

            int idx = (start + i) & Integer.MAX_VALUE;
            float ax = px[idx % (TRAIL_CAPACITY + 1)];
            float ay = py[idx % (TRAIL_CAPACITY + 1)];

            // Position and fade out the tail
            s.setPosition(ax + offsetX, ay + offsetY);
            s.setAlpha((i + 1) * alphaStep);
        }
    }

    private void addCursorPoints(int x1, int y1, int x2, int y2) {
        int d = 0, dy = Math.abs(y2 - y1), dx = Math.abs(x2 - x1);
        int dy2 = dy << 1, dx2 = dx << 1;
        int ix = x1 < x2 ? 1 : -1, iy = y1 < y2 ? 1 : -1;

        if (dy <= dx) {
            for (int i = 0; ; i++) {
                if (i == SAMPLE_SIZE) {
                    pushPoint(x1, y1);
                    i = 0;
                }
                if (x1 == x2) break;
                x1 += ix;
                d += dy2;
                if (d > dx) {
                    y1 += iy;
                    d -= dx2;
                }
            }
        } else {
            for (int i = 0; ; i++) {
                if (i == SAMPLE_SIZE) {
                    pushPoint(x1, y1);
                    i = 0;
                }
                if (y1 == y2) break;
                y1 += iy;
                d += dx2;
                if (d > dy) {
                    x1 += ix;
                    d -= dy2;
                }
            }
        }
    }
}
