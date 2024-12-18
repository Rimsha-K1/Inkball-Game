package inkball;

import java.awt.Point;
import java.util.ArrayList;
import processing.core.PApplet;
/**
 * Represents a drawable line that can interact with balls and handle mouse input.
 * Implements collision detection and reflection physics.
 */
public class Line implements Draw {
    private static final float THICKNESS = 10.0f;
    private static final float HALF_THICKNESS = THICKNESS / 2;
    /** Collection of points forming the line */
    private final ArrayList<Point> points;
    /** Reusable vector for calculations */
    private final Vector2D tempVector;
    /**
     * Utility class for 2D vector operations
     */
    private static class Vector2D {
        float x, y;

        Vector2D() {
            this.x = 0;
            this.y = 0;
        }

        void set(float x, float y) {
            this.x = x;
            this.y = y;
        }
        /** Normalizes vector to unit length */
        void normalize() {
            float length = (float) Math.sqrt(x * x + y * y);
            if (length > 0) {
                x /= length;
                y /= length;
            }
        }
        /** Calculates dot product with given vector */
        float dot(float ox, float oy) {
            return x * ox + y * oy;
        }
    }
    /** Creates new empty line */
    public Line() {
        points = new ArrayList<>();
        tempVector = new Vector2D();
    }
    /** Adds point to line at specified coordinates */
    public void addPoint(float x, float y) {
        points.add(new Point((int) x, (int) y));
    }
    /** Renders line on screen */
    public void draw(App app) {
        if (points.size() < 2) return;

        app.strokeWeight(THICKNESS);
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            app.line(p1.x, p1.y, p2.x, p2.y);
        }
    }

    public boolean mouseNearLine(float mouseX, float mouseY) {
        return checkPointNearLine(mouseX, mouseY, HALF_THICKNESS);
    }
    /**
     * Handles ball collision with line
     * @return true if collision occurred
     */
    public boolean handleCollision(Ball ball) {
        float nextX = ball.getX() + ball.getVx();
        float nextY = ball.getY() + ball.getVy();

        for (int i = 0; i < points.size() - 1; i++) {
            if (checkCollisionWithSegment(ball, nextX, nextY, i)) {
                return true;
            }
        }
        return false;
    }
    /** Checks collision with specific line segment */
    private boolean checkCollisionWithSegment(Ball ball, float nextX, float nextY, int segmentIndex) {
        Point p1 = points.get(segmentIndex);
        Point p2 = points.get(segmentIndex + 1);

        if (ballNearSegment(nextX, nextY, p1.x, p1.y, p2.x, p2.y)) {
            calculateReflection(ball, p1, p2);
            return true;
        }
        return false;
    }

    private void calculateReflection(Ball ball, Point p1, Point p2) {
        tempVector.set(p2.x - p1.x, p2.y - p1.y);
        float normalX = -tempVector.y;
        float normalY = tempVector.x;

        tempVector.set(normalX, normalY);
        tempVector.normalize();

        float ballVx = ball.getVx();
        float ballVy = ball.getVy();
        float dotProduct = tempVector.dot(ballVx, ballVy);

        ball.setVx(ballVx - 2 * dotProduct * tempVector.x);
        ball.setVy(ballVy - 2 * dotProduct * tempVector.y);
    }
    /** Utility methods for distance calculations */
    private boolean checkPointNearLine(float px, float py, float threshold) {
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            if (distanceToSegment(px, py, p1.x, p1.y, p2.x, p2.y) <= threshold) {
                return true;
            }
        }
        return false;
    }

    private boolean ballNearSegment(float px, float py, float x1, float y1, float x2, float y2) {
        return distanceToSegment(px, py, x1, y1, x2, y2) <= HALF_THICKNESS + Ball.radius;
    }

    private float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float segmentLengthSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);

        if (segmentLengthSquared == 0) {
            return dist(px, py, x1, y1);
        }

        float t = Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / segmentLengthSquared));
        float projectionX = x1 + t * (x2 - x1);
        float projectionY = y1 + t * (y2 - y1);

        return dist(px, py, projectionX, projectionY);
    }

    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public ArrayList<Point> getPoints() {
        return points;
    }
}
