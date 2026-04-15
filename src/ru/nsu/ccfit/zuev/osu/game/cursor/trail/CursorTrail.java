package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;

public class CursorTrail extends Entity {

    // ── Tune these ────────────────────────────────────────────────────────
    private static final int TRAIL_CAPACITY = 1024; // Increased to prevent clipping on fast swipes
    private static final int SAMPLE_SIZE = 3; // Pixels between texture stamps
    private static final float MAX_LIFETIME = 0.35f; // How long the trail lives in seconds
    // ─────────────────────────────────────────────────────────────────────

    private final StampSprite stamp;

    // Time-based ring buffer
    private final float[] px = new float[TRAIL_CAPACITY];
    private final float[] py = new float[TRAIL_CAPACITY];
    private final float[] pTime = new float[TRAIL_CAPACITY];

    private int head = 0;
    private int count = 0;
    private int sampleCounter = 0; // Persists across frames to fix slow movement

    private boolean spawning = false;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float currentTime = 0f;

    private final float offsetX;
    private final float offsetY;
    private final float baseSize;

    public CursorTrail(TextureRegion trailTex, CursorSprite cursor) {
        stamp = new StampSprite(0, 0, trailTex);
        stamp.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE); // Additive blending

        this.baseSize = cursor.baseSize;

        offsetX = -trailTex.getWidth() / 2f;
        offsetY = -trailTex.getHeight() / 2f;
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        spawning = enabled;
        if (!enabled) {
            head = 0;
            count = 0;
            sampleCounter = 0;
            lastX = Float.NaN;
            lastY = Float.NaN;
        }
    }

    public void update(float x, float y, float dt) {
        currentTime += dt; // Track absolute time for smooth decay
        if (!spawning) return;

        if (Float.isNaN(lastX)) {
            pushPoint(x, y);
            lastX = x;
            lastY = y;
            return;
        }

        // Fill all points along the path
        addCursorPoints((int) lastX, (int) lastY, (int) x, (int) y);
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

    private void addCursorPoints(int x1, int y1, int x2, int y2) {
        int d = 0, dy = Math.abs(y2 - y1), dx = Math.abs(x2 - x1);
        int dy2 = dy << 1, dx2 = dx << 1;
        int ix = x1 < x2 ? 1 : -1, iy = y1 < y2 ? 1 : -1;

        if (dy <= dx) {
            for (; ; ) {
                // Persistent counter guarantees a drop every SAMPLE_SIZE pixels, even if moving
                // slowly
                if (sampleCounter >= SAMPLE_SIZE) {
                    pushPoint(x1, y1);
                    sampleCounter = 0;
                }
                if (x1 == x2) break;
                x1 += ix;
                d += dy2;
                if (d > dx) {
                    y1 += iy;
                    d -= dx2;
                }
                sampleCounter++;
            }
        } else {
            for (; ; ) {
                if (sampleCounter >= SAMPLE_SIZE) {
                    pushPoint(x1, y1);
                    sampleCounter = 0;
                }
                if (y1 == y2) break;
                y1 += iy;
                d += dx2;
                if (d > dy) {
                    x1 += ix;
                    d -= dy2;
                }
                sampleCounter++;
            }
        }
    }

    @Override
    protected void onManagedDraw(GL10 pGL, Camera pCamera) {
        if (count == 0) return;

        // 1. Prune dead points strictly based on time
        int activeCount = 0;
        for (int i = 0; i < count; i++) {
            int idx = (head - 1 - i);
            if (idx < 0) idx += TRAIL_CAPACITY;

            if (currentTime - pTime[idx] > MAX_LIFETIME) {
                break; // Because points are chronological, we can safely stop here
            }
            activeCount++;
        }
        count = activeCount;

        // 2. Draw active points (Oldest to Newest for proper layering)
        for (int i = count - 1; i >= 0; i--) {
            int idx = (head - 1 - i);
            if (idx < 0) idx += TRAIL_CAPACITY;

            float age = currentTime - pTime[idx];

            // Ratio goes from 1.0 (brand new) to 0.0 (ready to die)
            float ratio = Math.max(0f, 1f - (age / MAX_LIFETIME));

            stamp.setPosition(px[idx] + offsetX, py[idx] + offsetY);
            stamp.setAlpha(ratio);

            // This is the magic line that tapers the tail exactly like the image
            stamp.setScale(baseSize * ratio);

            stamp.drawNow(pGL, pCamera);
        }
    }

    // A helper class to safely expose AndEngine's protected drawing methods
    private static class StampSprite extends Sprite {
        public StampSprite(float pX, float pY, TextureRegion pTextureRegion) {
            super(pX, pY, pTextureRegion);
        }

        public void drawNow(GL10 pGL, Camera pCamera) {
            super.onManagedDraw(pGL, pCamera);
        }
    }
}
