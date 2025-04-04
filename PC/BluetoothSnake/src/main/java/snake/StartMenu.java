package snake;

import javax.swing.*;
import java.awt.*;

public class StartMenu extends JPanel {
    private final JLabel bluetoothStatusLabel;

    public StartMenu(SnakeApp app) {
        setPreferredSize(new Dimension(400, 400));
        setLayout(new GridLayout(3, 1));
        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(e -> app.startGame());
        JButton difficultyButton = new JButton("Difficulty: EASY");
        difficultyButton.addActionListener(new DifficultyButtonListener(app, difficultyButton));
        bluetoothStatusLabel = new JLabel();
        bluetoothStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(startButton);
        add(difficultyButton);
        add(bluetoothStatusLabel);
    }

    public void updateBluetoothStatus(String status) {
        bluetoothStatusLabel.setText(status);
    }
}