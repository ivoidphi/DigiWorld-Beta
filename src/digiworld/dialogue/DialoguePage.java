package digiworld.dialogue;

import digiworld.app.*;
import digiworld.battle.*;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.ui.*;

public class DialoguePage {
    private final String speaker;
    private final String text;
    private final String portraitPath;

    public DialoguePage(String speaker, String text) {
        this(speaker, text, null);
    }

    public DialoguePage(String speaker, String text, String portraitPath) {
        this.speaker = speaker == null ? "" : speaker;
        this.text = text == null ? "" : text;
        this.portraitPath = portraitPath;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }

    public String getPortraitPath() {
        return portraitPath;
    }
}
