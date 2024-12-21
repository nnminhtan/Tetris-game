import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;

public class GameServer {
    private static final int PORT = 8888; // Server port
    private static ConcurrentHashMap<String, List<ClientHandler>> rooms = new ConcurrentHashMap<>();

    public GameServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is running on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());
            new ClientHandler(clientSocket, null).start(); // Pass the JFrame to the ClientHandler
        }
    }

    public static void main(String[] args) {
        try {
            new GameServer(); // Start the server
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private String clientName;
        private String currentRoom;
        private PrintWriter out;

        // public ClientHandler(Socket socket) {
        //     this.socket = socket;
        // }
        
        public ClientHandler(Socket socket, String clientName) {
            this.socket = socket;
            this.clientName = clientName;
            try {
                this.out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // @Override
        // public void run() {
        //     try (
        //         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        //         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        //     ) {
        //         this.out = out;

        //         // Initial handshake
        //         out.println("Welcome to the Tetris server!");
        //         out.println("Enter your name:");
        //         clientName = in.readLine();

        //         // Ensure client has entered a name
        //         while (clientName == null || clientName.trim().isEmpty()) {
        //             out.println("Name cannot be empty. Please enter your name:");
        //             clientName = in.readLine();
        //         }

        //         // Proceed to command input
        //         while (true) {
        //             out.println("Enter a command: CREATE <room>, JOIN <room>, or LIST");
        //             String command = in.readLine();
        //             if (command.startsWith("CREATE")) {
        //                 String roomName = command.split(" ")[1];
        //                 createRoom(roomName);
                    // } else if (command.startsWith("JOIN")) {
                    //     String roomName = command.split(" ")[1];
                    //     joinRoom(roomName);
                    // } else if (command.equals("LIST")) {
                    //     listRooms();
        //             } else {
        //                 out.println("Invalid command.");
        //             }
        //         }
        //     } catch (IOException e) {
        //         e.printStackTrace();
        //     } finally {
        //         leaveRoom();
        //         System.out.println(clientName + " disconnected.");
        //     }
        // }
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                this.out = out;

                out.println("Welcome to the Tetris server!");
                out.println("Enter your name:");
                clientName = in.readLine();

                while (true) {
                    String command = in.readLine();
                    if (command.startsWith("START_MATCH")) {
                        if (currentRoom != null && rooms.get(currentRoom).size() > 1) {
                            broadcastToRoom(currentRoom, "START_MATCH");
                        } else {
                            out.println("Not enough players to start the match.");
                        }
                    } else if (command.startsWith("CREATE")) {
                        String roomName = command.split(" ")[1];
                        createRoom(roomName);
                    } else if (command.startsWith("JOIN")) {
                        String roomName = command.split(" ")[1];
                        joinRoom(roomName);
                    } else if (command.equals("LIST")) {
                        listRooms();
                    } else {
                        out.println("Invalid command.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void createRoom(String roomName) {
            rooms.putIfAbsent(roomName, new ArrayList<>());
            out.println("Room " + roomName + " created.");
        }

        // private void joinRoom(String roomName) {
        //     if (!rooms.containsKey(roomName)) {
        //         out.println("Room " + roomName + " does not exist.");
        //         return;
        //     }
        //     leaveRoom(); // Leave the current room if already in one
        //     currentRoom = roomName;
        //     rooms.get(roomName).add(this);
        //     out.println("Joined room " + roomName);

        //     // Open RoomManager
        //     SwingUtilities.invokeLater(() -> {
        //         List<String> playersInRoom = getPlayersInRoom(roomName);
        //         new RoomManager(roomName, playersInRoom); // Pass room name and players
        //     });
        // }
        private void joinRoom(String roomName) {
            if (!rooms.containsKey(roomName)) {
                out.println("Room " + roomName + " does not exist.");
                return;
            }
        
            leaveRoom(); // Leave the current room if already in one
            currentRoom = roomName;
            rooms.get(roomName).add(this);
            out.println("Joined room " + roomName);
        
            // Notify all players in the room about the updated player list
            // broadcastToRoom(currentRoom, "Players in room: " + getPlayersInRoom(currentRoom));
            broadcastPlayerList(roomName);

        }
        private void broadcastToRoom(String roomName, String message) {
            for (ClientHandler handler : rooms.get(roomName)) {
                handler.out.println(message);
            }
        }
        private List<String> getPlayersInRoom(String roomName) {
            // Return the list of players in the specified room
            return rooms.get(roomName).stream()
                    .map(handler -> handler.clientName) // Assuming clientName is accessible
                    .collect(Collectors.toList());
        }
        private void broadcastPlayerList(String roomName) {
            List<ClientHandler> handlers = rooms.get(roomName);
            if (handlers != null) {
                String playerList = handlers.stream()
                    .map(handler -> handler.clientName) // Extract names
                    .collect(Collectors.joining(", ")); // Join into a single string
        
                for (ClientHandler handler : handlers) {
                    handler.out.println("Players in room: " + playerList); // Notify clients
                }
            }
        }

        private void listRooms() {
            out.println("Available rooms:");
            for (String room : rooms.keySet()) {
                out.println(" - " + room + " (" + rooms.get(room).size() + " players)");
            }
        }

        private void leaveRoom() {
            if (currentRoom != null) {
                rooms.get(currentRoom).remove(this);
                if (rooms.get(currentRoom).isEmpty()) {
                    rooms.remove(currentRoom);
                }
                currentRoom = null;
            }
        }
    }
}
