import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.function.Consumer;


public class ChatClient {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    
    private Consumer<String> messageListener;

    
    public void connect(String username) throws IOException {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty."
            );
        }

        socket = new Socket("localhost", 5000);

        reader = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.UTF_8
                )
        );

        writer = new PrintWriter(
                socket.getOutputStream(),
                true,
                StandardCharsets.UTF_8
        );

        
        writer.println(
                "LOGIN|" + username
        );

        
        String response = reader.readLine();

        if (response == null) {

            disconnect();

            throw new IOException(
                    "No response from server."
            );
        }

        if (!response.startsWith("LOGIN_OK")) {

            disconnect();

            throw new IOException(
                    response
            );
        }

        System.out.println(
                "Connected: " + response
        );

        
        Thread listenerThread =
                new Thread(
                        this::listenForMessages,
                        "ChatClient-Listener"
                );

        listenerThread.setDaemon(true);

        listenerThread.start();
    }

    
    public void sendMessage(
            String receiverUsername,
            String messageText) {

        if (writer == null) {

            System.out.println(
                    "Not connected to server."
            );

            return;
        }

        if (receiverUsername == null
                || receiverUsername.isBlank()) {

            System.out.println(
                    "Receiver username cannot be empty."
            );

            return;
        }

        if (messageText == null
                || messageText.isBlank()) {

            System.out.println(
                    "Message cannot be empty."
            );

            return;
        }

        writer.println(
                "SEND|"
                        + receiverUsername
                        + "|"
                        + messageText
        );
    }

    
    public void setMessageListener(
            Consumer<String> listener) {

        this.messageListener = listener;
    }

   
    private void listenForMessages() {

        try {

            String message;

            while (
                    socket != null
                            && !socket.isClosed()
                            && (message = reader.readLine()) != null
            ) {

                System.out.println(
                        "Server: " + message
                );

                
                if (messageListener != null) {

                    messageListener.accept(
                            message
                    );
                }
            }

        } catch (IOException exception) {

            
            if (socket != null
                    && !socket.isClosed()) {

                System.out.println(
                        "Disconnected from server."
                );

                if (messageListener != null) {

                    messageListener.accept(
                            "ERROR|Disconnected from server."
                    );
                }
            }
        }
    }

    
    public void disconnect() {

        try {

            if (socket != null
                    && !socket.isClosed()) {

                socket.close();
            }

        } catch (IOException exception) {

            System.out.println(
                    "Error closing connection: "
                            + exception.getMessage()
            );

        } finally {

            socket = null;
            reader = null;
            writer = null;
        }
    }

   
    public static void main(
            String[] args) {

        if (args.length != 1) {

            System.out.println(
                    "Run using: ChatClient username"
            );

            return;
        }

        try {

            ChatClient client =
                    new ChatClient();

            client.setMessageListener(
                    message ->
                            System.out.println(
                                    "Received: "
                                            + message
                            )
            );

            client.connect(args[0]);

            Scanner scanner =
                    new Scanner(System.in);

            System.out.println(
                    "Write: /to receiverUsername your message"
            );

            while (true) {

                String input =
                        scanner.nextLine();

                if (
                        input.equalsIgnoreCase(
                                "/exit"
                        )
                ) {

                    client.disconnect();

                    break;
                }

                String[] parts =
                        input.split(" ", 3);

                if (
                        parts.length == 3
                                && parts[0]
                                .equals("/to")
                ) {

                    client.sendMessage(
                            parts[1],
                            parts[2]
                    );

                } else {

                    System.out.println(
                            "Use: /to receiverUsername your message"
                    );
                }
            }

        } catch (IOException exception) {

            System.out.println(
                    "Connection failed: "
                            + exception.getMessage()
            );
        }
    }
    
}