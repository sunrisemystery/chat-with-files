import java.io.Serializable;

public class Message implements Serializable {

    static final int MESSAGE = 1, LOGOUT = 2, PICK = 3;
    private int type;
    private String message;

    Message(int type, String message) {
        this.type = type;
        this.message = message;
    }
    int getType() {
        return type;
    }

    String getMessage() {
        return message;
    }

}
