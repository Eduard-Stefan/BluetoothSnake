package snake;

import javax.swing.*;
import java.awt.*;

public class SnakeApp extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final StartMenu startMenu;
    public SnakeGame game;
    public GameState gameState = GameState.MENU;
    private Difficulty difficulty = Difficulty.EASY;

    public SnakeApp() {
        setTitle("Bluetooth Snake");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        startMenu = new StartMenu(this);
        mainPanel.add(startMenu, "startMenu");
        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        BluetoothServer server = new BluetoothServer(this);
        Thread serverThread = new Thread(server);
        serverThread.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SnakeApp::new);
    }

    public void startGame() {
        if (game == null || gameState == GameState.GAME_OVER) {
            game = new SnakeGame(this, difficulty);
            mainPanel.add(game, "game");
        }
        cardLayout.show(mainPanel, "game");
        gameState = GameState.GAME;
        game.requestFocusInWindow();
        game.resetGame();
    }

    public void showStartMenu() {
        cardLayout.show(mainPanel, "startMenu");
        gameState = GameState.MENU;
    }

    public void setGameState(GameState state) {
        gameState = state;
        if (gameState == GameState.GAME_OVER) {
            game = null;
            int choice = JOptionPane.showConfirmDialog(this, "Play Again?", "Game Over", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                startGame();
            } else {
                showStartMenu();
            }
        }
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void updateBluetoothStatus(String status) {
        startMenu.updateBluetoothStatus(status);
    }
}