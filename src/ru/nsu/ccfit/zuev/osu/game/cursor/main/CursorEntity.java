package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.cursor.trail.CursorTrail;

public class CursorEntity extends Entity {
    protected final CursorSprite cursorSprite;
    private CursorTrail trail = null;
    private boolean isShowing = false;

    public CursorEntity() {
        TextureRegion cursorTex = ResourceManager.getInstance().getTexture("cursor");
        cursorSprite = new CursorSprite(-cursorTex.getWidth() / 2f, -cursorTex.getWidth() / 2f, cursorTex);

        if (Config.isUseParticles()) {
            TextureRegion trailTex = ResourceManager.getInstance().getTexture("cursortrail");

            // Allow a capacity of 200 for a long, gapless tail
            trail = new CursorTrail(new PointParticleEmitter(0, 0), 200, trailTex, cursorSprite);
        }

        attachChild(cursorSprite);
        setVisible(false);
        setIgnoreUpdate(true);
    }

    public void setShowing(boolean showing) {
        isShowing = showing;
        setVisible(showing);
        if (trail != null) trail.setParticlesSpawnEnabled(showing);
    }

    public void click() {
        cursorSprite.handleClick();
    }

    public void update(float pSecondsElapsed) {
        if (isShowing) {
            cursorSprite.update(pSecondsElapsed);
            if (trail != null) {
                // Feeds current coordinates into the Bresenham logic
                trail.update(getX(), getY());
            }
        }
        super.onManagedUpdate(pSecondsElapsed);
    }

    public void attachToScene(Scene fgScene) {
        if (trail != null) fgScene.attachChild(trail);
        fgScene.attachChild(this);
    }

    @Override
    public void setPosition(float pX, float pY) {
        super.setPosition(pX, pY);
    }
}
