import java.io.IOException;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Start as Server or Client? (s/c): ");
        String mode = scanner.nextLine().trim().toLowerCase();

        if (mode.equals("s")) {
            System.out.println("Starting as server...");
            new GameServer(); // Start the server
        } else if (mode.equals("c")) {
            System.out.println("Starting as client...");
            new Window(); // Start the client window
        } else {
            System.out.println("Invalid choice. Exiting...");
        }
    }
}

