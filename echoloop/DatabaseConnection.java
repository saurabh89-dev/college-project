
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static final String CONFIG_FILE =
            "database.properties";

    private DatabaseConnection() {
    }

    public static Connection getConnection()
            throws SQLException {

        Properties settings = new Properties();

        try (FileInputStream input =
                     new FileInputStream(CONFIG_FILE)) {

            settings.load(input);

        } catch (IOException exception) {

            throw new SQLException(
                    "Cannot read database.properties from: "
                            + System.getProperty("user.dir"),
                    exception
            );
        }

        String url = settings.getProperty("db.url");
        String username = settings.getProperty("db.username");
        String password = settings.getProperty("db.password");

        if (url == null || username == null || password == null) {
            throw new SQLException(
                    "Missing db.url, db.username, or db.password "
                            + "in database.properties."
            );
        }

        return DriverManager.getConnection(
                url.trim(),
                username.trim(),
                password.trim()
        );
    }
}

