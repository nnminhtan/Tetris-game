// import javax.swing.*;
// import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
// import java.io.IOException;

// public class RoomManager extends JFrame {
//     private CardLayout cardLayout;
//     private JPanel mainPanel;
//     private JPanel roomSelectionPanel;
//     private JPanel gamePanel;

//     public RoomManager() {
//         setTitle("Tetris Room Manager");
//         setSize(800, 600);
//         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

//         cardLayout = new CardLayout();
//         mainPanel = new JPanel(cardLayout);

//         // Room Selection UI
//         roomSelectionPanel = createRoomSelectionPanel();
//         mainPanel.add(roomSelectionPanel, "RoomSelection");

//         // Placeholder for Tetris Game Panel
//         gamePanel = new JPanel();
//         gamePanel.setBackground(Color.BLACK);
//         mainPanel.add(gamePanel, "GamePanel");

//         add(mainPanel);
//         cardLayout.show(mainPanel, "RoomSelection");
//     }

//     private JPanel createRoomSelectionPanel() {
//         JPanel panel = new JPanel(new BorderLayout());

//         // Room List
//         DefaultListModel<String> roomListModel = new DefaultListModel<>();
//         JList<String> roomList = new JList<>(roomListModel);
//         roomListModel.addElement("Room 1");
//         roomListModel.addElement("Room 2");
//         roomListModel.addElement("Room 3");

//         // Add Room Button
//         JButton createRoomButton = new JButton("Create Room");
//         createRoomButton.addActionListener(e -> {
//             String newRoom = JOptionPane.showInputDialog(this, "Enter Room Name:");
//             if (newRoom != null && !newRoom.trim().isEmpty()) {
//                 roomListModel.addElement(newRoom);
//             }
//         });

//         // Join Room Button
//         JButton joinRoomButton = new JButton("Join Room");
//         joinRoomButton.addActionListener(e -> {
//             String selectedRoom = roomList.getSelectedValue();
//             if (selectedRoom != null) {
//                 // Transition to Game Panel
//                 try {
//                     startGame(selectedRoom);
//                 } catch (IOException e1) {
//                     // TODO Auto-generated catch block
//                     e1.printStackTrace();
//                 }
//             } else {
//                 JOptionPane.showMessageDialog(this, "Please select a room to join.");
//             }
//         });

//         // Layout
//         JPanel buttonPanel = new JPanel();
//         buttonPanel.add(createRoomButton);
//         buttonPanel.add(joinRoomButton);

//         panel.add(new JScrollPane(roomList), BorderLayout.CENTER);
//         panel.add(buttonPanel, BorderLayout.SOUTH);

//         return panel;
//     }

//     // private void startGame(String roomName) {
//     //     // Initialize TetrisPanel with room-specific data
//     //     gamePanel.removeAll();
//     //     TetrisPanel tetrisPanel = new TetrisPanel(2); // Example: 2 players
//     //     gamePanel.add(tetrisPanel);
//     //     cardLayout.show(mainPanel, "GamePanel");

//     //     // Start the game
//     //     tetrisPanel.requestFocusInWindow();
//     // }
//     private void startGame(String roomName) throws IOException {
//         int numOfPlayers = 2; // Example: Use actual logic based on room configuration
//         Window gameWindow = new Window(numOfPlayers);
//         gameWindow.setVisible(true);
//         dispose(); // Close the RoomManager window
//     }
    

//     public static void main(String[] args) {
//         SwingUtilities.invokeLater(() -> {
//             RoomManager manager = new RoomManager();
//             manager.setVisible(true);
//         });
//     }
// }
