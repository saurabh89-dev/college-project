import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

import javafx.scene.control.Alert.AlertType;

public class LoginScreen extends Application {

    private Stage stage;

    private VBox messageBox;
    private Label chatName;
    private Label statusLabel;

    private String selectedUser = null;
    private String currentUser;

    private ChatClient chatClient;

    private final String PRIMARY = "#6C63FF";
    private final String DARK = "#202333";
    private final String BACKGROUND = "#F6F7FB";

    @Override
    public void start(Stage stage) {

        this.stage = stage;

        showLoginScreen();

        stage.show();
    }

    private void showLoginScreen() {

        Label logo = new Label("EchoLoop");

        logo.setStyle(
                "-fx-font-size: 30px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + PRIMARY + ";");

        Label subtitle = new Label("Connect. Chat. Understand.");

        subtitle.setStyle(
                "-fx-text-fill: #7B8090;" +
                        "-fx-font-size: 14px;");

        TextField usernameField = new TextField();

        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(320);

        PasswordField passwordField = new PasswordField();

        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(320);

        Button loginButton = new Button("Login");

        loginButton.setMaxWidth(320);
        loginButton.setStyle(mainButtonStyle());

        Label registerLink = new Label("New user? Create an account");

        registerLink.setStyle(
                "-fx-text-fill: " + PRIMARY + ";" +
                        "-fx-cursor: hand;");

        registerLink.setOnMouseClicked(
                event -> showRegisterScreen());

        loginButton.setOnAction(event -> {

            String username = usernameField.getText().trim();

            String password = passwordField.getText();

            if (username.isEmpty()
                    || password.isEmpty()) {

                showAlert(
                        "Login Error",
                        "Please enter username and password.");

                return;
            }

            try {

                boolean validLogin = UserService.login(
                        username,
                        password);

                if (!validLogin) {

                    showAlert(
                            "Login Failed",
                            "Invalid username or password.");

                    return;
                }

                currentUser = username;

                chatClient = new ChatClient();

                chatClient.setMessageListener(
                        message -> Platform.runLater(
                                () -> handleServerMessage(message)));

                try {

                    chatClient.connect(username);

                    showChatScreen(username);

                } catch (IOException exception) {

                    chatClient = null;

                    showAlert(
                            "Connection Error",
                            "Could not connect to EchoLoop server.\n\n"
                                    + exception.getMessage());
                }

            } catch (Exception exception) {

                showAlert(
                        "Database Error",
                        exception.getMessage());
            }
        });

        VBox loginCard = new VBox(
                15,
                logo,
                subtitle,
                usernameField,
                passwordField,
                loginButton,
                registerLink);

        loginCard.setAlignment(Pos.CENTER);
        loginCard.setPadding(new Insets(40));
        loginCard.setMaxWidth(420);

        loginCard.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-effect: dropshadow(" +
                        "three-pass-box, rgba(0,0,0,0.12), 18, 0, 0, 5);");

        StackPane root = new StackPane(loginCard);

        root.setStyle(
                "-fx-background-color: " +
                        BACKGROUND + ";");

        stage.setTitle("EchoLoop - Login");

        stage.setScene(
                new Scene(root, 1000, 650));
    }

    private void showRegisterScreen() {

        Label title = new Label("Create EchoLoop Account");

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + PRIMARY + ";");

        TextField usernameField = new TextField();

        usernameField.setPromptText("Username");

        TextField emailField = new TextField();

        emailField.setPromptText("Email address");

        PasswordField passwordField = new PasswordField();

        passwordField.setPromptText("Password");

        Button registerButton = new Button("Create Account");

        registerButton.setMaxWidth(
                Double.MAX_VALUE);

        registerButton.setStyle(
                mainButtonStyle());

        Button backButton = new Button("Back to Login");

        backButton.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + PRIMARY + ";" +
                        "-fx-cursor: hand;");

        registerButton.setOnAction(event -> {

            String username = usernameField.getText().trim();

            String email = emailField.getText().trim();

            String password = passwordField.getText();

            if (username.isEmpty()
                    || email.isEmpty()
                    || password.isEmpty()) {

                showAlert(
                        "Registration Error",
                        "Please fill all fields.");

                return;
            }

            try {

                boolean registered = UserService.register(
                        username,
                        email,
                        password);

                if (!registered) {

                    showAlert(
                            "Registration Failed",
                            "Username or email already exists.");

                    return;
                }

                showAlert(

                        "Registration Successful",
                        "Account created successfully.\n"
                                + "You can now log in.");

                showLoginScreen();

            } catch (Exception exception) {

                showAlert(
                        "Database Error",
                        exception.getMessage());
            }
        });

        backButton.setOnAction(
                event -> showLoginScreen());

        VBox card = new VBox(
                15,
                title,
                usernameField,
                emailField,
                passwordField,
                registerButton,
                backButton);

        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(420);

        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-effect: dropshadow(" +
                        "three-pass-box, rgba(0,0,0,0.12), 18, 0, 0, 5);");

        StackPane root = new StackPane(card);

        root.setStyle(
                "-fx-background-color: " +
                        BACKGROUND + ";");

        stage.setTitle("EchoLoop - Register");

        stage.setScene(
                new Scene(root, 1000, 650));
    }

    private void showChatScreen(
            String loggedInUsername) {

        currentUser = loggedInUsername;

        selectedUser = null;

        BorderPane root = new BorderPane();

        root.setStyle(
                "-fx-background-color: " +
                        BACKGROUND + ";");

        VBox sidebar = createSidebar(
                loggedInUsername);

        root.setLeft(sidebar);

        VBox chatArea = createChatArea();

        root.setCenter(chatArea);

        stage.setTitle(
                "EchoLoop - Chat - "
                        + loggedInUsername);

        stage.setScene(
                new Scene(root, 1200, 720));

        stage.show();
    }

    private VBox createSidebar(
            String loggedInUsername) {

        Label appName = new Label("EchoLoop");

        appName.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;");

        Label profileName = new Label(loggedInUsername);

        profileName.setStyle(
                "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;");

        Label online = new Label("● Online");

        online.setStyle(
                "-fx-text-fill: #5BDE91;");

        VBox profile = new VBox(
                4,
                profileName,
                online);

        profile.setPadding(
                new Insets(15));

        profile.setStyle(
                "-fx-background-color: #2C3045;" +
                        "-fx-background-radius: 12;");

        TextField searchField = new TextField();

        searchField.setPromptText(
                "Search users");

        ObservableList<String> visibleUsers = FXCollections.observableArrayList();

        // Loading real users from MySQL
        try {

            List<String> databaseUsers = UserService.allUsernames();

            for (String username : databaseUsers) {

                if (!username.equals(loggedInUsername)) {

                    visibleUsers.add(
                            username + " • Online");
                }
            }

        } catch (Exception exception) {

            showAlert(
                    "User List Error",
                    "Could not load users.\n\n"
                            + exception.getMessage());
        }

        ListView<String> userList = new ListView<>(visibleUsers);

        userList.setPrefHeight(400);

        userList.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable,
                                oldValue,
                                newValue) -> {

                            if (newValue != null) {

                                selectedUser = newValue.split(
                                        " • ")[0];

                                if (chatName != null) {

                                    chatName.setText(
                                            selectedUser);
                                }

                                if (statusLabel != null) {

                                    statusLabel.setText(
                                            "● Online");
                                }

                                loadConversationHistory();
                            }
                        });

        searchField.textProperty()
                .addListener(
                        (observable,
                                oldValue,
                                text) -> {

                            String searchText = text.toLowerCase()
                                    .trim();

                            visibleUsers.clear();

                            try {

                                List<String> databaseUsers = UserService.allUsernames();

                                for (String username : databaseUsers) {

                                    if (!username.equals(
                                            loggedInUsername)
                                            && username
                                                    .toLowerCase()
                                                    .contains(
                                                            searchText)) {

                                        visibleUsers.add(
                                                username
                                                        + " • Online");
                                    }
                                }

                            } catch (Exception exception) {

                                showAlert(
                                        "Search Error",
                                        exception.getMessage());
                            }
                        });

        Button logoutButton = new Button("Logout");

        logoutButton.setMaxWidth(
                Double.MAX_VALUE);

        logoutButton.setStyle(
                "-fx-background-color: #EF476F;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;");

        logoutButton.setOnAction(

                event -> logout());

        Label usersLabel = new Label("USERS");

        usersLabel.setStyle(
                "-fx-text-fill: #AEB2C2;" +
                        "-fx-font-weight: bold;");

        VBox sidebar = new VBox(
                18,
                appName,
                profile,
                searchField,
                usersLabel,
                userList,
                logoutButton);

        VBox.setVgrow(
                userList,
                Priority.ALWAYS);

        sidebar.setPrefWidth(280);

        sidebar.setPadding(
                new Insets(22));

        sidebar.setStyle(
                "-fx-background-color: " +
                        DARK + ";");

        return sidebar;
    }

    private VBox createChatArea() {

        chatName = new Label(
                selectedUser == null
                        ? "Select a user"
                        : selectedUser);

        chatName.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #232637;");

        statusLabel = new Label(
                selectedUser == null
                        ? ""
                        : "● Online");

        statusLabel.setStyle(
                "-fx-text-fill: #28A745;");

        VBox userInfo = new VBox(
                3,
                chatName,
                statusLabel);

        Button keywordButton = new Button("Keywords");

        Button summarizeButton = new Button("Summarize");

        keywordButton.setStyle(
                secondaryButtonStyle());

        summarizeButton.setStyle(
                secondaryButtonStyle());

        keywordButton.setOnAction(
                event -> showAlert(
                        "AI Keywords",
                        "Keyword analysis will be connected next."));

        summarizeButton.setOnAction(
                event -> showAlert(
                        "AI Summary",
                        "Chat summarization will be connected next."));

        HBox header = new HBox(
                12,
                userInfo,
                keywordButton,
                summarizeButton);

        HBox.setHgrow(
                userInfo,
                Priority.ALWAYS);

        header.setAlignment(
                Pos.CENTER_LEFT);

        header.setPadding(
                new Insets(
                        18,
                        25,
                        18,
                        25));

        header.setStyle(
                "-fx-background-color: white;");

        messageBox = new VBox(12);

        messageBox.setPadding(
                new Insets(25));

        ScrollPane messagesScroll = new ScrollPane(
                messageBox);

        messagesScroll.setFitToWidth(
                true);

        messagesScroll.setStyle(
                "-fx-background: "
                        + BACKGROUND + ";" +
                        "-fx-background-color: transparent;");

        if (selectedUser != null) {

            loadConversationHistory();
        }

        TextField messageInput = new TextField();

        messageInput.setPromptText(
                "Type a message...");

        HBox.setHgrow(
                messageInput,
                Priority.ALWAYS);

        Button sendButton = new Button("Send");

        sendButton.setStyle(
                mainButtonStyle());

        sendButton.setOnAction(
                event -> sendMessage(
                        messageInput));

        messageInput.setOnAction(
                event -> sendMessage(
                        messageInput));

        HBox composer = new HBox(
                12,
                messageInput,
                sendButton);

        composer.setPadding(
                new Insets(
                        18,
                        25,
                        18,
                        25));

        composer.setStyle(
                "-fx-background-color: white;");

        VBox chatArea = new VBox(
                header,
                messagesScroll,
                composer);

        VBox.setVgrow(
                messagesScroll,
                Priority.ALWAYS);

        return chatArea;
    }

    private void sendMessage(
            TextField messageInput) {

        String text = messageInput.getText()
                .trim();

        if (text.isEmpty()) {
            return;
        }

        if (chatClient == null) {

            showAlert(
                    "Connection Error",
                    "You are not connected to the server.");

            return;
        }

        if (selectedUser == null
                || selectedUser.isBlank()) {

            showAlert(
                    "Message Error",
                    "Please select a user.");

            return;
        }

        if (selectedUser.equals(currentUser)) {

            showAlert(
                    "Message Error",
                    "You cannot message yourself.");

            return;
        }

        chatClient.sendMessage(
                selectedUser,
                text);

        messageInput.clear();
    }

    private void handleServerMessage(
            String message) {

        if (message == null) {
            return;
        }

        System.out.println(
                "Server: " + message);

        String[] parts = message.split(
                "\\|",
                3);

        if (parts.length == 0) {
            return;
        }

        switch (parts[0]) {

            case "MESSAGE":

                if (parts.length < 3) {
                    return;
                }

                String sender = parts[1];

                String text = parts[2];

                if (sender.equals(currentUser)) {

                    addMessage(
                            text,
                            true);

                } else if (sender.equals(selectedUser)) {

                    addMessage(
                            text,
                            false);
                }

                break;

            case "STATUS":

                if (parts.length >= 2) {

                    showStatusMessage(

                            parts[1]);
                }

                break;

            case "ERROR":

                if (parts.length >= 2) {

                    showAlert(
                            "Server Error",
                            parts[1]);
                }

                break;

            case "LOGIN_OK":

                System.out.println(
                        parts.length >= 2
                                ? parts[1]
                                : "Login successful.");

                break;

            default:

                System.out.println(
                        "Unknown server message: "
                                + message);
        }
    }

    private void addMessage(
            String text,
            boolean sentByMe) {

        if (messageBox == null) {
            return;
        }

        Label bubble = new Label(text);

        bubble.setWrapText(true);

        bubble.setMaxWidth(420);

        bubble.setPadding(
                new Insets(
                        11,
                        14,
                        11,
                        14));

        if (sentByMe) {

            bubble.setStyle(
                    "-fx-background-color: "
                            + PRIMARY + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: "
                            + "16 16 3 16;");

        } else {

            bubble.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-text-fill: #25283A;" +
                            "-fx-background-radius: "
                            + "16 16 16 3;");
        }

        HBox messageRow = new HBox(bubble);

        messageRow.setAlignment(
                sentByMe
                        ? Pos.CENTER_RIGHT
                        : Pos.CENTER_LEFT);

        messageBox.getChildren()
                .add(messageRow);
    }

    private void loadConversationHistory() {

        if (messageBox == null) {
            return;
        }

        messageBox.getChildren().clear();

        if (currentUser == null
                || selectedUser == null) {

            return;
        }

        try {

            List<MessageService.ChatRecord> history = MessageService.history(
                    currentUser,
                    selectedUser);

            if (history.isEmpty()) {

                addMessage(
                        "No messages yet. Start the conversation!",
                        false);

                return;
            }

            for (MessageService.ChatRecord record : history) {

                boolean sentByMe = record.sender()
                        .equals(currentUser);

                addMessage(
                        record.text(),
                        sentByMe);
            }

        } catch (Exception exception) {

            showAlert(
                    "History Error",
                    "Could not load conversation history.\n\n"
                            + exception.getMessage());
        }
    }

    private void logout() {

        if (chatClient != null) {

            chatClient.disconnect();

            chatClient = null;
        }

        currentUser = null;

        selectedUser = null;

        showLoginScreen();
    }

    private void showStatusMessage(
            String message) {

        System.out.println(
                "STATUS: " + message);
    }

    private String mainButtonStyle() {

        return "-fx-background-color: "
                + PRIMARY + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
    }

private String secondaryButtonStyle() {

return
"-fx-background-color: #EAE9FF;" +
"-fx-text-fill: " + PRIMARY + ";" +
"-fx-font-weight: bold;" +
"-fx-background-radius: 8;" +
"-fx-cursor: hand;";
}

private void showAlert(
String title,
String message) {

Alert alert =
new Alert(
AlertType.INFORMATION
);

alert.setTitle(title);

alert.setHeaderText(null);

alert.setContentText(message);

alert.showAndWait();
}


public static void main(
String[] args) {

launch(args);
}
}
