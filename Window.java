import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.DefaultListModel;

public class Window extends Frame {
    private static final long serialVersionUID = -1324363758675184283L;
    private BufferedReader br;
    private PrintWriter pw;
    private String userName;
    private TetrisPanel gamePanel;
    private Panel roomPanel;
    private List<String> roomPlayers = new ArrayList<>();
    private String currentRoom;

    public Window(String clientName) throws IOException {
        this.userName = clientName;
        connectToServer(clientName);
        setupRoomSelectionUI();
    }

    private void connectToServer(String clientName) throws IOException {
        Socket socket = new Socket("localhost", 8888);
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pw = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to server!");

        pw.println(clientName);

        // Start listening for server messages
        new Thread(this::handleServerMessages).start();
    }

    private void handleServerMessages() {
        try {
            String message;
            while ((message = br.readLine()) != null) {
                final String finalMessage = message;
                if (message.startsWith("ROOM_LIST:")) {
                    SwingUtilities.invokeLater(() -> {
                        System.out.println("Received room list: " + finalMessage);
                        String[] rooms = finalMessage.substring(10).split(",");
                        updateRoomList(rooms);
                    });
                }
                if (message.startsWith("ROOM_CREATED:")) {
                    System.out.println("Room created: " + message.split(":")[1]);
                } else if (message.startsWith("JOINED_ROOM:")) {
                    System.out.println("Joined room: " + message.split(":")[1]);
                } else if (message.startsWith("START_GAME")) {
                    startGame();
                } else if (message.startsWith("ADD_GARBAGE:")) {
                    handleGarbageLines(message);
                } else if (message.startsWith("PLAYER_DISCONNECTED:")) {
                    handlePlayerDisconnect(message);
                } else if (message.startsWith("GRID_STATE:")) {
                    updateOpponentGrid(message);
                } else if (message.startsWith("ROOM_INFO:")) {
                    String[] parts = message.split(":");
                    String roomName = parts[1];
                    String opponentName = parts[2];
                    int opponentScore = Integer.parseInt(parts[3]);
                    int opponentLines = Integer.parseInt(parts[4]);
                    int opponentLevel = Integer.parseInt(parts[5]);
                    if (gamePanel != null) {
                        gamePanel.updateOpponentInfo(opponentName, opponentScore, opponentLines, opponentLevel);
                    }
                } else if (message.startsWith("SCORE_UPDATE:")) {
                    String[] parts = message.split(":");
                    String senderName = parts[1];
                    int score = Integer.parseInt(parts[2]);
                    int lines = Integer.parseInt(parts[3]);
                    int level = Integer.parseInt(parts[4]);

                    if (gamePanel != null && !senderName.equals(userName)) {
                        gamePanel.updateOpponentInfo(senderName, score, lines, level);
                        System.out.println("Received score from " + senderName + ": " + score);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateRoomList(String[] rooms) {
        for (java.awt.Window window : java.awt.Window.getWindows()) {
            for (java.awt.Window dialog : window.getOwnedWindows()) {
                if (dialog instanceof JDialog && ((JDialog) dialog).getTitle().equals("Available Rooms")) {
                    JDialog roomDialog = (JDialog) dialog;
                    for (Component comp : roomDialog.getContentPane().getComponents()) {
                        if (comp instanceof JScrollPane) {
                            JScrollPane scrollPane = (JScrollPane) comp;
                            if (scrollPane.getViewport().getView() instanceof JList) {
                                JList<String> roomList = (JList<String>) scrollPane.getViewport().getView();
                                DefaultListModel<String> model = (DefaultListModel<String>) roomList.getModel();
                                model.clear();
                                for (String room : rooms) {
                                    if (!room.isEmpty()) {
                                        model.addElement(room);
                                        System.out.println("Added room to list: " + room);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void setupRoomSelectionUI() {
        setTitle("Tetris Battle - " + userName);
        setSize(400, 300);

        roomPanel = new Panel();
        roomPanel.setLayout(new GridLayout(0, 1));

        Button createRoomBtn = new Button("Create Room");
        createRoomBtn.addActionListener(e -> createRoom());

        Button joinRoomBtn = new Button("Join Room");
        joinRoomBtn.addActionListener(e -> joinRoom());

        roomPanel.add(createRoomBtn);
        roomPanel.add(joinRoomBtn);

        add(roomPanel);
        setVisible(true);
    }

    private void createRoom() {
        String roomName = "Room_" + userName + "_" + System.currentTimeMillis();
        pw.println("CREATE " + roomName);
        currentRoom = roomName;
    }

    private void joinRoom() {
        JDialog dialog = new JDialog(this, "Available Rooms", true);
        dialog.setSize(300, 400);
        dialog.setLayout(new BorderLayout());
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> roomList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(roomList);
        
        JPanel buttonsPanel = new JPanel();
        JButton refreshButton = new JButton("Refresh");
        JButton joinButton = new JButton("Join");
        JButton cancelButton = new JButton("Cancel");
        
        refreshButton.addActionListener(e -> pw.println("GET_ROOMS"));
        
        joinButton.addActionListener(e -> {
            String selectedRoom = roomList.getSelectedValue();
            if (selectedRoom != null) {
                pw.println("JOIN " + selectedRoom);
                currentRoom = selectedRoom;
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please select a room");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonsPanel.add(refreshButton);
        buttonsPanel.add(joinButton);
        buttonsPanel.add(cancelButton);
        
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        // Request initial room list
        pw.println("GET_ROOMS");
        
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void startGame() {
        remove(roomPanel);
        String[] players = { userName, getOpponentName() }; // Lấy tên đối thủ thực tế
        gamePanel = new TetrisPanel(2, players, pw, currentRoom); // Truyền tên phòng
        add(gamePanel);
        gamePanel.requestFocusInWindow();
        validate();
        repaint();
    }

    private void handleGarbageLines(String message) {
        if (gamePanel != null) {
            String[] parts = message.split(":");
            String sender = parts[1];
            int lines = Integer.parseInt(parts[2]);
            if (!sender.equals(userName)) {
                gamePanel.screens[0].addGarbageLines(lines);
            }
        }
    }

    private void handlePlayerDisconnect(String message) {
        String disconnectedPlayer = message.split(":")[1];
        if (gamePanel != null) {
            SwingUtilities.invokeLater(() -> {
                gamePanel.screens[1].isGameOver = true;
                gamePanel.repaint();
            });
        }
    }

    private void updateOpponentGrid(String message) {
        String[] parts = message.split(":");
        if (parts.length > 1) {
            String[] gridValues = parts[1].split(",");
            int[][] opponentGrid = new int[22][10];
            for (int i = 0; i < 22; i++) {
                for (int j = 0; j < 10; j++) {
                    opponentGrid[i][j] = Integer.parseInt(gridValues[i * 10 + j]);
                }
            }
            // Cập nhật giao diện người dùng với lưới của đối thủ
            gamePanel.screens[1].setOpponentGrid(opponentGrid);
            gamePanel.repaint(); // Vẽ lại giao diện
        }
    }

    private String getOpponentName() {
        if (currentRoom != null) {
            String[] parts = currentRoom.split("_");
            if (parts.length > 1) {
                String roomOwner = parts[1];
                return roomOwner.equals(userName) ? "Waiting..." : roomOwner;
            }
        }
        return "Unknown";
    }
}