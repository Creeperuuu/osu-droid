package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.LinkedList;

import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorTrail extends Entity {

    // ── Tune these ────────────────────────────────────────────────────────
    private static final int SAMPLE_SIZE = 5; // Bresenham sample rate (every N steps)
    private static final int POOL_SIZE = 256; // max sprites in pool
    private static final float SPRITE_SCALE = 1.0f;

    // ─────────────────────────────────────────────────────────────────────

    private static final class Point {
        float x, y;

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private final LinkedList<Point> trail = new LinkedList<>();
    private final LinkedList<Point> freePool = new LinkedList<>(); // recycled points

    private final Sprite[] spritePool;
    private final float halfW;
    private final float halfH;
    private final float scale;
    private final CursorSprite cursor;
    private final TextureRegion texture;

    private boolean spawning = false;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float currentFPS = 60f;

    public CursorTrail(TextureRegion texture, CursorSprite cursor) {
        this.texture = texture;
        this.cursor = cursor;

        scale = cursor.baseSize * SPRITE_SCALE;
        halfW = (texture.getWidth() * scale) / 2f;
        halfH = (texture.getHeight() * scale) / 2f;

        spritePool = new Sprite[POOL_SIZE];
        for (int i = 0; i < POOL_SIZE; i++) {
            Sprite s = new Sprite(0, 0, texture);
            s.setScale(scale);
            s.setAlpha(0f);
            spritePool[i] = s;
        }
    }

    public void attachToScene(Scene scene) {
        for (int i = 0; i < POOL_SIZE; i++) {
            scene.attachChild(spritePool[i]);
        }
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        spawning = enabled;
        if (!enabled) {
            trail.clear();
            lastX = Float.NaN;
            lastY = Float.NaN;
            for (Sprite s : spritePool) s.setAlpha(0f);
        }
    }

    public void update(float x, float y, float dt) {
        // track FPS for remove rate
        if (dt > 0) currentFPS = 1f / dt;

        if (!spawning) return;

        if (Float.isNaN(lastX)) {
            lastX = x;
            lastY = y;
            return;
        }

        // 1. Add all points between last and current position via Bresenham
        addCursorPoints((int) lastX, (int) lastY, (int) x, (int) y);
        lastX = x;
        lastY = y;

        // 2. Remove points from tail — proportional to trail size, scaled by FPS
        //    Same formula as opsu: size / (6 * FPSmod) + 1
        float FPSmod = Math.max(currentFPS, 1) / 60f;
        int removeCount = (int) (trail.size() / (6f * FPSmod)) + 1;
        for (int i = 0; i < removeCount && !trail.isEmpty(); i++) {
            trail.removeFirst();
        }

        // 3. Render
        refreshSprites(x, y);
        updateRotation();
    }

    /**
     * Bresenham's line — adds one point every SAMPLE_SIZE steps. Identical logic to opsu's
     * addCursorPoints().
     */
    private void addCursorPoints(int x1, int y1, int x2, int y2) {
        int d = 0;
        int dy = Math.abs(y2 - y1);
        int dx = Math.abs(x2 - x1);
        int dy2 = dy << 1;
        int dx2 = dx << 1;
        int ix = x1 < x2 ? 1 : -1;
        int iy = y1 < y2 ? 1 : -1;

        if (dy <= dx) {
            for (int i = 0; ; i++) {
                if (i == SAMPLE_SIZE) {
                    trail.add(new Point(x1, y1));
                    i = 0;
                }
                if (x1 == x2) break;
                x1 += ix;
                d += dy2;
                if (d > dx) {
                    y1 += iy;
                    d -= dx2;
                }
            }
        } else {
            for (int i = 0; ; i++) {
                if (i == SAMPLE_SIZE) {
                    trail.add(new Point(x1, y1));
                    i = 0;
                }
                if (y1 == y2) break;
                y1 += iy;
                d += dx2;
                if (d > dy) {
                    x1 += ix;
                    d -= dy2;
                }
            }
        }
    }

    private void refreshSprites(float curX, float curY) {
        int size = trail.size();

        // alpha step — same as opsu: 2f / size so it spans 0→1 across the trail
        float alphaStep = size > 0 ? 2f / size : 0f;
        float alpha = 0f;

        int i = 0;
        for (Point p : trail) {
            if (i >= POOL_SIZE) break;
            alpha += alphaStep;
            Sprite s = spritePool[i];
            s.setPosition(p.x - halfW, p.y - halfH);
            s.setAlpha(Math.min(alpha, 1f));
            i++;
        }

        // draw one extra sprite at current cursor position (opsu does this too)
        if (i < POOL_SIZE) {
            Sprite s = spritePool[i];
            s.setPosition(curX - halfW, curY - halfH);
            s.setAlpha(1f);
            i++;
        }

        // hide all unused sprites
        for (; i < POOL_SIZE; i++) {
            if (spritePool[i].getAlpha() != 0f)
                spritePool[i].setAlpha(0f);
        }
    }

    private void updateRotation() {
        if (OsuSkin.get().isRotateCursorTrail()) {
            float rot = cursor.getRotation();
            for (int i = 0; i < POOL_SIZE; i++) {
                if (spritePool[i].getAlpha() > 0f)
                    spritePool[i].setRotation(rot);
            }
        }
    }
}