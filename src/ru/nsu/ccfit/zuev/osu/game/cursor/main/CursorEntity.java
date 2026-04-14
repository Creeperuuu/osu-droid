package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.cursor.trail.CursorTrail;

public class CursorEntity extends Entity {
    protected final CursorSprite cursorSprite;
    private CursorTrail trail = null;
    private PointParticleEmitter emitter = null;
    private boolean isShowing = false;
    private float particleOffsetX, particleOffsetY;
    private float lastX = -1, lastY = -1;

    public CursorEntity() {
        TextureRegion cursorTex = ResourceManager.getInstance().getTexture("cursor");
        cursorSprite = new CursorSprite(-cursorTex.getWidth() / 2f, -cursorTex.getWidth() / 2f, cursorTex);

        if (Config.isUseParticles()) {
            TextureRegion trailTex = ResourceManager.getInstance().getTexture("cursortrail");

            particleOffsetX = -trailTex.getWidth() / 2f;
            particleOffsetY = -trailTex.getHeight() / 2f;

            var spawnRate = (int) (GlobalManager.getInstance().getMainActivity().getRefreshRate() * 2);

            emitter = new PointParticleEmitter(particleOffsetX, particleOffsetY);
            trail = new CursorTrail(trailTex, cursorSprite); // ← no emitter / spawnRate args
            trail.setParticlesSpawnEnabled(false);
        }

        attachChild(cursorSprite);
        setVisible(false);

        // Not necessary to update by itself since it's done by GameScene.
        setIgnoreUpdate(true);
    }

    public void setShowing(boolean showing) {
        isShowing = showing;
        setVisible(showing);
        if (trail != null)
            trail.setParticlesSpawnEnabled(showing);
    }

    public void click() {
        cursorSprite.handleClick();
    }

    public void update(float pSecondsElapsed) {
        if (isShowing && trail != null) {
            float curX = getX();
            float curY = getY();

            // If this is the first frame, just snap to position
            if (lastX == -1) {
                lastX = curX;
                lastY = curY;
            }

            // Calculate distance moved since last frame
            float dx = curX - lastX;
            float dy = curY - lastY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            // BRIDGE THE GAP:
            // If the cursor moved more than 5 pixels, fill the space with extra particles
            float stepSize = 5.0f; // Lower = smoother but heavier on CPU
            if (distance > stepSize) {
                int extraParticles = (int) (distance / stepSize);

                for (int i = 1; i <= extraParticles; i++) {
                    float ratio = (float) i / extraParticles;
                    float interpX = lastX + (dx * ratio);
                    float interpY = lastY + (dy * ratio);

                    // Move emitter to the interpolated spot and force a particle out
                    emitter.setCenter(interpX + particleOffsetX, interpY + particleOffsetY);

                    // This triggers the ParticleSystem to spawn 1 particle immediately
                    trail.onManagedUpdate(0.001f);
                }
            }

            // Always update the "current" position for the sprite
            lastX = curX;
            lastY = curY;
        }

        if (isShowing) cursorSprite.update(pSecondsElapsed);
        super.onManagedUpdate(pSecondsElapsed);
    }

    public void attachToScene(Scene fgScene) {
        if (trail != null) {
            fgScene.attachChild(trail);
        }
        fgScene.attachChild(this);
    }

    @Override
    public void setPosition(float pX, float pY) {
        super.setPosition(pX, pY);
    }
}
