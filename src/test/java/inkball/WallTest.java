package inkball;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WallTest {
    /** Verifies wall grid positioning system */
    @Test
    void testWallPositioning() {
        Wall wall = new Wall(5, 3, "wall1", null);
        assertEquals(5, wall.getX());
        assertEquals(3, wall.getY());
    }

    @Test
    /** Tests conversion from grid coordinates to screen pixels */
    void testScreenCoordinateConversion() {
        Wall wall = new Wall(2, 3, "wall1", null);
        float expectedX = 2 * App.CELLSIZE;
        float expectedY = 3 * App.CELLSIZE + App.TOPBAR;
        MockApp mockApp = new MockApp();
        wall.draw(mockApp);
        assertEquals(expectedX, mockApp.getLastDrawX());
        assertEquals(expectedY, mockApp.getLastDrawY());
    }

    @Test
    /** Confirms wall type identification system works */
    void testWallTypeRetrieval() {
        Wall wall = new Wall(0, 0, "wall2", null);
        assertEquals("wall2", wall.getType());
    }
}