package inkball;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LineTest {
    /** Verifies points are correctly added to form a line */
    @Test
    void testLineCreation() {
        Line line = new Line();
        line.addPoint(10, 20);
        line.addPoint(30, 40);
        assertEquals(2, line.getPoints().size());
    }
    /** Tests ball reflection physics when colliding with line segment */
    @Test
    void testBallLineCollision() {
        Line line = new Line();
        line.addPoint(100, 100);
        line.addPoint(200, 100);
        Ball ball = new Ball(150, 90, 0, 2, "ball1", null);
        assertTrue(line.handleCollision(ball));
        assertTrue(ball.getVy() < 0); // Ball should bounce upward
    }
    /** Verifies mouse proximity detection for line segments */
    @Test
    void testMouseNearLine() {
        Line line = new Line();
        line.addPoint(100, 100);
        line.addPoint(200, 100);
        assertTrue(line.mouseNearLine(150, 102));
        assertFalse(line.mouseNearLine(150, 150));
    }
    /** Tests handling of drawing empty lines (edge case) */
    @Test
    void testEmptyLineDrawing() {
        Line line = new Line();
        MockApp mockApp = new MockApp();
        line.draw(mockApp);
        assertEquals(0, mockApp.getDrawCalls());
    }
}
