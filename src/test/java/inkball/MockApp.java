package inkball;

import processing.core.PImage;
/**
 * Mock implementation of App class for testing rendering logic without actual graphics.
 * Tracks drawing operations and stores coordinates for verification.
 */
public class MockApp extends App {
    /** Counter for number of drawing operations performed */
    private int drawCalls = 0;
    /** Last x-coordinate passed to drawing operation */
    private float lastDrawX = 0;
    /** Last y-coordinate passed to drawing operation */
    private float lastDrawY = 0;

    @Override

    /**
     * Simulates image drawing operation by recording parameters
     * @param img Image to draw (ignored in mock)
     * @param x X-coordinate of draw operation
     * @param y Y-coordinate of draw operation
     * @param w Width of draw operation
     * @param h Height of draw operation
     */
    public void image(PImage img, float x, float y, float w, float h) {
        drawCalls++;
        lastDrawX = x;
        lastDrawY = y;
    }

    /** @return Number of drawing operations performed */
    public int getDrawCalls() { return drawCalls; }
    /** @return X-coordinate of last draw operation */
    public float getLastDrawX() { return lastDrawX; }
    /** @return Y-coordinate of last draw operation */
    public float getLastDrawY() { return lastDrawY; }
}
