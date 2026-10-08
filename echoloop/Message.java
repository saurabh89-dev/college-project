/** Stores one chat message. */
public class Message {
    private final int senderId;
    private final String text;

    public Message(int senderId, String text) {
        this.senderId = senderId;
        this.text = text;
    }

    public int getSenderId() { return senderId; }
    public String getText() { return text; }
}
