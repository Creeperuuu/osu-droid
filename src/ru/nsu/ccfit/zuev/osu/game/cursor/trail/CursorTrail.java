package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorTrail extends Entity {

    // ── Tune these ────────────────────────────────────────────────────────
    private static final int TRAIL_CAPACITY = 128; // max trail points
    private static final int SAMPLE_SIZE = 8; // Bresenham: 1 point every N steps
    private static final float SPRITE_SCALE = 0.8f;
    // ─────────────────────────────────────────────────────────────────────

    private final Sprite[] spritePool;
    private final float[] trailX;
    private final float[] trailY;

    // Ring buffer indices
    private int head = 0; // next write index
    private int tail = 0; // oldest point index
    private int count = 0; // current number of active points

    private final float halfW;
    private final float halfH;
    private final CursorSprite cursor;

    private boolean spawning = false;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float currentFPS = 60f;

    public CursorTrail(TextureRegion texture, CursorSprite cursor) {
        this.cursor = cursor;

        float scale = cursor.baseSize * SPRITE_SCALE;
        halfW = (texture.getWidth() * scale) / 2f;
        halfH = (texture.getHeight() * scale) / 2f;

        trailX = new float[TRAIL_CAPACITY];
        trailY = new float[TRAIL_CAPACITY];
        spritePool = new Sprite[TRAIL_CAPACITY];

        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            Sprite s = new Sprite(0, 0, texture);
            s.setScale(scale);
            s.setAlpha(0f);
            spritePool[i] = s;
        }
    }

    public void attachToScene(Scene scene) {
        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            scene.attachChild(spritePool[i]);
        }
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        spawning = enabled;
        if (!enabled) {
            head = 0;
            tail = 0;
            count = 0;
            lastX = Float.NaN;
            lastY = Float.NaN;
            for (int i = 0; i < TRAIL_CAPACITY; i++)
                spritePool[i].setAlpha(0f);
        }
    }

    public void update(float x, float y, float dt) {
        if (dt > 0) currentFPS = 1f / dt;
        if (!spawning) return;

        if (Float.isNaN(lastX)) {
            lastX = x;
            lastY = y;
            return;
        }

        // 1. Add points via Bresenham
        addCursorPoints((int) lastX, (int) lastY, (int) x, (int) y);
        lastX = x;
        lastY = y;

        // 2. Remove from tail — only if trail is long enough to be worth trimming.
        //    Skip removal when count is small so slow movement stays visible.
        if (count > 8) {
            float FPSmod = Math.max(currentFPS, 1) / 60f;
            int removeCount = (int) (count / (6f * FPSmod)) + 1;
            // Never remove more than half the trail in one frame
            removeCount = Math.min(removeCount, count / 2);
            for (int i = 0; i < removeCount; i++) {
                spritePool[tail].setAlpha(0f);
                tail = (tail + 1) % TRAIL_CAPACITY;
                count--;
            }
        }

        // 3. Refresh visible sprites
        refreshSprites(x, y);
        updateRotation();
    }

    private void addCursorPoints(int x1, int y1, int x2, int y2) {
        int d = 0;
        int dy = Math.abs(y2 - y1);
        int dx = Math.abs(x2 - x1);
        int dy2 = dy << 1;
        int dx2 = dx << 1;
        int ix = x1 < x2 ? 1 : -1;
        int iy = y1 < y2 ? 1 : -1;

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

    private void pushPoint(float x, float y) {
        if (count == TRAIL_CAPACITY) {
            // Buffer full — evict oldest
            spritePool[tail].setAlpha(0f);
            tail = (tail + 1) % TRAIL_CAPACITY;
            count--;
        }
        trailX[head] = x;
        trailY[head] = y;
        head = (head + 1) % TRAIL_CAPACITY;
        count++;
    }

    private void refreshSprites(float curX, float curY) {
        // Alpha step spans 0→1 across the whole trail
        float alphaStep = count > 0 ? 1f / count : 0f;
        float alpha = 0f;

        for (int i = 0; i < count; i++) {
            int idx = (tail + i) % TRAIL_CAPACITY;
            alpha += alphaStep;
            Sprite s = spritePool[idx];
            s.setPosition(trailX[idx] - halfW, trailY[idx] - halfH);
            s.setAlpha(alpha);
        }
    }

    private void updateRotation() {
        if (!OsuSkin.get().isRotateCursorTrail()) return;
        float rot = cursor.getRotation();
        for (int i = 0; i < count; i++) {
            int idx = (tail + i) % TRAIL_CAPACITY;
            spritePool[idx].setRotation(rot);
        }
    }
}