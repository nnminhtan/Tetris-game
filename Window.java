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

    public Window(String clientName) throws IOException { // Accept client name as a parameter
        connectToServer(clientName);
        userName = clientName;
        setupRoomSelectionUI();
    }

    private void connectToServer(String clientName) throws IOException {
        System.out.println("Connecting to server...");
        Socket socket = new Socket("172.16.195.132", 8888); // Server IP and port
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pw = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to server!");

        // Send the client's name to the server
        pw.println(clientName);

        // Listen to server messages in a separate thread
        // new Thread(() -> {
        //     try {
        //         String message;
        //         while ((message = br.readLine()) != null) {
        //             System.out.println("Server: " + message);
        //         }
        //     } catch (IOException e) {
        //         System.out.println("Disconnected from server.");
        //     }
        // }).start();
        new Thread(() -> {
            try {
                String message;
                while ((message = br.readLine()) != null) {
                    if (message.equals("START_MATCH")) {
                        SwingUtilities.invokeLater(() -> {
                            JFrame tetrisFrame = new JFrame("Tetris Game");
                            TetrisPanel tetrisPanel = new TetrisPanel(numOfPlayers, userName); // Initialize game
                            tetrisFrame.add(tetrisPanel);
                            tetrisFrame.setSize(800, 600);
                            tetrisFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            tetrisFrame.setVisible(true);
                        });
                    } else {
                        System.out.println("Server: " + message);
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