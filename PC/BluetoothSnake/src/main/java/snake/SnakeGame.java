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
    private static final int GRID_SIZE = 20;
    private static final int GAME_WIDTH = 400;
    private static final int GAME_HEIGHT = 400;
    private final LinkedList<Point> snake;
    private final Timer timer;
    private final SnakeApp app;
    private Direction currentDirection = Direction.RIGHT;
    private Point food;
    private int score = 0;
    private boolean gameOver = false;

    public SnakeGame(SnakeApp app) {
        this.app = app;
        setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        snake = new LinkedList<>();
        snake.add(new Point(GAME_WIDTH / 2, GAME_HEIGHT / 2));
        generateFood();
        timer = new Timer(100, this);
        timer.start();
    }

    private void move() {
        if (gameOver) return;
        Point head = snake.getFirst();
        Point newHead = new Point(head);
        switch (currentDirection) {
            case UP -> newHead.y -= GRID_SIZE;
            case DOWN -> newHead.y += GRID_SIZE;
            case LEFT -> newHead.x -= GRID_SIZE;
            case RIGHT -> newHead.x += GRID_SIZE;
        }
        if (newHead.x < 0) newHead.x = GAME_WIDTH - GRID_SIZE;
        else if (newHead.x >= GAME_WIDTH) newHead.x = 0;
        if (newHead.y < 0) newHead.y = GAME_HEIGHT - GRID_SIZE;
        else if (newHead.y >= GAME_HEIGHT) newHead.y = 0;
        if (snake.contains(newHead)) {
            gameOver = true;
            app.setGameState(GameState.GAME_OVER);
            return;
        }
        snake.addFirst(newHead);
        if (newHead.equals(food)) {
            score++;
            generateFood();
        } else {
            snake.removeLast();
        }
    }

    private boolean isOnInitialLine(Point point, Point startingPoint) {
        return point.y == startingPoint.y;
    }

    private void generateFood() {
        Random random = new Random();
        Point startingPoint = new Point(GAME_WIDTH / 2, GAME_HEIGHT / 2);
        int x, y;
        Point newFood;
        do {
            x = random.nextInt(GAME_WIDTH / GRID_SIZE) * GRID_SIZE;
            y = random.nextInt(GAME_HEIGHT / GRID_SIZE) * GRID_SIZE;
            newFood = new Point(x, y);
        } while (snake.contains(newFood) || isOnInitialLine(newFood, startingPoint));
        food = new Point(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            String gameOverText = "Game Over! Score: " + score;
            g.drawString(gameOverText, (GAME_WIDTH - g.getFontMetrics().stringWidth(gameOverText)) / 2, GAME_HEIGHT / 2);
            return;
        }
        g.setColor(Color.GREEN);
        for (Point p : snake) {
            g.fillRect(p.x, p.y, GRID_SIZE, GRID_SIZE);
        }
        g.setColor(Color.RED);
        g.fillRect(food.x, food.y, GRID_SIZE, GRID_SIZE);
        g.setColor(Color.GRAY);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 10, 20);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
    }

    public void setDirection(Direction newDirection) {
        if ((currentDirection == Direction.UP && newDirection != Direction.DOWN) ||
                (currentDirection == Direction.DOWN && newDirection != Direction.UP) ||
                (currentDirection == Direction.LEFT && newDirection != Direction.RIGHT) ||
                (currentDirection == Direction.RIGHT && newDirection != Direction.LEFT)) {
            currentDirection = newDirection;
        }
    }

    public void resetGame() {
        snake.clear();
        snake.add(new Point(GAME_WIDTH / 2, GAME_HEIGHT / 2));
        currentDirection = Direction.RIGHT;
        score = 0;
        gameOver = false;
        generateFood();
        timer.setDelay(100);
        timer.start();
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
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

    @Override
    public void keyReleased(KeyEvent e) {
    }
}