package game.ui.sound;

public enum SoundEffect {
    PLACE   ("place.wav"),
    REMOVE  ("remove.wav"),
    DEPOSIT ("deposit.wav");

    private final String fileName;
    SoundEffect(String fileName) { this.fileName = fileName; }
    public String getFileName()  { return fileName; }
}