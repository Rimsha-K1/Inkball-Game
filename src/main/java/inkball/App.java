package inkball;

import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.event.KeyEvent;

import java.util.*;


public class App extends PApplet {

    public static final int CELLSIZE = 32; //8;

    public static final int TOPBAR = 64;
    public static int WIDTH = 576; ;
    public static int HEIGHT = 640;
    public static final int FPS = 30;
    public String configPath;
    private String[][] board;
    private ArrayList<Wall> walls = new ArrayList<>();
    private ArrayList<Ball> balls = new ArrayList<>(); //stores balls
    private ArrayList<Line> lines;    // stores lines
    private ArrayList<Object[]> holeCentres = new ArrayList<>(); //stores the hole centres and colour (x , y , colour)
    private Line currentLine;
    private ArrayList<int[]> spawnerLocations = new ArrayList<>();
    private Queue<String> ballColoursToSpawn = new LinkedList<>(); //ball colours to spawn
    private int spawnInterval;
    private int spawnTimer;
    private int currentBallIndex = 0;
    private int queueOffset = 0; //for visual queue
    private int countdownX = 200;
    private int countdownY = 20;
    private int score = 0;
    private float scoreIncreaseModifier;
    private float scoreDecreaseModifier;
    private HashMap<String, Integer> scoreIncreaseValues = new HashMap<>();
    private HashMap<String, Integer> scoreDecreaseValues = new HashMap<>();
    private int countdownTime;
    private int timeRemaining;
    private boolean levelEnded = false;
    private boolean gameEnded = false;
    private boolean timeUp = false;
    private boolean gameFinished = false;
    private int currentLevelIndex = 0;
    private JSONArray levels;
    private boolean paused = false;
    private int incrementedTime = 0;
    private boolean incrementingScore = false;
    private String lastBallColor = "";
    private boolean animationTriggered = false;
    private HashMap<String, PImage> sprites = new HashMap<>();
    private TileAnimation tileAnimation;
    public App() {
        this.configPath = "config.json";
    }


    @Override
    public void settings() {
        size(WIDTH, HEIGHT);
    }


    @Override
    public void setup() {
        frameRate(FPS);
        lines = new ArrayList<>();
        currentLine = null;

        loadSprites();
        initialiseBoard();
        loadConfigData();
        tileAnimation = new TileAnimation();


        loadLevelData(currentLevelIndex);


        spawnTimer = spawnInterval * FPS;
        countdownTime = spawnInterval;


    }

    /**
     * Loads all game sprites into memory. Initializes sprites for tiles, walls,
     * holes, balls and other game elements.
     */
    private void loadSprites() {


        String[] spriteNames = {
                "tile", "wall0", "wall1", "wall2", "wall3", "wall4",
                "hole0", "hole1", "hole2", "hole3", "hole4", "entrypoint",
                "ball0", "ball1", "ball2", "ball3", "ball4",

        };


        for (String spriteName : spriteNames) {
            getSprite(spriteName);
        }
    }

    /**
     * Initialises the game board with dimensions based on window size and cell size.
     * Creates a 2D array to store the board state.
     */
    private void initialiseBoard() {

        this.board = new String[(HEIGHT - TOPBAR) / CELLSIZE][WIDTH / CELLSIZE];
    }

    /**
     * Loads game configuration from config.json file.
     * Stores level data in the levels JSONArray.
     */
    private void loadConfigData() {
        JSONObject config = loadJSONObject("config.json");
        levels = config.getJSONArray("levels");
    }

    /**
     * Retrieves or loads a sprite image by name.
     * @param spriteName Name of the sprite to load
     * @return The loaded PImage sprite, or null if not found
     */
    private PImage getSprite(String spriteName) {


        PImage sprite = sprites.get(spriteName);
        if (sprite == null) {
            try {


                String path = this.getClass().getResource("/inkball/" + spriteName + ".png").getPath().replace("%20", " ");
                sprite = loadImage(path);
                sprites.put(spriteName, sprite);
            } catch (NullPointerException e) {


                System.out.println("The file /inkball/" + spriteName + ".png is missing or inaccessible");
            }
        }
        return sprite;
    }

    /**
     * Loads level data for the specified level index.
     * Sets up game state, timer, and level configuration.
     * @param levelIndex Index of level to load
     */
    private void loadLevelData(int levelIndex) {

        if (levelIndex >= levels.size()) {
            gameEnded = true;
            return;
        }

        JSONObject levelData = levels.getJSONObject(levelIndex);
        loadLevel(levelData.getString("layout"));
        loadConfig(levelData);

        timeRemaining = levelData.getInt("time");
        levelEnded = false;
        timeUp = false;
        spawnTimer = spawnInterval * FPS;
        countdownTime = spawnInterval;
    }


    private enum TileType {
        WALL('X', '1', '2', '3', '4'),
        SPAWNER('S'),
        BALL('B'),
        HOLE('H'),
        DEFAULT(' ');

        private final char[] chars;

        TileType(char... chars) {
            this.chars = chars;
        }

        static TileType from(char c) {
            for (TileType type : values()) {
                for (char typeChar : type.chars) {
                    if (typeChar == c) return type;
                }
            }
            return DEFAULT;
        }
    }

    private class TilePosition {
        final int row, col;
        final float x, y;

        TilePosition(int row, int col) {
            this.row = row;
            this.col = col;
            this.x = col * CELLSIZE;
            this.y = row * CELLSIZE + TOPBAR;
        }
    }
    /**
     * Loads level layout from file and initializes game objects.
     * Creates walls, holes, spawners and initial balls.
     * @param filename Path to level layout file
     */
    private void loadLevel(String filename) {
        spawnerLocations.clear();
        holeCentres.clear();
        walls.clear();
        balls.clear();
        lines.clear();
        String[] levelLines = loadStrings(filename);


        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                board[row][col] = "tile";
            }
        }


        for (int row = 0; row < levelLines.length; row++) {
            String line = levelLines[row];
            for (int col = 0; col < line.length(); col++) {
                char currentTile = line.charAt(col);
                char nextTile = (col + 1 < line.length()) ? line.charAt(col + 1) : ' ';
                showTiles(row, col, currentTile, nextTile, line);

                if (currentTile == 'B' || currentTile == 'H') {
                    col++;
                }
            }
        }
    }

    private void showTiles(int row, int col, char tileChar, char nextChar, String line) {
        TilePosition pos = new TilePosition(row, col);

        switch (TileType.from(tileChar)) {
            case WALL:
                String wallName = "wall" + (tileChar == 'X' ? "0" : String.valueOf(tileChar));
                createWall(pos, wallName);
                board[row][col] = wallName;
                break;

            case SPAWNER:
                createSpawner(pos);
                break;

            case BALL:
                createBall(pos, nextChar);
                break;

            case HOLE:
                createHole(pos, nextChar);
                break;

            default:
                board[row][col] = "tile";
        }
    }

    private void createWall(TilePosition pos, String wallName) {
        PImage wallSprite = getSprite(wallName);
        walls.add(new Wall(pos.col, pos.row, wallName, wallSprite));
    }

    private void createBall(TilePosition pos, char colorChar) {
        String ballName = "ball" + colorChar;
        Ball ball = new Ball(pos.x, pos.y, 0, 0, ballName, getSprite(ballName));
        ball.initialiseRandomVelocity();
        balls.add(ball);

        board[pos.row][pos.col] = "tile";
        board[pos.row][pos.col + 1] = "tile";
    }

    private void createHole(TilePosition pos, char colorChar) {
        float centerX = pos.x + CELLSIZE;
        float centerY = pos.y + CELLSIZE;
        int holeColor = Character.getNumericValue(colorChar);
        String holeName = "hole" + colorChar;

        holeCentres.add(new Object[]{centerX, centerY, holeColor});

        // Set hole tiles
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                board[pos.row + r][pos.col + c] = holeName;
            }
        }
        board[pos.row][pos.col + 1] = "tile";
    }

    private void createSpawner(TilePosition pos) {
        spawnerLocations.add(new int[]{(int)pos.x, (int)pos.y});
        board[pos.row][pos.col] = "entrypoint";
    }



    public void loadConfig(JSONObject levelData) {


        spawnInterval = levelData.getInt("spawn_interval");
        JSONArray ballsArray = levelData.getJSONArray("balls");


        ballColoursToSpawn.clear();
        for (int i = 0; i < ballsArray.size(); i++) {
            String colour = ballsArray.getString(i);
            ballColoursToSpawn.add(String.valueOf(getColour(colour)));
        }


        spawnTimer = spawnInterval * FPS;
        currentBallIndex = 0;


        scoreIncreaseModifier = (float) levelData.getDouble("score_increase_from_hole_capture_modifier");
        scoreDecreaseModifier = (float) levelData.getDouble("score_decrease_from_wrong_hole_modifier");


        loadScore();
    }


    private void loadScore() {


        JSONObject config = loadJSONObject("config.json");
        JSONObject scoreIncreaseConfig = config.getJSONObject("score_increase_from_hole_capture");
        JSONObject scoreDecreaseConfig = config.getJSONObject("score_decrease_from_wrong_hole");


        scoreIncreaseValues.put("grey", scoreIncreaseConfig.getInt("grey"));
        scoreIncreaseValues.put("orange", scoreIncreaseConfig.getInt("orange"));
        scoreIncreaseValues.put("blue", scoreIncreaseConfig.getInt("blue"));
        scoreIncreaseValues.put("green", scoreIncreaseConfig.getInt("green"));
        scoreIncreaseValues.put("yellow", scoreIncreaseConfig.getInt("yellow"));


        scoreDecreaseValues.put("grey", scoreDecreaseConfig.getInt("grey"));
        scoreDecreaseValues.put("orange", scoreDecreaseConfig.getInt("orange"));
        scoreDecreaseValues.put("blue", scoreDecreaseConfig.getInt("blue"));
        scoreDecreaseValues.put("green", scoreDecreaseConfig.getInt("green"));
        scoreDecreaseValues.put("yellow", scoreDecreaseConfig.getInt("yellow"));
    }


    @Override
    public void keyPressed(KeyEvent event) {
        char key = event.getKey();

        if (key== 'r') {
            resetGame();  // Directly call resetGame to restart anytime
        }

        if (key == ' ') {
            paused = !paused;
        }
    }


    private void resetGame() {

        currentLevelIndex = 0;
        score = 0;
        incrementedTime = 0;
        gameEnded = false;
        levelEnded = false;
        timeUp = false;
        incrementingScore = false;
        animationTriggered = false;

        balls.clear();
        lines.clear();

        loadLevelData(currentLevelIndex);
    }




    @Override
    public void mousePressed() {

        if (mouseButton == LEFT) {
            startNewLine();
        } else if (mouseButton == RIGHT) {
            deleteLine();
        }
    }


    @Override
    public void mouseDragged() {


        if (mouseButton == LEFT && currentLine != null) {
            currentLine.addPoint(mouseX, mouseY);
        }
    }


    @Override
    public void mouseReleased() {


        if (mouseButton == LEFT && currentLine != null) {
            lines.add(currentLine);
            currentLine = null;
        }
    }


    private void startNewLine() {


        currentLine = new Line();
        currentLine.addPoint(mouseX, mouseY);
    }


    private void deleteLine() {


        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (line.mouseNearLine(mouseX, mouseY)) {
                lines.remove(i);
                break;
            }
        }
    }


    @Override
    public void draw() {
        background(123);
        GameState.update(this);
        renderGame();

        if (levelEnded || gameEnded) {
            return;
        }
    }

    private static class GameState {
        private static void update(App app) {
            app.updateTimers();
            app.handleGameProgress();
            //app.displayGameInfo();
        }
    }

    private void updateTimers() {
        if (paused || timeUp) {
            return;
        }

        updateGameTimer();
        updateBallSpawner();
    }

    private void updateGameTimer() {
        if (levelEnded || timeRemaining <= 0) {
            return;
        }

        if (frameCount % FPS == 0) {
            timeRemaining--;
            checkTimeUp();
        }
    }

    private void checkTimeUp() {
        if (timeRemaining <= 0) {
            timeUp = true;
            levelEnded = true;
        }
    }

    private void updateBallSpawner() {
        if (ballColoursToSpawn.isEmpty() && spawnTimer <= 0) {
            countdownTime = 0;
            return;
        }

        if (spawnTimer <= 0) {
            spawnNewBall();
        } else {
            spawnTimer--;
            countdownTime = spawnTimer / FPS;
        }
    }

    private void spawnNewBall() {
        spawnBall();
        spawnTimer = spawnInterval * FPS;
        countdownTime = spawnInterval;
    }

    private void handleGameProgress() {
        if (paused || incrementingScore) {
            if (incrementingScore) {
                updateScore();
                displayScore();
            }
            return;
        }

        checkLevelCompletion();
    }

    private void updateScore() {
        if (incrementedTime >= timeRemaining) {
            incrementingScore = false;
            animationTriggered = false;
            timeRemaining = 0;
            return;
        }

        if (frameCount % 2 == 0) {
            score++;
            incrementedTime++;
            animateTiles(1);
        } else {
            animateTiles(0);
        }
    }



    private void renderGame() {
        drawGameElements();
        displayMessages();
    }

    private void drawGameElements() {


        drawBoard();
        drawWalls();
        drawLines();
        drawBalls();
        displayBallQueue();
        displayTimer();
        displayScore();
    }


    private void drawBoard() {
        // Layer 1: Draw base tiles
        drawBaseTiles();

        // Layer 2: Draw holes
        drawHoles();

        // Layer 3: Draw borders/UI elements
        drawBoardBorders();
    }
    /**
     * Renders the base tile layer of the game board.
     * Draws background tiles and spawner points across the entire game area.
     * Uses tile and spawner sprites from the sprite collection.
     */
    private void drawBaseTiles() {
        PImage tileSprite = getSprite("tile");
        PImage spawnerSprite = getSprite("entrypoint");

        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                float x = col * CELLSIZE;
                float y = row * CELLSIZE + TOPBAR;

                // Draw base tile
                image(tileSprite, x, y, CELLSIZE, CELLSIZE);

                // Draw spawner points on top if present
                if (board[row][col].equals("entrypoint")) {
                    image(spawnerSprite, x, y, CELLSIZE, CELLSIZE);
                }
            }
        }
    }

    private void drawHoles() {
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                String spriteName = board[row][col];
                if (spriteName != null && spriteName.startsWith("hole")) {
                    float x = col * CELLSIZE;
                    float y = row * CELLSIZE + TOPBAR;
                    PImage holeSprite = getSprite(spriteName);

                    // Draw holes at double size
                    image(holeSprite, x, y, CELLSIZE * 2, CELLSIZE * 2);
                }
            }
        }
    }

    private void drawBoardBorders() {
        stroke(80); // Darker border color
        strokeWeight(2);
        noFill();

        // Draw border around game area
        rect(0, TOPBAR, WIDTH, HEIGHT - TOPBAR);

        // Draw grid lines (optional)
        stroke(200, 100); // Light gray, semi-transparent
        strokeWeight(1);

        for (int x = 0; x <= WIDTH; x += CELLSIZE) {
            line(x, TOPBAR, x, HEIGHT);
        }

        for (int y = TOPBAR; y <= HEIGHT; y += CELLSIZE) {
            line(0, y, WIDTH, y);
        }
    }



    private void drawWalls() {
        for (Wall wall : walls) {
            wall.draw(this);
        }
    }


    private void drawLines() {

        //draw lines if time not up
        if (!timeUp) {
            stroke(0);  // line colour
            strokeWeight(10);  // line thickness
            for (Line line : lines) {
                line.draw(this);
            }

            //draw line if exists
            if (currentLine != null) {
                currentLine.draw(this);
            }
        }
    }


    private void drawBalls() {

        //List to store removed balls
        List<Ball> ballsToRemove = new ArrayList<>();

        //iterate through each ball
        for (Ball ball : balls) {
            updateBalls(ball, ballsToRemove);
        }

        //remove balls marked for removal
        balls.removeAll(ballsToRemove);
    }

    /**
     * Defines constants for ball queue display positioning and dimensions.
     * Contains layout measurements for the UI element showing upcoming balls.
     */
    private static final class QueueDisplay {
        static final int QUEUE_START_X = 20;
        static final int QUEUE_START_Y = 10;
        static final int BALL_SPACING = 34;
        static final int QUEUE_WIDTH = 170;
        static final int QUEUE_HEIGHT = 44;
        static final int QUEUE_TOP = 5;
    }

    private void displayBallQueue() {
        drawQueueBackground();
        updateQueueOffset();
        drawBallQueue();
        drawQueueBorders();
    }

    private void drawQueueBackground() {
        noStroke();
        fill(0);
        rect(18, QueueDisplay.QUEUE_TOP, QueueDisplay.QUEUE_WIDTH, QueueDisplay.QUEUE_HEIGHT);
    }

    private void updateQueueOffset() {
        if (queueOffset > 0) {
            queueOffset--;
        }
    }

    private void drawBallQueue() {
        int startX = QueueDisplay.QUEUE_START_X + queueOffset;

        // Draw last emitted ball
        drawLastEmittedBall(startX);

        // Draw upcoming balls
        drawUpcomingBalls(startX);

        // Update queue state
        updateQueueState();
    }

    private void drawLastEmittedBall(int startX) {
        if (!lastBallColor.isEmpty()) {
            PImage sprite = getSprite("ball" + lastBallColor);
            if (sprite != null) {
                image(sprite, startX - QueueDisplay.BALL_SPACING, QueueDisplay.QUEUE_START_Y, CELLSIZE, CELLSIZE);
            }
        }
    }

    private void drawUpcomingBalls(int startX) {
        String[] queueArray = ballColoursToSpawn.toArray(new String[0]);
        for (int i = 0; i < queueArray.length; i++) {
            PImage sprite = getSprite("ball" + queueArray[i]);
            if (sprite != null) {
                float x = startX + i * QueueDisplay.BALL_SPACING;
                image(sprite, x, QueueDisplay.QUEUE_START_Y, CELLSIZE, CELLSIZE);
            }
        }
    }

    private void updateQueueState() {
        if (ballColoursToSpawn.isEmpty() && spawnTimer == 0) {
            lastBallColor = "";
        }
    }

    private void drawQueueBorders() {
        fill(200);
        // Left border
        rect(0, QueueDisplay.QUEUE_TOP, 18, QueueDisplay.QUEUE_HEIGHT);
        // Right border
        rect(188, QueueDisplay.QUEUE_TOP, WIDTH - 190, QueueDisplay.QUEUE_HEIGHT);
        // Top border
        rect(0, 0, WIDTH, QueueDisplay.QUEUE_TOP);
        // Bottom border
        rect(0, 49, WIDTH, TOPBAR - 49);
    }

    private void displayMessages() {

        //display message on game state
        //pause message
        if (paused) {
            fill(256, 0, 0);
            textAlign(CENTER, CENTER);
            textSize(20);
            text("=== PAUSED ===", WIDTH / 2, TOPBAR - 20);
        } else if (timeUp && gameEnded && !gameFinished) {

            //time up message
            fill(256, 0, 0);
            textAlign(CENTER, CENTER);
            textSize(25);
            text("=== TIME'S UP ===", WIDTH / 2, TOPBAR - 20);
        } else if (gameEnded && gameFinished) {

            //game end message
            fill(256, 0, 0);
            textAlign(CENTER, CENTER);
            textSize(25);
            text("=== ENDED ===", WIDTH / 2, TOPBAR / 2);
        }
    }


    private static class ScoreManager {
        private static final int SCORE_INCREMENT_FRAME_INTERVAL = 2;

        static void updateScore(App app) {
            if (app.incrementedTime >= app.timeRemaining) {
                finishScoreIncrement(app);
                return;
            }

            if (app.frameCount % SCORE_INCREMENT_FRAME_INTERVAL == 0) {
                incrementScore(app);
            } else {
                app.animateTiles(0);
            }
            app.displayScore();
        }

        private static void incrementScore(App app) {
            app.score++;
            app.incrementedTime++;
            app.animateTiles(1);
        }

        private static void finishScoreIncrement(App app) {
            app.incrementingScore = false;
            app.animationTriggered = false;
            app.timeRemaining = 0;
        }
    }
    private void displayTimer() {
        // Main timer
        setTextProperties(16, RIGHT, CENTER);
        text("Time: " + timeRemaining, WIDTH - 30, 40);

        // Spawn countdown timer
        setTextProperties(14, LEFT, CENTER);
        String countdownDisplay = String.format("%.1f", spawnTimer / (float) FPS);
        text(countdownDisplay, countdownX, countdownY);
    }

    private void displayScore() {
        setTextProperties(16, RIGHT, CENTER);
        text("Score: " + score, WIDTH - 30, 20);
    }

    private void setTextProperties(int size, int alignX, int alignY) {
        fill(0);
        textSize(size);
        textAlign(alignX, alignY);
    }

    private static class DisplayManager {
        private static void renderTimer(App app) {
            app.setupTextStyle(16, app.RIGHT, app.CENTER, 0);
            app.text("Time: " + app.timeRemaining, app.WIDTH - 30, 40);

            app.setupTextStyle(14, app.LEFT, app.CENTER, 0);
            app.text(String.format("%.1f", app.spawnTimer / (float) app.FPS),
                    app.countdownX, app.countdownY);
        }

        private static void renderScore(App app) {
            app.setupTextStyle(16, app.RIGHT, app.CENTER, 0);
            app.text("Score: " + app.score, app.WIDTH - 30, 20);
        }
    }

    private void setupTextStyle(int size, int alignX, int alignY, int color) {
        fill(color);
        textSize(size);
        textAlign(alignX, alignY);
    }

    private void updateBalls(Ball ball, List<Ball> ballsToRemove) {
        updateBallState(ball);
        handleBallInteractions(ball, ballsToRemove);
        ball.draw(this);
    }

    private void updateBallState(Ball ball) {
        if (!paused && !timeUp) {
            ball.updatePosition();
        }
        ball.checkCollisions(walls, sprites);
    }
    /**
     * Defines possible outcomes when a ball collides with a hole.
     * CORRECT_HOLE (1): Ball matches hole color
     * WRONG_HOLE (-1): Ball doesn't match hole color
     * NO_COLLISION (0): No hole collision occurred
     */
    private enum CollisionResult {
        CORRECT_HOLE(1),
        WRONG_HOLE(-1),
        NO_COLLISION(0);

        private final int value;
        CollisionResult(int value) { this.value = value; }

        static CollisionResult fromValue(int value) {
            for (CollisionResult result : values()) {
                if (result.value == value) return result;
            }
            return NO_COLLISION;
        }
    }

    private void handleBallInteractions(Ball ball, List<Ball> ballsToRemove) {
        int ballColor = ball.getColourNumber();

        handleLineCollisions(ball);
        handleHoleCollisions(ball, ballColor, ballsToRemove);
    }

    private void handleLineCollisions(Ball ball) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).handleCollision(ball)) {
                lines.remove(i);
                break;
            }
        }
    }
    /**
     * Processes ball collisions with holes, updating game state and score.
     * Handles both correct and incorrect hole collisions with appropriate score adjustments.
     * @param ball The ball involved in the collision
     * @param ballColor Color of the ball
     * @param ballsToRemove List to track balls that should be removed
     */
    private void handleHoleCollisions(Ball ball, int ballColor, List<Ball> ballsToRemove) {
        CollisionResult result = CollisionResult.fromValue(ball.checkHole(holeCentres, ballColor));

        switch (result) {
            case CORRECT_HOLE:
                handleCorrectHole(ball, ballColor, ballsToRemove);
                break;

            case WRONG_HOLE:
                handleWrongHole(ball, ballColor, ballsToRemove);
                break;

            default:
                // Ball continues moving
                break;
        }
    }

    private void handleCorrectHole(Ball ball, int ballColor, List<Ball> ballsToRemove) {
        increaseScore(ballColor);
        ballsToRemove.add(ball);
    }

    private void handleWrongHole(Ball ball, int ballColor, List<Ball> ballsToRemove) {
        decreaseScore(ballColor);
        ballsToRemove.add(ball);
        ballColoursToSpawn.add(String.valueOf(ballColor));
        spawnTimer = spawnInterval * FPS;
    }


    private enum ScoreAction {
        INCREASE(1),
        DECREASE(-1);

        private final int multiplier;
        ScoreAction(int multiplier) { this.multiplier = multiplier; }
    }

    private void updateScore(int ballColor, ScoreAction action) {
        String colorName = getNumber(ballColor);
        Map<String, Integer> valueMap = (action == ScoreAction.INCREASE) ?
                scoreIncreaseValues : scoreDecreaseValues;
        int modifier = (int) ((action == ScoreAction.INCREASE) ?
                        scoreIncreaseModifier : scoreDecreaseModifier);

        if (valueMap.containsKey(colorName)) {
            int changeValue = valueMap.get(colorName) * modifier * action.multiplier;
            score += changeValue;
        } else {
            System.out.println("Invalid color name: " + colorName);
        }
    }

    private void increaseScore(int ballColor) {
        updateScore(ballColor, ScoreAction.INCREASE);
    }

    private void decreaseScore(int ballColor) {
        updateScore(ballColor, ScoreAction.DECREASE);
    }

    private void checkLevelCompletion() {
        if (!isLevelComplete()) return;

        handleTimeBonus();

        if (!incrementingScore) {
            progressLevel();
        }
    }

    private boolean isLevelComplete() {
        return balls.isEmpty() && ballColoursToSpawn.isEmpty();
    }

    private void handleTimeBonus() {
        if (!gameEnded && timeRemaining > 0 && !incrementingScore) {
            System.out.println("time remaining: " + timeRemaining + " " + incrementingScore);
            incrementingScore = true;
            incrementedTime = 0;
            animationTriggered = false;
        }
    }

    private void progressLevel() {
        if (isLastLevel()) {
            endGame();
        } else {
            loadNextLevel();
        }
    }

    private void endGame() {
        gameEnded = true;
        gameFinished = true;
    }

    private void loadNextLevel() {
        currentLevelIndex++;
        resetLevelState();
        clearGameObjects();
        loadLevelData(currentLevelIndex);
    }

    private void resetLevelState() {
        levelEnded = false;
        timeUp = false;
        gameFinished = false;
        incrementingScore = false;
        animationTriggered = true;
        incrementedTime = 0;
    }

    private void clearGameObjects() {
        balls.clear();
        lines.clear();
    }


    private enum Direction {
        RIGHT(1, 0),
        DOWN(0, 1),
        LEFT(-1, 0),
        UP(0, -1);

        final int dx, dy;
        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        Direction next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
    /**
     * Represents a tile that can move in four directions around the game board.
     * Handles individual tile movement, direction changes, and rendering.
     */
    private class AnimatedTile {
        private int x, y;
        private Direction direction;

        AnimatedTile(int x, int y, Direction direction) {
            this.x = x;
            this.y = y;
            this.direction = direction;
        }

        void move() {
            x += direction.dx;
            y += direction.dy;
        }

        void changeDirection() {
            direction = direction.next();
        }

        void draw(PImage sprite) {
            image(sprite, x * CELLSIZE, y * CELLSIZE + TOPBAR, CELLSIZE, CELLSIZE);
        }
    }
    /**
     * Manages the animation of tiles during score increments.
     * Controls two animated tiles that move in opposite directions around the board perimeter.
     * Handles animation state, movement timing, and rendering.
     */
    private class TileAnimation {
        private static final int GRID_SIZE = 17;
        private final AnimatedTile topLeft;
        private final AnimatedTile bottomRight;
        private int stepCounter = 0;
        private boolean isActive = false;
        private PImage sprite;

        TileAnimation() {
            sprite = getSprite("wall4");
            topLeft = new AnimatedTile(0, 0, Direction.RIGHT);
            bottomRight = new AnimatedTile(GRID_SIZE, GRID_SIZE, Direction.LEFT);
        }

        void reset() {
            topLeft.x = 0;
            topLeft.y = 0;
            topLeft.direction = Direction.RIGHT;
            bottomRight.x = GRID_SIZE;
            bottomRight.y = GRID_SIZE;
            bottomRight.direction = Direction.LEFT;
            stepCounter = 0;
            isActive = true;
        }

        void update(boolean shouldMove) {
            if (!isActive) return;

            topLeft.draw(sprite);
            bottomRight.draw(sprite);

            if (!shouldMove) return;

            topLeft.move();
            bottomRight.move();

            stepCounter++;
            if (stepCounter >= GRID_SIZE) {
                stepCounter = 0;
                topLeft.changeDirection();
                bottomRight.changeDirection();
            }
        }
    }



    private void animateTiles(int shouldMove) {
        if (!animationTriggered) {
            tileAnimation.reset();
            animationTriggered = true;
        }

        tileAnimation.update(shouldMove == 1);
    }



    private boolean isLastLevel() {
        return currentLevelIndex >= levels.size() - 1;
    }

    /**
     * Manages ball spawning mechanics including random spawn location selection,
     * ball creation with initial velocities, and queue management.
     * Provides controlled access to spawn new balls into the game.
     */
    private class BallSpawner {
        private static final float INITIAL_VELOCITY = 2.0f;
        private static final int QUEUE_OFFSET_INCREMENT = 34;

        private final Random random = new Random();

        // Simple helper class for spawn coordinates
        private class SpawnLocation {
            private final float x;
            private final float y;

            public SpawnLocation(int[] location) {
                this.x = location[0];
                this.y = location[1];
            }

            public float getX() { return x; }
            public float getY() { return y; }
        }

        private SpawnLocation getRandomSpawnLocation() {
            int[] location = spawnerLocations.get(random.nextInt(spawnerLocations.size()));
            return new SpawnLocation(location);
        }

        private Ball createBall(SpawnLocation spawn, String colorString) {
            String spriteName = "ball" + colorString;
            PImage sprite = getSprite(spriteName);

            return new Ball(
                    spawn.getX(),
                    spawn.getY(),
                    randomVelocity(),
                    randomVelocity(),
                    spriteName,
                    sprite
            );
        }

        private float randomVelocity() {
            return random.nextBoolean() ? INITIAL_VELOCITY : -INITIAL_VELOCITY;
        }

        public boolean spawn() {
            if (spawnerLocations.isEmpty() || ballColoursToSpawn.isEmpty()) {
                return false;
            }

            SpawnLocation spawnLocation = getRandomSpawnLocation();
            String colorString = ballColoursToSpawn.poll();

            Ball newBall = createBall(spawnLocation, colorString);
            balls.add(newBall);

            lastBallColor = colorString;
            currentBallIndex++;
            queueOffset += QUEUE_OFFSET_INCREMENT;

            return true;
        }
    }

    private final BallSpawner ballSpawner = new BallSpawner();

    private void spawnBall() {
        ballSpawner.spawn();
    }
    private int getColour(String colourName) {


        switch (colourName.toLowerCase()) {
            case "orange":
                return 1;
            case "blue":
                return 2;
            case "green":
                return 3;
            case "yellow":
                return 4;
            case "grey":
                return 0;
            default:
                return 0;
        }
    }


    private String getNumber(int colorNumber) {

        switch (colorNumber) {
            case 0:
                return "grey";
            case 1:
                return "orange";
            case 2:
                return "blue";
            case 3:
                return "green";
            case 4:
                return "yellow";
            default:
                return "grey";
        }
    }




    public static void main(String[] args) {

        PApplet.main("inkball.App");
    }
}
