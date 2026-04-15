package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.primitive.Line;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;

public class CursorTrail extends Entity {

    // ── Tune these ────────────────────────────────────────────────────────
    private static final int TRAIL_CAPACITY = 64; // number of line segments
    private static final int SAMPLE_SIZE = 3; // Bresenham: 1 point per N steps
    private static final float LINE_WIDTH = 8f; // thickness of the trail line
    // ─────────────────────────────────────────────────────────────────────

    private final Line[] lines;

    // Ring buffer of recorded positions
    private final float[] px = new float[TRAIL_CAPACITY + 1];
    private final float[] py = new float[TRAIL_CAPACITY + 1];
    private int head = 0;
    private int count = 0;

    private boolean spawning = false;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float currentFPS = 60f;

    public CursorTrail(CursorSprite cursor) {
        lines = new Line[TRAIL_CAPACITY];
        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            Line l = new Line(0, 0, 0, 0);
            l.setLineWidth(LINE_WIDTH * cursor.baseSize);
            l.setAlpha(0f);
            lines[i] = l;
        }
    }

    public void attachToScene(Scene scene) {
        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            scene.attachChild(lines[i]);
        }
    }

    public void setParticlesSpawnEnabled(boolean enabled) {
        spawning = enabled;
        if (!enabled) {
            head = 0;
            count = 0;
            lastX = Float.NaN;
            lastY = Float.NaN;
            for (Line l : lines) l.setAlpha(0f);
        }
    }

    public void update(float x, float y, float dt) {
        if (dt > 0) currentFPS = 1f / dt;
        if (!spawning) return;

        if (Float.isNaN(lastX)) {
            pushPoint(x, y);
            lastX = x;
            lastY = y;
            return;
        }

        // Fill all points along the path using Bresenham
        addCursorPoints((int) lastX, (int) lastY, (int) x, (int) y);
        lastX = x;
        lastY = y;

        // Remove from tail proportional to size — same formula as opsu
        if (count > 2) {
            float FPSmod = Math.max(currentFPS, 1f) / 60f;
            int removeCount = Math.min((int) (count / (6f * FPSmod)) + 1, count - 2);
            count -= removeCount;
        }

        refreshLines();
    }

    private void pushPoint(float x, float y) {
        px[head % (TRAIL_CAPACITY + 1)] = x;
        py[head % (TRAIL_CAPACITY + 1)] = y;
        head++;
        if (count < TRAIL_CAPACITY + 1) count++;
    }

    private void refreshLines() {
        // number of segments = number of points - 1
        int segments = Math.min(count - 1, TRAIL_CAPACITY);
        int start = head - count; // oldest point offset

        float alphaStep = segments > 0 ? 1f / segments : 0f;

        for (int i = 0; i < TRAIL_CAPACITY; i++) {
            Line l = lines[i];
            if (i >= segments) {
                if (l.getAlpha() != 0f) l.setAlpha(0f);
                continue;
            }

            int idxA = (start + i) & Integer.MAX_VALUE;
            int idxB = (start + i + 1) & Integer.MAX_VALUE;
            float ax = px[idxA % (TRAIL_CAPACITY + 1)];
            float ay = py[idxA % (TRAIL_CAPACITY + 1)];
            float bx = px[idxB % (TRAIL_CAPACITY + 1)];
            float by = py[idxB % (TRAIL_CAPACITY + 1)];

            l.setPosition(ax, ay, bx, by);
            l.setAlpha((i + 1) * alphaStep);
        }
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
}