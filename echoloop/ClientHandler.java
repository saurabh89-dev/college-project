import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private int userId;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            loginUser();

            String input;
            while ((input = reader.readLine()) != null) {
                handleRequest(input);
            }
        } catch (IOException exception) {
            System.out.println("Client disconnected.");
        } finally {
            closeConnection();
        }
    }

    private void loginUser() throws IOException {
        String loginRequest = reader.readLine();
        if (loginRequest == null || !loginRequest.startsWith("LOGIN|")) {
            sendMessage("ERROR|Login request is invalid.");
            throw new IOException("Invalid login request.");
        }

        username = loginRequest.substring(6).trim();
        try {
            userId = getUserId(username);
        } catch (SQLException exception) {
            sendMessage("ERROR|Database error while logging in.");
            throw new IOException("Could not check user in database.", exception);
        }

        if (userId == 0) {
            sendMessage("ERROR|User does not exist. Register first.");
            throw new IOException("Unknown user.");
        }

        if (!ChatServer.addOnlineUser(username, this)) {
            sendMessage("ERROR|This user is already online.");
            throw new IOException("Duplicate login.");
        }

        sendMessage("LOGIN_OK|Welcome " + username);
        System.out.println(username + " connected.");
    }

    private void handleRequest(String input) {
        String[] parts = input.split("\\|", 3);
        if (parts.length == 3 && parts[0].equals("SEND")) {
            String receiverUsername = parts[1].trim();
            String messageText = parts[2].trim();

            if (receiverUsername.isEmpty() || messageText.isEmpty()) {
                sendMessage("ERROR|Receiver and message cannot be empty.");
                return;
            }
            saveAndSendMessage(receiverUsername, messageText);
        } else {
            sendMessage("ERROR|Unknown command.");
        }
    }

    private void saveAndSendMessage(String receiverUsername, String messageText) {
        try {
            int receiverId = getUserId(receiverUsername);
            if (receiverId == 0) {
                sendMessage("ERROR|Receiver username does not exist.");
                return;
            }
            if (receiverId == userId) {
                sendMessage("ERROR|You cannot message yourself.");
                return;
            }

            int conversationId = getOrCreateConversation(userId, receiverId);
            saveMessage(conversationId, userId, messageText);

            String realTimeMessage = "MESSAGE|" + username + "|" + messageText;
            sendMessage(realTimeMessage);
            ChatServer.sendToUser(receiverUsername, realTimeMessage);
            sendMessage("STATUS|Message saved successfully.");
        } catch (SQLException exception) {
            sendMessage("ERROR|Database error: " + exception.getMessage());
        }
    }

    private int getUserId(String requestedUsername) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestedUsername);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("id");
                }
            }
        }
        return 0;
    }

    private int getOrCreateConversation(int firstUserId, int secondUserId) throws SQLException {
        int userOneId = Math.min(firstUserId, secondUserId);
        int userTwoId = Math.max(firstUserId, secondUserId);

        String findSql = """
                SELECT id
                FROM conversations
                WHERE user_one_id = ?
                AND user_two_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(findSql)) {
            statement.setInt(1, userOneId);
            statement.setInt(2, userTwoId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("id");
                }
            }
        }

        String insertSql = """
                INSERT INTO conversations
                (user_one_id, user_two_id)
                VALUES (?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     insertSql,
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userOneId);
            statement.setInt(2, userTwoId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException("Conversation could not be created.");
    }

    private void saveMessage(int conversationId, int senderId, String messageText)
            throws SQLException {
        String sql = """
                INSERT INTO messages
                (conversation_id, sender_id, message_text)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            statement.setInt(2, senderId);
            statement.setString(3, messageText);
            statement.executeUpdate();
        }
    }

    public synchronized void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    private void closeConnection() {
        ChatServer.removeOnlineUser(username);
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        if (username != null) {
            System.out.println(username + " disconnected.");
        }
    }
}
