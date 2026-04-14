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

    // How many intermediate emitter steps to simulate per frame
    private static final int INTERPOLATION_STEPS = 6;

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
        addParticleModifier(new AlphaModifier(life, 1.0f, 0.0f, 0f, life));

        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        addParticleInitializer(new ScaleInitializer(cursor.baseSize));
        setParticlesSpawnEnabled(false);
        updateRotation();
    }

    /**
     * Call every frame with the cursor's current world position. Interpolates emitter position
     * between last and current to fill gaps.
     */
    public void update(float x, float y) {
        if (Float.isNaN(lastX)) {
            lastX = x;
            lastY = y;
            emitter.setCenter(x, y);
            updateRotation();
            return;
        }

        float dx = x - lastX;
        float dy = y - lastY;

        // Move the emitter in small steps between last and current position
        // so particles are spawned along the full path, not just at the endpoint
        for (int i = 1; i <= INTERPOLATION_STEPS; i++) {
            float t = (float) i / INTERPOLATION_STEPS;
            float ix = lastX + dx * t;
            float iy = lastY + dy * t;
            emitter.setCenter(ix, iy);
        }

        lastX = x;
        lastY = y;

        updateRotation();
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        super.setParticlesSpawnEnabled(enabled);
        if (!enabled) {
            lastX = Float.NaN;
            lastY = Float.NaN;
        }
    }

    private void updateRotation() {
        if (OsuSkin.get().isRotateCursorTrail()) {
            setRotation(cursor.getRotation());
        }
    }
}