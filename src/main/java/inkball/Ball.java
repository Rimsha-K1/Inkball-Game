package inkball;

import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;

import processing.core.PImage;
/**
 * Ball class represents game balls that interact with walls and holes.
 * Ball collision detection logic partially adapted from Ankit's bootcamp collision code.
 */
public class Ball {
    private static final float DEFAULT_VELOCITY = 2.0f;
    private static final float HOLE_RANGE = 45.0f;
    private static final float ATTRACTION_FACTOR = 0.005f;
    private static final float CAPTURE_RANGE = 15.0f;



    public void updatePosition() {
        position.add(velocity);
    }
    /**
     * Vector2D utility class for handling 2D vector operations
     *
     *          * Creates a new 2D vector
     *          * @param x X coordinate
     *          * @param y Y coordinate
     *          Adds another vector to this vector
     *          Calculates vector magnitude
     *          @return Length of vector
     *          Normalizes vector to specified speed
     *          *@param targetSpeed Desired speed magnitude
     */
    private static class Vector2D {
        float x, y;

        Vector2D(float x, float y) {
            this.x = x;
            this.y = y;
        }

        void add(Vector2D other) {
            x += other.x;
            y += other.y;
        }

        float magnitude() {
            return (float) Math.sqrt(x * x + y * y);
        }

        void normalize(float targetSpeed) {
            float mag = magnitude();
            if (mag > 0) {
                x = (x / mag) * targetSpeed;
                y = (y / mag) * targetSpeed;
            }
        }
    }

    public static int radius = App.CELLSIZE / 2;
    private final Vector2D position;
    private final Vector2D velocity;
    private String colour;
    private PImage sprite;
    private int collisionBuffer;
    private int colourNumber;
    /**
     * Creates a new ball with specified properties
     */
    public Ball(float x, float y, float vx, float vy, String colour, PImage sprite) {
        position = new Vector2D(x + App.CELLSIZE / 2, y + App.CELLSIZE / 2);
        velocity = new Vector2D(vx, vy);
        this.colour = colour;
        this.sprite = sprite;
        this.colourNumber = Integer.parseInt(colour.substring(4));
    }

    public void initialiseRandomVelocity() {
        Random random = new Random();
        velocity.x = random.nextBoolean() ? -DEFAULT_VELOCITY : DEFAULT_VELOCITY;
        velocity.y = random.nextBoolean() ? -DEFAULT_VELOCITY : DEFAULT_VELOCITY;
    }


    /**
     * Checks and handles all possible collisions
     */
    public void checkCollisions(ArrayList<Wall> walls, HashMap<String, PImage> sprites) {
        checkScreenCollisions();
        if (collisionBuffer == 0) {
            checkWallCollisions(walls, sprites);
        } else {
            collisionBuffer--;
        }
    }
    /**
     * Handles ball collision with screen boundaries
     */
    private void checkScreenCollisions() {
        if (position.x - radius < 0 || position.x + radius > App.WIDTH) {
            velocity.x *= -1;
        }
        if (position.y - radius < App.TOPBAR || position.y + radius > App.HEIGHT) {
            velocity.y *= -1;
        }
    }

    private void checkWallCollisions(ArrayList<Wall> walls, HashMap<String, PImage> sprites) {
        for (Wall wall : walls) {
            if (isCollidingWithWall(wall)) {
                handleWallCollision(wall, sprites);
                collisionBuffer = 2;
                return;
            }
        }
    }
    /**
     * Tests if ball is colliding with specific wall
     */
    private boolean isCollidingWithWall(Wall wall) {
        Vector2D wallCenter = new Vector2D(
                wall.getX() * App.CELLSIZE + App.CELLSIZE / 2,
                wall.getY() * App.CELLSIZE + App.CELLSIZE / 2 + App.TOPBAR
        );
        float distance = new Vector2D(position.x - wallCenter.x, position.y - wallCenter.y).magnitude();
        return distance <= radius + App.CELLSIZE / 2;
    }
    /**
     * Processes wall collision effects including bouncing and color changes
     */
    private void handleWallCollision(Wall wall, HashMap<String, PImage> sprites) {
        // Handle velocity reflection
        if (Math.abs(position.x - (wall.getX() * App.CELLSIZE + App.CELLSIZE / 2)) >
                Math.abs(position.y - (wall.getY() * App.CELLSIZE + App.CELLSIZE / 2 + App.TOPBAR))) {
            velocity.x *= -1;
        } else {
            velocity.y *= -1;
        }

        // Handle color changing
        String wallType = wall.getType();
        String ballType = this.colour;

        if (!wallType.equals("wall0") && !wallType.equals(ballType.replace("ball", "wall"))) {
            String newBallType = wallType.replace("wall", "ball");
            PImage newSprite = sprites.get(newBallType);

            if (newSprite != null) {
                changeColour(newBallType, newSprite);
            }
        }
    }
    /**
     * Checks for hole interactions and captures
     * @return 1 for correct capture, -1 for wrong capture, 0 for no capture
     */

    public int checkHole(ArrayList<Object[]> holeCentres, int ballColour) {
        for (Object[] hole : holeCentres) {
            Vector2D holePosition = new Vector2D((float)hole[0], (float)hole[1]);
            Vector2D distanceVector = new Vector2D(
                    holePosition.x - position.x,
                    holePosition.y - position.y
            );

            float distance = distanceVector.magnitude();

            if (distance <= HOLE_RANGE) {
                // Shrink ball as it approaches hole
                float shrinkFactor = distance / HOLE_RANGE;
                radius = (int)(App.CELLSIZE / 2 * shrinkFactor);

                // Apply hole attraction
                float currentSpeed = velocity.magnitude();
                Vector2D attraction = new Vector2D(
                        distanceVector.x * ATTRACTION_FACTOR,
                        distanceVector.y * ATTRACTION_FACTOR
                );

                velocity.x += attraction.x;
                velocity.y += attraction.y;
                velocity.normalize(currentSpeed);

                // Check for capture
                if (distance <= CAPTURE_RANGE) {
                    int holeColor = (int)hole[2];
                    return isCaptureValid(ballColour, holeColor) ? 1 : -1;
                }
                return 0;
            }
        }

        radius = App.CELLSIZE / 2;
        return 0;
    }
    /**
     * Validates if ball can be captured by hole
     */
    private boolean isCaptureValid(int ballColor, int holeColor) {
        return holeColor == ballColor || ballColor == 0 || holeColor == 0;
    }


    /**
     * Changes ball color and updates related properties
     */
    public void changeColour(String newColour, PImage newSprite) {
        this.colour = newColour;
        this.sprite = newSprite;
        this.colourNumber = Integer.parseInt(newColour.substring(4));
    }


    /**
     * Renders ball on screen
     */
    public void draw(App app) {
        app.image(sprite, position.x - radius, position.y - radius, 2 * radius, 2 * radius);
    }

    // Getters and setters
    public float getX() { return position.x; }
    public float getY() { return position.y; }
    public float getVx() { return velocity.x; }
    public float getVy() { return velocity.y; }
    public void setVx(float vx) { velocity.x = vx; }
    public void setVy(float vy) { velocity.y = vy; }
    public int getColourNumber() { return colourNumber; }
}