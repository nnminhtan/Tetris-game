import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

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
                    if (gamePanel != null) {
                        gamePanel.updateOpponentInfo(opponentName, opponentScore);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        String roomName = javax.swing.JOptionPane.showInputDialog("Enter room name");
        if (roomName != null && !roomName.trim().isEmpty()) {
            pw.println("JOIN " + roomName);
            currentRoom = roomName;
        }
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