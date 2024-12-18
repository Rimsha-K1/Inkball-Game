package inkball;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class BallTest {
    /** Verifies random velocity initialization sets expected magnitude in both directions */
    @Test
    void testBallInitialization() {
        Ball ball = new Ball(10, 20, 2, 2, "ball1", null);
        assertEquals(10 + App.CELLSIZE/2, ball.getX());
        assertEquals(20 + App.CELLSIZE/2, ball.getY());
    }


    /** Confirms ball position updates correctly based on velocity */
    @Test
    void testBallMovement() {
        Ball ball = new Ball(100, 100, 2, 3, "ball1", null);
        float initialX = ball.getX();
        float initialY = ball.getY();
        ball.updatePosition();
        assertEquals(initialX + 2, ball.getX());
        assertEquals(initialY + 3, ball.getY());
    }
    /** Tests ball bouncing behavior when colliding with a wall */
    @Test
    void testWallCollision() {
        Ball ball = new Ball(50, 50, 2, 0, "ball1", null);
        ArrayList<Wall> walls = new ArrayList<>();
        walls.add(new Wall(3, 3, "wall1", null));
        ball.checkCollisions(walls, new HashMap<>());
        assertEquals(-2, ball.getVx()); // Velocity should reverse
    }
    /** Verifies ball bounces correctly off screen boundaries */
    @Test
    void testScreenBoundaryCollision() {
        Ball ball = new Ball(App.WIDTH - Ball.radius, 100, 2, 0, "ball1", null);
        ball.checkCollisions(new ArrayList<>(), new HashMap<>());
        assertEquals(-2, ball.getVx()); // Should bounce off screen edge
    }
    /** Tests ball capture mechanics when entering hole with matching color */
    @Test
    void testHoleCapture() {
        Ball ball = new Ball(100, 100, 0, 0, "ball1", null);
        ArrayList<Object[]> holes = new ArrayList<>();
        holes.add(new Object[]{100f, 100f, 1}); // Matching color hole
        assertEquals(1, ball.checkHole(holes, 1)); // Should return 1 for correct capture
    }
}
