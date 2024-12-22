import java.awt.*;
import java.io.*;
import java.net.Socket;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Window extends Frame {
    private static final long serialVersionUID = -1324363758675184283L;
    private BufferedReader br;
    private PrintWriter pw;
    private int numOfPlayers;
    private String userName;

    public Window(String clientName) throws IOException {
        connectToServer(clientName);
        userName = clientName;
        setupRoomSelectionUI();
    }

    private void connectToServer(String clientName) throws IOException {
        System.out.println("Connecting to server...");
        Socket socket = new Socket("192.168.0.102", 8888);
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pw = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to server!");

        pw.println(clientName);

        // Create final reference for TetrisPanel
        final TetrisPanel[] gamePanelRef = new TetrisPanel[1];

        new Thread(() -> {
            try {
                String message;
                while ((message = br.readLine()) != null) {
                    if (message.startsWith("START_MATCH:")) {
                        String[] parts = message.split(":");
                        numOfPlayers = Integer.parseInt(parts[1]);
                        String opponentName = parts[2];
                        SwingUtilities.invokeLater(() -> {
                            JFrame tetrisFrame = new JFrame("Tetris Game - " + clientName);
                            TetrisPanel tetrisPanel = new TetrisPanel(numOfPlayers, clientName, pw);
                            gamePanelRef[0] = tetrisPanel;
                            tetrisFrame.add(tetrisPanel);
                            tetrisFrame.setSize(800, 600);
                            tetrisFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            tetrisFrame.setVisible(true);
                            tetrisPanel.requestFocusInWindow();
                        });
                    } else if (message.startsWith("OPPONENT_STATE:")) {
                        final String gameState = message.substring("OPPONENT_STATE:".length());
                        if (gamePanelRef[0] != null) {
                            SwingUtilities.invokeLater(() -> {
                                gamePanelRef[0].updateOpponentState(gameState);
                            });
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Disconnected from server.");
            }
        }).start();
    }

    private void setupRoomSelectionUI() {
        setTitle("Tetris Room Selector");
        setSize(400, 300);
        setLayout(new GridLayout(3, 1));

        Label label = new Label("Enter a command: CREATE <room>, JOIN <room>, or LIST");
        add(label);

        TextField commandField = new TextField();
        add(commandField);

        Button sendButton = new Button("Send");
        add(sendButton);

        sendButton.addActionListener(e -> {
            String command = commandField.getText();
            pw.println(command); // Send the command to the server
            commandField.setText(""); // Clear the input field
        });

        setVisible(true);
    }
}