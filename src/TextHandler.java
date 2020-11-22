import java.net.URLConnection;

public class TextHandler implements Handler {
    private final ImageHandler imageHandler = new ImageHandler();

    @Override
    public String handleRequest(String message) {
        if (URLConnection.guessContentTypeFromName(message) != null) {
            String content = URLConnection.guessContentTypeFromName(message);
            if (content.equals("image/bmp") || content.equals("audio/x-wav")) {
                return imageHandler.handleRequest(message);
            }

        }
        return message;
    }
}