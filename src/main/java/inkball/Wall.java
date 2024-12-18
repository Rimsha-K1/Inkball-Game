package inkball;

import processing.core.PImage;
/**
 * Represents a wall element in the game that can interact with balls and affect gameplay.
 * Implements Draw interface for rendering.
 */
public class Wall implements Draw {
    private final Position position;
    private final String type;
    private final PImage sprite;

    /**
     * Inner class representing grid-based positioning with screen coordinate conversion
     */
    private static class Position {
        final int x, y;

        /**
         * Creates a new grid position
         * @param x Grid x-coordinate
         * @param y Grid y-coordinate
         */
        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
        /**
         * Converts grid x-coordinate to screen coordinate
         * @return Screen x-coordinate in pixels
         */
        float getScreenX() {
            return x * App.CELLSIZE;
        }
        /**
         * Converts grid y-coordinate to screen coordinate
         * @return Screen y-coordinate in pixels, adjusted for top bar
         */
        float getScreenY() {
            return y * App.CELLSIZE + App.TOPBAR;
        }
    }
    /**
     * Creates a new wall with specified properties
     * @param x Grid x-coordinate
     * @param y Grid y-coordinate
     * @param type Wall type identifier
     * @param sprite Wall visual representation
     */
    public Wall(int x, int y, String type, PImage sprite) {
        this.position = new Position(x, y);
        this.type = type;
        this.sprite = sprite;
    }
    /**
     * Renders the wall on screen
     * @param app Reference to main application for drawing
     */
    public void draw(App app) {
        app.image(sprite, position.getScreenX(), position.getScreenY(),
                App.CELLSIZE, App.CELLSIZE);
    }

    public int getX() { return position.x; }
    public int getY() { return position.y; }
    public String getType() { return type; }
}



