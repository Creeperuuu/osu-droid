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
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorTrail extends ParticleSystem {
    private final CursorSprite cursor;
    private final PointParticleEmitter emitter;

    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float targetX = Float.NaN;
    private float targetY = Float.NaN;

    private boolean isSpawning = false;

    // Defines how many pixels between particle spawns.
    // Lower = smoother but higher object count. 12f is an optimal balance for osu!
    private static final float TRAIL_DENSITY = 12f;

    public CursorTrail(
            PointParticleEmitter emitter,
            int spawnRate,
            TextureRegion pTextureRegion,
            CursorSprite cursor) {
        super(emitter, spawnRate, spawnRate, spawnRate * 2, pTextureRegion);

        this.cursor = cursor;
        this.emitter = emitter;

        float life = 0.3f * GameHelper.getSpeedMultiplier();

        addParticleModifier(new ExpireModifier(life));
        addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0f, life));

        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        addParticleInitializer(new ScaleInitializer(cursor.baseSize));
        setParticlesSpawnEnabled(false);
        updateRotation();
    }

    /**
     * Call every frame with the cursor's current world position. We now strictly queue the target
     * position instead of jumping the emitter.
     */
    public void update(float x, float y) {
        this.targetX = x;
        this.targetY = y;

        if (Float.isNaN(lastX)) {
            this.lastX = x;
            this.lastY = y;
            emitter.setCenter(x, y);
        }

        updateRotation();
    }

    /**
     * Intercepts the engine's internal update tick. Slices the frame into smaller steps based on
     * velocity, moving the emitter during the emission phase to completely eliminate gaps.
     */
    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {
        if (!this.isSpawning || Float.isNaN(lastX) || Float.isNaN(targetX)) {
            super.onManagedUpdate(pSecondsElapsed);
            return;
        }

        float dx = targetX - lastX;
        float dy = targetY - lastY;
        float distance = (float) Math.hypot(dx, dy);

        // THE FIX: If the cursor hasn't moved, pause emission.
        // This allows you to use a massive spawnRate (like 1500) for fast flicks
        // without spawning 1500 particles per second when the cursor is standing still.
        if (distance < 2.0f) {
            super.setParticlesSpawnEnabled(false);
            super.onManagedUpdate(pSecondsElapsed);

            // Restore the intended spawning state for the next frame
            super.setParticlesSpawnEnabled(this.isSpawning);
            return;
        }

        // Ensure emission is on while we process movement
        super.setParticlesSpawnEnabled(this.isSpawning);

        int steps = (int) Math.max(1, distance / TRAIL_DENSITY);
        float stepTime = pSecondsElapsed / steps;

        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            float ix = lastX + dx * t;
            float iy = lastY + dy * t;

            emitter.setCenter(ix, iy);
            super.onManagedUpdate(stepTime);
        }

        this.lastX = targetX;
        this.lastY = targetY;
    }

    @Override
    public void setParticlesSpawnEnabled(boolean enabled) {
        super.setParticlesSpawnEnabled(enabled);
        this.isSpawning = enabled;

        if (!enabled) {
            this.lastX = Float.NaN;
            this.lastY = Float.NaN;
            this.targetX = Float.NaN;
            this.targetY = Float.NaN;
        }
    }

    private void updateRotation() {
        if (OsuSkin.get().isRotateCursorTrail()) {
            setRotation(cursor.getRotation());
        }
    }
}
