package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;

public class CursorTrail extends Entity {

    // ── Tuning Parameters ────────────────────────────────────────────────
    private static final int TRAIL_CAPACITY = 2048; 
    
    // Trail timings (in seconds)
    private static final float FADE_LIFETIME  = 0.35f; 
    private static final float SCALE_LIFETIME = 0.5f; 
    
    // THE GLOW CONTROL: 
    // 1.0f = Instant bright white (your original issue)
    // 0.3f = Soft, colorful glow that gets brighter in the center
    // 0.15f = Very faint, deep color, rarely hits pure white
    private static final float ADDITIVE_BUMP  = 0.25f;  
    
    private static final float MAX_LIFETIME = Math.max(FADE_LIFETIME, SCALE_LIFETIME);
    // ─────────────────────────────────────────────────────────────────────

    private final StampSprite stamp;
    private final float[] px = new float[TRAIL_CAPACITY];
    private final float[] py = new float[TRAIL_CAPACITY];
    private final float[] pTime = new float[TRAIL_CAPACITY];
    
    private int head = 0;
    private int count = 0;
    private boolean spawning = false;
    private float currentTime = 0f;

    // History for Spline Interpolation
    private float p0x = Float.NaN, p0y = Float.NaN;
    private float p1x = Float.NaN, p1y = Float.NaN;
    private float p2x = Float.NaN, p2y = Float.NaN;

    private final float offsetX;
    private final float offsetY;
    private final float baseSize;

    public CursorTrail(TextureRegion trailTex, CursorSprite cursor) {
        stamp = new StampSprite(0, 0, trailTex);
        
        // Back to Additive Blending to get that luminescent glow back!
        stamp.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
        
        this.baseSize = cursor.baseSize;
        offsetX = -trailTex.getWidth() / 2f;
        offsetY = -trailTex.getHeight() / 2f;
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        spawning = enabled;
        if (!enabled) {
            head = 0; count = 0;
            p0x = Float.NaN; p0y = Float.NaN;
            p1x = Float.NaN; p1y = Float.NaN;
            p2x = Float.NaN; p2y = Float.NaN;
        }
    }

    public void update(float x, float y, float dt) {
        currentTime += dt;
        if (!spawning) return;

        if (Float.isNaN(p2x)) {
            p2x = x; p2y = y;
            pushPoint(x, y);
            return;
        }

        p0x = p1x; p0y = p1y;
        p1x = p2x; p1y = p2y;
        p2x = x;   p2y = y;

        if (Float.isNaN(p0x)) {
            fillPathLinear(p1x, p1y, p2x, p2y);
            return;
        }

        float p3x = p2x + (p2x - p1x);
        float p3y = p2y + (p2y - p1y);
        fillPathCatmullRom(p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y);
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
        float stepSize = 1.5f; 
        int steps = (int) (distance / stepSize);
        if (steps <= 0) { pushPoint(x2, y2); return; }
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            pushPoint(x1 + dx * t, y1 + dy * t);
        }
    }

    private void fillPathCatmullRom(float p0x, float p0y, float p1x, float p1y, float p2x, float p2y, float p3x, float p3y) {
        float dx = p2x - p1x, dy = p2y - p1y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float stepSize = 1.5f; 
        int steps = (int) (distance / stepSize);
        if (steps <= 0) { pushPoint(p2x, p2y); return; }

        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            float t2 = t * t, t3 = t2 * t;
            float outX = 0.5f * ((2.0f * p1x) + (-p0x + p2x) * t + (2.0f * p0x - 5.0f * p1x + 4.0f * p2x - p3x) * t2 + (-p0x + 3.0f * p1x - 3.0f * p2x + p3x) * t3);
            float outY = 0.5f * ((2.0f * p1y) + (-p0y + p2y) * t + (2.0f * p0y - 5.0f * p1y + 4.0f * p2y - p3y) * t2 + (-p0y + 3.0f * p1y - 3.0f * p2y + p3y) * t3);
            pushPoint(outX, outY);
        }
    }

    @Override
    protected void onManagedDraw(GL10 pGL, Camera pCamera) {
        if (count == 0) return;

        int activeCount = 0;
        for (int i = 0; i < count; i++) {
            int idx = (head - 1 - i);
            if (idx < 0) idx += TRAIL_CAPACITY;
            if (currentTime - pTime[idx] > MAX_LIFETIME) break;
            activeCount++;
        }
        count = activeCount;

        for (int i = count - 1; i >= 0; i--) {
            int idx = (head - 1 - i);
            if (idx < 0) idx += TRAIL_CAPACITY;

            float age = currentTime - pTime[idx];
            
            float fadeRatio = Math.max(0f, 1f - (age / FADE_LIFETIME));
            float scaleRatio = Math.max(0f, 1f - (age / SCALE_LIFETIME));

            stamp.setPosition(px[idx] + offsetX, py[idx] + offsetY);
            
            // This is the magic. By multiplying the fade by ADDITIVE_BUMP, 
            // the stamps start much darker. It takes several overlapping stamps
            // to "add" up to pure white, giving you the color back while keeping the glow.
            stamp.setAlpha(fadeRatio * ADDITIVE_BUMP); 
            stamp.setScale(baseSize * scaleRatio); 

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
