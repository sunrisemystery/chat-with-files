import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioHandler implements Handler {

    @Override
    public String handleRequest(String message) {
        try {
            File file = new File(message);
            Clip clip;
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();

        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            e.printStackTrace();
        }
        return "Audio file played in background";
    }
}

