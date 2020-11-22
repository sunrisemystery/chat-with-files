import java.io.Serializable;

public class Message implements Serializable {

    public static final int MESSAGE = 1;
    public static final int LOGOUT = 2;
    public static final int PICK = 3;
    private int type;
    private String message;

    Message(int type, String message) {
        this.type = type;
        this.message = message;
    }
    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

}
