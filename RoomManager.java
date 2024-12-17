import java.awt.*;
import java.util.List;
import javax.swing.*;

public class RoomManager extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JPanel roomInfoPanel;

    public RoomManager(String roomName, List<String> players) {
        setTitle("Room: " + roomName);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Close this window only

        roomInfoPanel = createRoomInfoPanel(roomName, players);
        add(roomInfoPanel);

        setVisible(true);
    }

    private JPanel createRoomInfoPanel(String roomName, List<String> players) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(new JLabel("Room: " + roomName));
        panel.add(new JLabel("Players in this room:"));

        for (String player : players) {
            panel.add(new JLabel(player));
        }

        // Play Button
        JButton playButton = new JButton("Play");
        playButton.addActionListener(e -> {
            // Open TetrisPanel when Play button is clicked
            JFrame tetrisFrame = new JFrame("Tetris Game");
            TetrisPanel tetrisPanel = new TetrisPanel(players.size()); // Pass the number of players
            tetrisFrame.add(tetrisPanel);
            tetrisFrame.setSize(800, 600); // Set size for the Tetris game window
            tetrisFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            tetrisFrame.setVisible(true);
        });
        panel.add(playButton);

        // Back Button
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> {
            dispose(); // Close the RoomManager window
            GameServer.main(null); // Call the main method of GameServer to reopen it
        });
        panel.add(backButton);

        return panel;
    }
}
