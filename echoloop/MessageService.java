import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class MessageService {

    private MessageService() {
    }

    public record ChatRecord(
            String sender,
            String text,
            Timestamp sentAt) {
    }

    public static int conversationId(
            String firstUser,
            String secondUser,
            boolean create) throws Exception {

        int firstId = UserService.idOf(firstUser);

        int secondId = UserService.idOf(secondUser);

        if (firstId == 0 || secondId == 0) {
            throw new IllegalArgumentException(
                    "User does not exist.");
        }

        int userOneId = Math.min(firstId, secondId);

        int userTwoId = Math.max(firstId, secondId);

        try (
                Connection connection = DatabaseConnection.getConnection()) {

            String findSql = """
                    SELECT id
                    FROM conversations
                    WHERE user_one_id = ?
                    AND user_two_id = ?
                    """;

            try (
                    PreparedStatement statement = connection.prepareStatement(findSql)) {

                statement.setInt(1, userOneId);
                statement.setInt(2, userTwoId);

                try (
                        ResultSet result = statement.executeQuery()) {

                    if (result.next()) {
                        return result.getInt("id");
                    }
                }
            }

            if (!create) {
                return 0;
            }

            String insertSql = """
                    INSERT INTO conversations
                    (user_one_id, user_two_id)
                    VALUES (?, ?)
                    """;

            try (
                    PreparedStatement statement = connection.prepareStatement(
                            insertSql,
                            Statement.RETURN_GENERATED_KEYS)) {

                statement.setInt(1, userOneId);
                statement.setInt(2, userTwoId);

                statement.executeUpdate();

                try (
                        ResultSet keys = statement.getGeneratedKeys()) {

                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        }

        throw new SQLException(
                "Could not create conversation.");
    }

    public static void save(
            String sender,
            String receiver,
            String text) throws Exception {

        int conversation = conversationId(
                sender,
                receiver,
                true);

        int senderId = UserService.idOf(sender);

        String sql = """
                INSERT INTO messages
                (conversation_id, sender_id, message_text)
                VALUES (?, ?, ?)
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();

                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, conversation);
            statement.setInt(2, senderId);
            statement.setString(3, text);

            statement.executeUpdate();
        }
    }

    public static List<ChatRecord> history(
            String firstUser,
            String secondUser) throws Exception {

        int conversation = conversationId(
                firstUser,
                secondUser,
                false);

        List<ChatRecord> records = new ArrayList<>();

        if (conversation == 0) {
            return records;
        }

        String sql = """
                SELECT
                    u.username,
                    m.message_text,
                    m.sent_at
                FROM messages m
                JOIN users u
                    ON u.id = m.sender_id
                WHERE m.conversation_id = ?
                ORDER BY m.sent_at
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();

                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, conversation);

            try (
                    ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    records.add(
                            new ChatRecord(
                                    result.getString("username"),
                                    result.getString("message_text"),
                                    result.getTimestamp("sent_at")));
                }
            }
        }

        return records;
    }

    public static void saveAnalysis(
            String user,
            String receiver,
            String type,
            String resultText) throws Exception {

        int conversation = conversationId(
                user,
                receiver,
                true);

        String sql = """
                INSERT INTO ai_analysis
                (conversation_id, requested_by,
                 analysis_type, result_text)
                VALUES (?, ?, ?, ?)
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();

                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, conversation);
            statement.setInt(
                    2,
                    UserService.idOf(user));
            statement.setString(3, type);
            statement.setString(4, resultText);

            statement.executeUpdate();
        }
    }
}