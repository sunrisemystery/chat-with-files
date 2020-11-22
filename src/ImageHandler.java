import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;

public class ImageHandler implements Handler {
    private AudioHandler audioHandler = new AudioHandler();

    @Override
    public String handleRequest(String message) {

        if (URLConnection.guessContentTypeFromName(message) != null) {
            try {
                String content = URLConnection.guessContentTypeFromName(message);

                if (content.equals("image/bmp")) {

                    File file = new File(message);
                    FileInputStream fileInputStream = new FileInputStream(file);
                    fileInputStream.getChannel().lock(0, Long.MAX_VALUE, true);
                    BufferedImage myPicture = ImageIO.read(fileInputStream);
                    JLabel jLabel = new JLabel(new ImageIcon(myPicture));
                    JPanel jPanel = new JPanel();
                    jPanel.add(jLabel);
                    JFrame jFrame = new JFrame();
                    jFrame.setSize(new Dimension(myPicture.getWidth(), myPicture.getHeight()));
                    jFrame.add(jPanel);
                    jFrame.setVisible(true);
                    fileInputStream.close();
                } else if (content.equals("audio/x-wav")) {
                    return audioHandler.handleRequest(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "Image file displayed";
    }
}



