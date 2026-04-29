public class DialoguePage {
    private final String speaker;
    private final String text;

    public DialoguePage(String speaker, String text) {
        this.speaker = speaker == null ? "" : speaker;
        this.text = text == null ? "" : text;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }
}
