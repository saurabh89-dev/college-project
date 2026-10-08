import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class UserService {

    private UserService() {
    }

    public static boolean register(
            String username,
            String email,
            String password) throws SQLException {

        String sql = """
                INSERT INTO users (username, email, password_hash)
                VALUES (?, ?, ?)
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();

                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, email);

            statement.setString(3, password);

            statement.executeUpdate();

            return true;

        } catch (SQLException exception) {

            if (exception.getErrorCode() == 1062) {
                return false;
            }

            throw exception;
        }
    }

    public static boolean login(
            String username,
            String password) throws SQLException {

        String sql = """
                SELECT id
                FROM users
                WHERE username = ?
                AND password_hash = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();

                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, password);

            try (
                    ResultSet result = statement.executeQuery()) {

                return result.next();
            }
        }
    }

    public static int idOf(
            String username) throws SQLException {

        String sql = """
                SELECT id
                FROM users
                WHERE username = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();

                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (
                    ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return result.getInt("id");
                }
            }
        }

        return 0;
    }

    public static boolean exists(
            String username) throws SQLException {

        String sql = """
                SELECT id
                FROM users
                WHERE username = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();

                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (
                    ResultSet result = statement.executeQuery()) {

                return result.next();
            }
        }
    }

    public static List<String> allUsernames()
            throws SQLException {

        String sql = """
                SELECT username
                FROM users
                ORDER BY username
                """;

        List<String> usernames = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();

                PreparedStatement statement = connection.prepareStatement(sql);

                ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                usernames.add(
                        result.getString("username"));
            }
        }

        return usernames;
    }
}