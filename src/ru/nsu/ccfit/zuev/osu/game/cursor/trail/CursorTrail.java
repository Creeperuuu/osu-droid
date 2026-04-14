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

    public CursorTrail(PointParticleEmitter emitter, int spawnRate, TextureRegion pTextureRegion, CursorSprite cursor) {
        super(emitter, spawnRate, spawnRate, spawnRate, pTextureRegion);
        this.cursor = cursor;

        // A longer life makes the ribbon stay on screen
        float lifeTime = 0.6f * GameHelper.getSpeedMultiplier();
        addParticleModifier(new ExpireModifier(lifeTime));

        // Smooth alpha fade
        addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0f, lifeTime));

        // Use GL_ONE for additive blending (makes the ribbon look solid and glowing)
        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
        addParticleInitializer(new ScaleInitializer(cursor.baseSize));

        // We will control spawning manually for the "bridge" effect
        setParticlesSpawnEnabled(false);
    }

    public void update() {
        updateRotation();
    }

    private void updateRotation() {
        if (OsuSkin.get().isRotateCursorTrail()) {
            setRotation(cursor.getRotation());
        }
    }
}
