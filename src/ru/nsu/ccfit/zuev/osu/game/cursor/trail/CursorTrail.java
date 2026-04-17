package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorTrail extends Entity {

    private static final int TRAIL_CAPACITY = 2048;
    private static final float TRAIL_LIFETIME = 0.5f;
    private static final float MAX_OPACITY = 0.4f;
    private static final float FADE_DURATION_RATIO = 1.0f;
    private static final float SCALE_DURATION_RATIO = 1.0f;
    private static final float TRAIL_STEP_SIZE = 2.2f;

    private final StampSprite stamp;
    private final CursorSprite cursor;

    private final float[] px = new float[TRAIL_CAPACITY];
    private final float[] py = new float[TRAIL_CAPACITY];
    private final float[] pTime = new float[TRAIL_CAPACITY];

    private int head = 0;
    private int count = 0;
    private boolean spawning = false;
    private float currentTime = 0f;

    private float p1x = Float.NaN, p1y = Float.NaN;
    private float p2x = Float.NaN, p2y = Float.NaN;

    private final float offsetX;
    private final float offsetY;
    private final float baseSize;

    public CursorTrail(TextureRegion trailTex, CursorSprite cursor) {
        this.cursor = cursor;
        this.stamp = new StampSprite(0, 0, trailTex);

        // SWITCHED: Standard Alpha Blending (0x302, 0x303)
        // This supports soft edges and true transparency without the "glow"
        stamp.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        this.baseSize = cursor.baseSize;
        offsetX = -trailTex.getWidth() / 2f;
        offsetY = -trailTex.getHeight() / 2f;
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        this.spawning = enabled;
        // We stopped resetting 'count' and 'head' here.
        // This allows existing particles to finish their lifetime naturally.
        if (!enabled) {
            p1x = Float.NaN;
            p1y = Float.NaN;
            p2x = Float.NaN;
            p2y = Float.NaN;
        }
    }

    public void update(float x, float y, float dt) {
        currentTime += dt;
        if (!spawning) return;

        if (Float.isNaN(p2x)) {
            p2x = x;
            p2y = y;
            pushPoint(x, y);
            return;
        }

        p1x = p2x;
        p1y = p2y;
        p2x = x;
        p2y = y;

        fillPathLinear(p1x, p1y, p2x, p2y);
    }

    private void pushPoint(float x, float y) {
        px[head] = x;
        py[head] = y;
        pTime[head] = currentTime;
        head = (head + 1) % TRAIL_CAPACITY;
        if (count < TRAIL_CAPACITY) count++;
    }

    private void fillPathLinear(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = (int) (distance / TRAIL_STEP_SIZE);

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

        float currentLifetime = TRAIL_LIFETIME * GameHelper.getSpeedMultiplier();

        // ROTATION CHECK
        if (OsuSkin.get().isRotateCursorTrail()) {
            stamp.setRotation(cursor.getRotation());
        } else {
            stamp.setRotation(0f);
        }

        // DRAW & PRUNE LOOP
        int newCount = 0;
        for (int i = 0; i < count; i++) {
            int idx = (head - 1 - i);
            if (idx < 0) idx += TRAIL_CAPACITY;

            float age = currentTime - pTime[idx];
            if (age > currentLifetime) {
                // Since points are ordered by age, once we hit one too old,
                // all subsequent points in the loop are also too old.
                break;
            }
            newCount++;

            float fadeLifeRatio = Math.max(0f, 1f - (age / (currentLifetime * FADE_DURATION_RATIO)));
            float scaleLifeRatio = Math.max(0f, 1f - (age / (currentLifetime * SCALE_DURATION_RATIO)));

            stamp.setPosition(px[idx] + offsetX, py[idx] + offsetY);
            stamp.setScale(baseSize * scaleLifeRatio);
            stamp.setAlpha(MAX_OPACITY * fadeLifeRatio);

            stamp.drawNow(pGL, pCamera);
        }

        // Update count for the next frame so old points eventually disappear
        this.count = newCount;
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
