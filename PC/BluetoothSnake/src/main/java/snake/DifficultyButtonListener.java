package snake;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

public class DifficultyButtonListener implements ActionListener {
    // Reference to the main application for difficulty updates
    private final SnakeApp app;
    // Reference to the difficulty button for text updates
    private final JButton difficultyButton;

    // Constructs a new DifficultyButtonListener.
    public DifficultyButtonListener(SnakeApp app, JButton difficultyButton) {
        this.app = app;
        this.difficultyButton = difficultyButton;
    }

    // Handles button click events to cycle through difficulty levels.
    @Override
    public void actionPerformed(ActionEvent e) {
        // Get current difficulty from the application
        Difficulty currentDifficulty = app.getDifficulty();
        Difficulty newDifficulty;

        // Determine next difficulty level in the cycle
        switch (currentDifficulty) {
            case EASY -> newDifficulty = Difficulty.MEDIUM;
            case MEDIUM -> newDifficulty = Difficulty.HARD;
            default -> newDifficulty = Difficulty.EASY; // Wraps around from HARD to EASY
        }

        // Update application with new difficulty
        app.setDifficulty(newDifficulty);

        // Update button text to reflect new difficulty
        difficultyButton.setText("Difficulty: " + newDifficulty);
    }
}