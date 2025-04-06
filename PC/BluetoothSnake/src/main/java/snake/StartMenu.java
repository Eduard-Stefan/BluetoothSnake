package snake;

import javax.swing.*;
import java.awt.*;

public class StartMenu extends JPanel {
    // Label to display the current Bluetooth connection status
    private final JLabel bluetoothStatusLabel;

    // Constructs the StartMenu panel with game controls.
    public StartMenu(SnakeApp app) {
        // Set the preferred size of the menu panel
        setPreferredSize(new Dimension(400, 400));

        // Use GridLayout with 3 rows and 1 column for vertical stacking
        setLayout(new GridLayout(3, 1));

        // Create and configure the Start Game button
        JButton startButton = new JButton("Start Game");
        // Add action listener to start the game when clicked
        startButton.addActionListener(e -> app.startGame());

        // Create and configure the Difficulty button
        JButton difficultyButton = new JButton("Difficulty: EASY");
        // Add specialized listener to handle difficulty cycling
        difficultyButton.addActionListener(new DifficultyButtonListener(app, difficultyButton));

        // Initialize and configure the Bluetooth status label
        bluetoothStatusLabel = new JLabel();
        bluetoothStatusLabel.setHorizontalAlignment(SwingConstants.CENTER); // Center-align text

        // Add components to the panel in order
        add(startButton);
        add(difficultyButton);
        add(bluetoothStatusLabel);
    }

    // Updates the Bluetooth status text displayed on the menu.
    public void updateBluetoothStatus(String status) {
        bluetoothStatusLabel.setText(status);
    }
}