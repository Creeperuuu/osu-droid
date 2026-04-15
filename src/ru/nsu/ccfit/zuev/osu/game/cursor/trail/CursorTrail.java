package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorTrail extends Entity {

    // ── Tune these ────────────────────────────────────────────────────────
    // SAMPLE_SIZE controls both point density AND the fixed sprite width.
    // Higher = fewer sprites needed, better FPS, slightly less smooth.
    private static final int SAMPLE_SIZE = 5;
    // Total trail length in points. Length in pixels = TRAIL_CAPACITY * SAMPLE_SIZE.
    // 120 * 5 = 600px trail length.
    private static final int TRAIL_CAPACITY = 120;
    // ─────────────────────────────────────────────────────────────────────

    private final Sprite[] segments;
    private final TextureRegion texture;
    private final CursorSprite cursor;

    // Ring buffer
    private final float[] px = new float[TRAIL_CAPACITY + 1];
    private final float[] py = new float[TRAIL_CAPACITY + 1];
    private int head = 0;
    private int count = 0;

    private boolean spawning = false;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float currentFPS = 60f;

    // Fixed sprite dimensions — computed once at construction, never changed
    private final float spriteW; // = SAMPLE_SIZE (segment length)
    private final float spriteH; // = texture height scaled by cursor size

    public CursorTrail(TextureRegion texture, CursorSprite cursor) {
        this.texture = texture;
        this.cursor = cursor;

        // Width = exactly one Bresenham step so no gap and no overlap
        spriteW = SAMPLE_SIZE;
        // Height = texture natural height * cursor scale — this IS the trail thickness
        spriteH = texture.getHeight() * cursor.baseSize;

        segments = new Sprite[TRAIL_CAPACITY];
        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            // Construct with fixed size — never resized at runtime
            Sprite s = new Sprite(0, 0, spriteW, spriteH, texture);
            // Rotation always pivots around sprite center
            s.setRotationCenter(spriteW / 2f, spriteH / 2f);
            s.setAlpha(0f);
            segments[i] = s;
        }
    }

    public void attachToScene(Scene scene) {
        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            scene.attachChild(segments[i]);
        }
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        spawning = enabled;
        if (!enabled) {
            head = 0;
            count = 0;
            lastX = Float.NaN;
            lastY = Float.NaN;
            for (Sprite s : segments) s.setAlpha(0f);
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

        addCursorPoints((int) lastX, (int) lastY, (int) x, (int) y);
        lastX = x;
        lastY = y;

        // Remove from tail proportional to size — trail fades out naturally
        if (count > 4) {
            float FPSmod = Math.max(currentFPS, 1f) / 60f;
            int removeCount = Math.min((int) (count / (6f * FPSmod)) + 1, count - 2);
            count -= removeCount;
        }

        refreshSegments();
    }

    private void refreshSegments() {
        int segCount = Math.min(count - 1, TRAIL_CAPACITY);
        int start = head - count;
        float alphaStep = segCount > 0 ? 1f / segCount : 0f;

        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            Sprite s = segments[i];
            if (i >= segCount) {
                // Avoid redundant GL calls — only hide if currently visible
                if (s.getAlpha() != 0f) s.setAlpha(0f);
                continue;
            }

            int idxA = ((start + i) % (TRAIL_CAPACITY + 1) + (TRAIL_CAPACITY + 1)) % (TRAIL_CAPACITY + 1);
            int idxB = ((start + i + 1) % (TRAIL_CAPACITY + 1) + (TRAIL_CAPACITY + 1)) % (TRAIL_CAPACITY + 1);

            float ax = px[idxA], ay = py[idxA];
            float bx = px[idxB], by = py[idxB];
            float dx = bx - ax;
            float dy = by - ay;

            // Skip degenerate segments
            if (dx == 0 && dy == 0) {
                if (s.getAlpha() != 0f) s.setAlpha(0f);
                continue;
            }

            // atan2 gives us angle without needing sqrt — position at midpoint
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            float midX = (ax + bx) * 0.5f;
            float midY = (ay + by) * 0.5f;

            s.setPosition(midX - spriteW * 0.5f, midY - spriteH * 0.5f);
            s.setRotation(OsuSkin.get().isRotateCursorTrail()
                    ? angle + cursor.getRotation()
                    : angle);
            s.setAlpha((i + 1) * alphaStep);
        }
    }

    private void pushPoint(float x, float y) {
        int idx = head % (TRAIL_CAPACITY + 1);
        px[idx] = x;
        py[idx] = y;
        head++;
        if (count < TRAIL_CAPACITY + 1) count++;
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