package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;

public class CursorTrail extends Entity {

    // ── Tune these for your device (perf vs visual length) ───────────────
    // Lower capacity = faster, but shorter max trail
    private static final int TRAIL_CAPACITY = 96; // was 128 — still plenty long, much cheaper

    // Distance (pixels) between new trail stamps
    // Higher = sparser/faster, lower = denser/smoother but more stamps
    private static final float STAMP_STEP = 3.5f; // similar to old SAMPLE_SIZE=3, tuned for 30-60
                                                  // FPS

    // How fast each stamp fades (alpha units per second)
    // Higher value = shorter trail, lower value = longer trail
    // 3.0f ≈ 0.33 seconds lifetime — gives nice long trail without killing FPS
    private static final float FADE_RATE = 3.0f;
    // ─────────────────────────────────────────────────────────────────────

    private final Sprite[] sprites;

    private final float offsetX;
    private final float offsetY;

    private int nextSpriteIndex = 0;
    private boolean spawning = false;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;

    public CursorTrail(TextureRegion trailTex, CursorSprite cursor) {
        sprites = new Sprite[TRAIL_CAPACITY];

        offsetX = -trailTex.getWidth() / 2f;
        offsetY = -trailTex.getHeight() / 2f;

        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            Sprite s = new Sprite(0, 0, trailTex);
            s.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
            s.setAlpha(0f);
            s.setScale(cursor.baseSize);
            sprites[i] = s;
            attachChild(s);
        }
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        spawning = enabled;
        if (!enabled) {
            lastX = Float.NaN;
            lastY = Float.NaN;
            nextSpriteIndex = 0;
            for (Sprite s : sprites) {
                s.setAlpha(0f);
            }
        }
    }

    public void update(float x, float y, float dt) {
        if (!spawning) return;

        if (Float.isNaN(lastX)) {
            stamp(x, y);
            lastX = x;
            lastY = y;
            return;
        }

        // ── Distance-based stamping (much cheaper than Bresenham every frame) ──
        final float dx = x - lastX;
        final float dy = y - lastY;
        final float distSq = dx * dx + dy * dy;

        if (distSq > STAMP_STEP * STAMP_STEP) {
            final float dist = (float) Math.sqrt(distSq);
            final int numStamps = Math.max(1, (int) (dist / STAMP_STEP));

            final float stepX = dx / numStamps;
            final float stepY = dy / numStamps;

            float px = lastX;
            float py = lastY;

            for (int k = 1; k <= numStamps; k++) {
                px += stepX;
                py += stepY;
                stamp(px, py);
            }

            lastX = x;
            lastY = y;
        }

        // ── Fade all active stamps (time-based, frame-rate independent) ──
        fadeTrails(dt);
    }

    private void stamp(float x, float y) {
        final Sprite s = sprites[nextSpriteIndex];
        s.setPosition(x + offsetX, y + offsetY);
        s.setAlpha(1f);

        nextSpriteIndex = (nextSpriteIndex + 1) % TRAIL_CAPACITY;
    }

    private void fadeTrails(float dt) {
        final float fadeAmount = FADE_RATE * dt;

        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            final Sprite s = sprites[i];
            if (s.getAlpha() > 0f) {
                float newAlpha = s.getAlpha() - fadeAmount;
                s.setAlpha(Math.max(0f, newAlpha));
            }
        }
    }
}