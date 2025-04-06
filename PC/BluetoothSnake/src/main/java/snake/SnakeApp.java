package snake;

import javax.swing.*;
import java.awt.*;

public class SnakeApp extends JFrame {
    // CardLayout for switching between different views (menu/game)
    private final CardLayout cardLayout;
    // Main panel that holds all views
    private final JPanel mainPanel;
    // The start menu component
    private final StartMenu startMenu;
    // Current game instance
    public SnakeGame game;
    // Current state of the game
    public GameState gameState = GameState.MENU;
    // Current difficulty setting
    private Difficulty difficulty = Difficulty.EASY;

    // Constructs the main application window and initializes components.
    public SnakeApp() {
        // Set up the main window
        setTitle("Bluetooth Snake");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Initialize the card layout for view switching
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Create and add the start menu
        startMenu = new StartMenu(this);
        mainPanel.add(startMenu, "startMenu");

        // Add main panel to frame and pack
        add(mainPanel);
        pack();

        // Center the window on screen
        setLocationRelativeTo(null);
        setVisible(true);

        // Start the Bluetooth server in a separate thread
        BluetoothServer server = new BluetoothServer(this);
        Thread serverThread = new Thread(server);
        serverThread.start();
    }

    // Main entry point for the application.
    public static void main(String[] args) {
        // Ensure GUI creation happens on the Event Dispatch Thread
        SwingUtilities.invokeLater(SnakeApp::new);
    }

    // Starts a new game session.
    public void startGame() {
        // Only create new game if none exists or previous game ended
        if (game == null || gameState == GameState.GAME_OVER) {
            game = new SnakeGame(this, difficulty);
            mainPanel.add(game, "game");
        }

        // Show the game panel and update state
        cardLayout.show(mainPanel, "game");
        gameState = GameState.GAME;

        // Ensure game has focus for keyboard input
        game.requestFocusInWindow();
        game.resetGame();
    }

    // Shows the start menu view.
    public void showStartMenu() {
        cardLayout.show(mainPanel, "startMenu");
        gameState = GameState.MENU;
    }

    // Sets the game state and handles state transitions.
    public void setGameState(GameState state) {
        gameState = state;

        // Special handling for game over state
        if (gameState == GameState.GAME_OVER) {
            game = null; // Clear current game instance

            // Show play again dialog
            int choice = JOptionPane.showConfirmDialog(this, "Play Again?", "Game Over", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                startGame();
            } else {
                showStartMenu();
            }
        }
    }

    // Gets the current difficulty setting.
    public Difficulty getDifficulty() {
        return difficulty;
    }

    // Sets the game difficulty.
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    // Updates the Bluetooth status display in the start menu.
    public void updateBluetoothStatus(String status) {
        startMenu.updateBluetoothStatus(status);
    }
}