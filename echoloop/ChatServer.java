import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    public static final int PORT = 5000;

    private static final Map<String, ClientHandler> onlineUsers =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {

        System.out.println(
                "EchoLoop server started on port " + PORT
        );

        try (
                ServerSocket serverSocket =
                        new ServerSocket(PORT)
        ) {

            System.out.println(
                    "Waiting for clients..."
            );

            while (true) {

                Socket socket =
                        serverSocket.accept();

                System.out.println(
                        "New client connected: "
                                + socket.getInetAddress()
                );

                ClientHandler clientHandler =
                        new ClientHandler(socket);

                new Thread(clientHandler).start();
            }

        } catch (IOException exception) {

            System.out.println(
                    "Server error: "
                            + exception.getMessage()
            );
        }
    }

   
    public static boolean addOnlineUser(
            String username,
            ClientHandler client) {

        return onlineUsers.putIfAbsent(
                username,
                client
        ) == null;
    }

   
    public static void removeOnlineUser(
            String username) {

        if (username != null) {
            onlineUsers.remove(username);
        }
    }

   
    public static void sendToUser(
            String username,
            String message) {

        ClientHandler receiver =
                onlineUsers.get(username);

        if (receiver != null) {
            receiver.sendMessage(message);
        }
    }
}