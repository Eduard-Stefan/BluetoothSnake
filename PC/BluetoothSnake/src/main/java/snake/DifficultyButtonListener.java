package snake;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

public class DifficultyButtonListener implements ActionListener {
    private final SnakeApp app;
    private final JButton difficultyButton;

    public DifficultyButtonListener(SnakeApp app, JButton difficultyButton) {
        this.app = app;
        this.difficultyButton = difficultyButton;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Difficulty currentDifficulty = app.getDifficulty();
        Difficulty newDifficulty;
        switch (currentDifficulty) {
            case EASY -> newDifficulty = Difficulty.MEDIUM;
            case MEDIUM -> newDifficulty = Difficulty.HARD;
            default -> newDifficulty = Difficulty.EASY;
        }
        app.setDifficulty(newDifficulty);
        difficultyButton.setText("Difficulty: " + newDifficulty);
    }
}