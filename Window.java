import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
public class Window extends Frame {
	private static final long serialVersionUID = -1324363758675184283L;
	// private BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	// private StringTokenizer st;
	private BufferedReader br;
    private PrintWriter pw;
	private int numOfPlayers;
	
	public Window() throws IOException {
        connectToServer();
        setupRoomSelectionUI();
    }

    private void connectToServer() throws IOException {
        System.out.println("Connecting to server...");
        Socket socket = new Socket("172.16.195.132", 8888); // Server IP and port
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pw = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to server!");

        // Listen to server messages in a separate thread
        new Thread(() -> {
            try {
                String message;
                while ((message = br.readLine()) != null) {
                    System.out.println("Server: " + message);
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
	// Window () throws IOException {
	// 	System.out.println("Enter number of players: ");
	// 	numOfPlayers = readInt();
	// 	setTitle("Tetris");
	// 	setSize(400*numOfPlayers, 600);
	// 	setLocation(100, 100);
	// 	setResizable(false);
	// 	add(new TetrisPanel(numOfPlayers));
	// 	setVisible(true);
	// }
	// private String next () throws IOException {
	// 	while (st == null || !st.hasMoreTokens())
	// 		st = new StringTokenizer(br.readLine().trim());
	// 	return st.nextToken();
	// }
	// private int readInt() throws IOException {
	// 	return Integer.parseInt(next());
	// }
}
