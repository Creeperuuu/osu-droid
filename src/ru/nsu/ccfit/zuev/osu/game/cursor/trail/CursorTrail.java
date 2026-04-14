package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorTrail extends Sprite {

    // ── Tune these ────────────────────────────────────────────────────────
    private static final int TRAIL_CAPACITY = 128;
    private static final int SAMPLE_SIZE = 8;
    private static final float SPRITE_SCALE = 1.0f;
    // ─────────────────────────────────────────────────────────────────────

    private final float[] trailX;
    private final float[] trailY;

    private int head = 0;
    private int tail = 0;
    private int count = 0;

    // unscaled offset to center the sprite on the cursor position
    private final float offsetX;
    private final float offsetY;
    private final float drawW;
    private final float drawH;
    private final float scale;
    private final CursorSprite cursor;

    private boolean spawning = false;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float currentFPS = 60f;

    public CursorTrail(TextureRegion texture, CursorSprite cursor) {
        // Sprite at 0,0 — we only use its texture/batch capability, not its position
        super(0, 0, texture);
        this.cursor = cursor;

        scale = cursor.baseSize * SPRITE_SCALE;
        drawW = texture.getWidth() * scale;
        drawH = texture.getHeight() * scale;
        offsetX = drawW / 2f;
        offsetY = drawH / 2f;

        trailX = new float[TRAIL_CAPACITY];
        trailY = new float[TRAIL_CAPACITY];

        setVisible(false); // hide the base sprite — we draw manually
    }

    public void attachToScene(Scene scene) {
        scene.attachChild(this); // only ONE scene child
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        spawning = enabled;
        if (!enabled) {
            head = 0;
            tail = 0;
            count = 0;
            lastX = Float.NaN;
            lastY = Float.NaN;
        }
        setVisible(enabled);
    }

    public void update(float x, float y, float dt) {
        if (dt > 0) currentFPS = 1f / dt;
        if (!spawning) return;

        if (Float.isNaN(lastX)) {
            lastX = x;
            lastY = y;
            return;
        }

        addCursorPoints((int) lastX, (int) lastY, (int) x, (int) y);
        lastX = x;
        lastY = y;

        // remove from tail — skip when trail is short so slow movement stays visible
        if (count > 8) {
            float FPSmod = Math.max(currentFPS, 1) / 60f;
            int removeCount = Math.min((int) (count / (6f * FPSmod)) + 1, count / 2);
            for (int i = 0; i < removeCount; i++) {
                tail = (tail + 1) % TRAIL_CAPACITY;
                count--;
            }
        }
    }

    // ── AndEngine calls this once per frame to draw this entity ──────────
    @Override
    protected void onManagedDraw(
            final javax.microedition.khronos.opengles.GL10 pGL,
            final org.anddev.andengine.engine.camera.Camera pCamera) {

        if (count == 0) return;

        float alphaStep = 1f / count;
        float alpha = 0f;
        float rotation = OsuSkin.get().isRotateCursorTrail() ? cursor.getRotation() : 0f;

        // startUse/drawEmbedded/endUse = single GL batch for all trail points
        getTextureRegion().getTexture().bind(pGL);
        this.mTextureRegion.getTexture().bind(pGL);

        beginDraw(pGL);
        for (int i = 0; i < count; i++) {
            int idx = (tail + i) % TRAIL_CAPACITY;
            alpha = Math.min((i + 1) * alphaStep, 1f);
            drawPoint(pGL, trailX[idx], trailY[idx], alpha, rotation);
        }
        endDraw(pGL);
    }

    private void beginDraw(javax.microedition.khronos.opengles.GL10 gl) {
        gl.glEnable(javax.microedition.khronos.opengles.GL10.GL_BLEND);
        gl.glBlendFunc(
                javax.microedition.khronos.opengles.GL10.GL_SRC_ALPHA,
                javax.microedition.khronos.opengles.GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(javax.microedition.khronos.opengles.GL10.GL_TEXTURE_2D);
    }

    private void endDraw(javax.microedition.khronos.opengles.GL10 gl) {
        // no-op — AndEngine restores state after onManagedDraw
    }

    private void drawPoint(
            javax.microedition.khronos.opengles.GL10 gl,
            float x, float y, float alpha, float rotation) {
        gl.glPushMatrix();
        gl.glTranslatef(x, y, 0);
        if (rotation != 0f) gl.glRotatef(rotation, 0, 0, 1);
        gl.glColor4f(1f, 1f, 1f, alpha);

        float l = -offsetX, t = -offsetY, r = offsetX, b = offsetY;

        final float[] verts = {l, t, r, t, l, b, r, b};
        final float[] tex = {0, 0, 1, 0, 0, 1, 1, 1};

        java.nio.FloatBuffer vb = java.nio.ByteBuffer.allocateDirect(32)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        vb.put(verts).position(0);

        java.nio.FloatBuffer tb = java.nio.ByteBuffer.allocateDirect(32)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        tb.put(tex).position(0);

        gl.glEnableClientState(javax.microedition.khronos.opengles.GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(javax.microedition.khronos.opengles.GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glVertexPointer(2, javax.microedition.khronos.opengles.GL10.GL_FLOAT, 0, vb);
        gl.glTexCoordPointer(2, javax.microedition.khronos.opengles.GL10.GL_FLOAT, 0, tb);
        gl.glDrawArrays(javax.microedition.khronos.opengles.GL10.GL_TRIANGLE_STRIP, 0, 4);
        gl.glDisableClientState(javax.microedition.khronos.opengles.GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(javax.microedition.khronos.opengles.GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glPopMatrix();
    }

    private void addCursorPoints(int x1, int y1, int x2, int y2) {
        int d = 0, dy = Math.abs(y2 - y1), dx = Math.abs(x2 - x1);
        int dy2 = dy << 1, dx2 = dx << 1;
        int ix = x1 < x2 ? 1 : -1, iy = y1 < y2 ? 1 : -1;

        if (dy <= dx) {
            for (int i = 0; ; i++) {
                if (i == SAMPLE_SIZE) {
                    pushPoint(x1, y1);
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
                    pushPoint(x1, y1);
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

    private void pushPoint(float x, float y) {
        if (count == TRAIL_CAPACITY) {
            tail = (tail + 1) % TRAIL_CAPACITY;
            count--;
        }
        trailX[head] = x;
        trailY[head] = y;
        head = (head + 1) % TRAIL_CAPACITY;
        count++;
    }
}