package snake;

import javax.swing.*;
import java.awt.*;

public class StartMenu extends JPanel {
    public StartMenu(SnakeApp app) {
        setPreferredSize(new Dimension(400, 400));
        setLayout(new GridLayout(3, 1));
        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(e -> app.startGame());
        add(startButton);
    }
}