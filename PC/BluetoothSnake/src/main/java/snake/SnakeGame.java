package snake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import java.util.Random;

public class SnakeGame extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int GRID_SIZE = 20; // Size of each grid cell
    private static final int GAME_WIDTH = 400; // Total game width
    private static final int GAME_HEIGHT = 400; // Total game height

    // Game objects
    private final LinkedList<Point> snake; // Stores snake segments
    private final LinkedList<Point> obstacles; // Stores obstacle positions
    private final Timer timer; // Game timer for updates
    private final SnakeApp app; // Reference to main application
    private final Difficulty difficulty; // Current difficulty level

    // Game state variables
    private Direction currentDirection = Direction.RIGHT; // Current movement direction
    private Point food; // Current food position
    private int score = 0; // Player score
    private boolean gameOver = false; // Game over flag

    // Constructs the SnakeGame panel.
    public SnakeGame(SnakeApp app, Difficulty difficulty) {
        this.app = app;
        this.difficulty = difficulty;

        // Set up panel properties
        setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        // Initialize snake with starting position
        snake = new LinkedList<>();
        snake.add(new Point(GAME_WIDTH / 2, GAME_HEIGHT / 2));

        // Initialize obstacles
        obstacles = new LinkedList<>();
        generateFood();
        generateObstacles();

        // Set up game timer with difficulty-based delay
        timer = new Timer(difficulty.getDelay(), this);
        timer.start();
    }

    // Moves the snake according to current direction.
    // Handles collision detection, food eating, and game over conditions.
    private void move() {
        if (gameOver) return;

        // Calculate new head position
        Point head = snake.getFirst();
        Point newHead = new Point(head);
        switch (currentDirection) {
            case UP -> newHead.y -= GRID_SIZE;
            case DOWN -> newHead.y += GRID_SIZE;
            case LEFT -> newHead.x -= GRID_SIZE;
            case RIGHT -> newHead.x += GRID_SIZE;
        }

        // Handle screen wrapping
        if (newHead.x < 0) newHead.x = GAME_WIDTH - GRID_SIZE;
        else if (newHead.x >= GAME_WIDTH) newHead.x = 0;
        if (newHead.y < 0) newHead.y = GAME_HEIGHT - GRID_SIZE;
        else if (newHead.y >= GAME_HEIGHT) newHead.y = 0;

        // Check for collisions
        if (snake.contains(newHead)) {
            gameOver = true;
            app.setGameState(GameState.GAME_OVER);
            return;
        }
        if (obstacles.contains(newHead)) {
            gameOver = true;
            app.setGameState(GameState.GAME_OVER);
            return;
        }

        // Move snake
        snake.addFirst(newHead);

        // Check if food was eaten
        if (newHead.equals(food)) {
            score++;
            generateFood();
        } else {
            snake.removeLast(); // Remove tail if no food eaten
        }
    }

    // Generates obstacles based on current difficulty.
    private void generateObstacles() {
        obstacles.clear();
        Random random = new Random();
        Point startingPoint = new Point(GAME_WIDTH / 2, GAME_HEIGHT / 2);

        for (int i = 0; i < difficulty.getNumObstacles(); i++) {
            int x, y;
            Point newObstacle;
            do {
                // Generate random position that doesn't overlap with snake, food, or starting line
                x = random.nextInt(GAME_WIDTH / GRID_SIZE) * GRID_SIZE;
                y = random.nextInt(GAME_HEIGHT / GRID_SIZE) * GRID_SIZE;
                newObstacle = new Point(x, y);
            } while (snake.contains(newObstacle) || newObstacle.equals(food) || isOnInitialLine(newObstacle, startingPoint));
            obstacles.add(newObstacle);
        }
    }

    // Checks if a point is on the snake's initial horizontal line.
    private boolean isOnInitialLine(Point point, Point startingPoint) {
        return point.y == startingPoint.y;
    }

    // Generates new food at a random valid position.
    private void generateFood() {
        Random random = new Random();
        Point startingPoint = new Point(GAME_WIDTH / 2, GAME_HEIGHT / 2);
        int x, y;
        Point newFood;
        do {
            // Find position that doesn't overlap with snake, obstacles, or starting line
            x = random.nextInt(GAME_WIDTH / GRID_SIZE) * GRID_SIZE;
            y = random.nextInt(GAME_HEIGHT / GRID_SIZE) * GRID_SIZE;
            newFood = new Point(x, y);
        } while (snake.contains(newFood) || obstacles.contains(newFood) || isOnInitialLine(newFood, startingPoint));
        food = new Point(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw game over screen if applicable
        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            String gameOverText = "Game Over! Score: " + score;
            // Center the game over text
            g.drawString(gameOverText, (GAME_WIDTH - g.getFontMetrics().stringWidth(gameOverText)) / 2, GAME_HEIGHT / 2);
            return;
        }

        // Draw snake (green)
        g.setColor(Color.GREEN);
        for (Point p : snake) {
            g.fillRect(p.x, p.y, GRID_SIZE, GRID_SIZE);
        }

        // Draw food (red)
        g.setColor(Color.RED);
        g.fillRect(food.x, food.y, GRID_SIZE, GRID_SIZE);

        // Draw obstacles (gray)
        g.setColor(Color.GRAY);
        for (Point p : obstacles) {
            g.fillRect(p.x, p.y, GRID_SIZE, GRID_SIZE);
        }

        // Draw score (white)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 10, 20);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint(); // Update the display
    }

    // Changes the snake's direction if the new direction is valid.
    public void setDirection(Direction newDirection) {
        if ((currentDirection == Direction.UP && newDirection != Direction.DOWN) ||
                (currentDirection == Direction.DOWN && newDirection != Direction.UP) ||
                (currentDirection == Direction.LEFT && newDirection != Direction.RIGHT) ||
                (currentDirection == Direction.RIGHT && newDirection != Direction.LEFT)) {
            currentDirection = newDirection;
        }
    }

    // Resets the game to initial state.
    public void resetGame() {
        snake.clear();
        snake.add(new Point(GAME_WIDTH / 2, GAME_HEIGHT / 2));
        currentDirection = Direction.RIGHT;
        score = 0;
        gameOver = false;
        generateFood();
        generateObstacles();
        timer.setDelay(difficulty.getDelay());
        timer.start();
        repaint();
    }

    // Unused KeyListener methods
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        // Handle arrow key inputs
        int key = e.getKeyCode();
        switch (key) {
            case KeyEvent.VK_UP:
                setDirection(Direction.UP);
                break;
            case KeyEvent.VK_DOWN:
                setDirection(Direction.DOWN);
                break;
            case KeyEvent.VK_LEFT:
                setDirection(Direction.LEFT);
                break;
            case KeyEvent.VK_RIGHT:
                setDirection(Direction.RIGHT);
                break;
        }
    }
}