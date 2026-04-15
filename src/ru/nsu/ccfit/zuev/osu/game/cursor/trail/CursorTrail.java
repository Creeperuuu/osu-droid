package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.ScaleInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;

public class CursorTrail extends ParticleSystem {

    // ── Tune these ────────────────────────────────────────────────────────
    private static final int SAMPLE_SIZE = 3; // Pixels between texture stamps
    // ─────────────────────────────────────────────────────────────────────

    private final PointParticleEmitter emitter;
    private final CursorSprite cursor;

    private boolean spawning = false;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    
    private final float offsetX;
    private final float offsetY;

    public CursorTrail(
            PointParticleEmitter emitter,
            int maxCapacity,
            TextureRegion pTextureRegion,
            CursorSprite cursor
    ) {
        // maxCapacity allows us to keep a long tail without overflowing
        super(emitter, maxCapacity, maxCapacity, maxCapacity, pTextureRegion);
        
        this.emitter = emitter;
        this.cursor = cursor;

        this.offsetX = -pTextureRegion.getWidth() / 2f;
        this.offsetY = -pTextureRegion.getHeight() / 2f;

        // Tune lifeTime to make the trail longer or shorter
        float lifeTime = 0.6f;
        addParticleModifier(new ExpireModifier(lifeTime * GameHelper.getSpeedMultiplier()));
        addParticleModifier(new AlphaModifier(GameHelper.getSpeedMultiplier(), 0.0f, 0f, lifeTime));

        // Additive blending for the continuous ribbon glow
        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
        addParticleInitializer(new ScaleInitializer(cursor.baseSize));

        // Disable auto-spawn; we will force spawn exactly where we want via Bresenham
        setParticlesSpawnEnabled(false);
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        spawning = enabled;
        if (!enabled) {
            lastX = Float.NaN;
            lastY = Float.NaN;
        }
    }

    public void update(float x, float y) {
        if (!spawning) return;

        if (Float.isNaN(lastX)) {
            spawnAt(x, y);
            lastX = x;
            lastY = y;
            return;
        }

        // Fill the path smoothly
        addCursorPoints((int) lastX, (int) lastY, (int) x, (int) y);
        
        lastX = x;
        lastY = y;
    }

    private void spawnAt(float x, float y) {
        // Move the emitter exactly to the Bresenham point
        emitter.setCenter(x + offsetX, y + offsetY);
        // This is the trick: force the ParticleSystem to push 1 stamp into the batch immediately
        super.onManagedUpdate(0.001f); 
    }

    private void addCursorPoints(int x1, int y1, int x2, int y2) {
        int d=0, dy=Math.abs(y2-y1), dx=Math.abs(x2-x1);
        int dy2=dy<<1, dx2=dx<<1;
        int ix=x1<x2?1:-1, iy=y1<y2?1:-1;

        if (dy <= dx) {
            for (int i=0;;i++) {
                if (i == SAMPLE_SIZE) { spawnAt(x1, y1); i=0; }
                if (x1 == x2) break;
                x1+=ix; d+=dy2;
                if (d > dx) { y1+=iy; d-=dx2; }
            }
        } else {
            for (int i=0;;i++) {
                if (i == SAMPLE_SIZE) { spawnAt(x1, y1); i=0; }
                if (y1 == y2) break;
                y1+=iy; d+=dx2;
                if (d > dy) { x1+=ix; d-=dy2; }
            }
        }
    }
}
