package digiworld.dialogue;

import digiworld.app.*;
import digiworld.battle.*;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.ui.*;

public class DialogueController {
    private DialogueSequence sequence = new DialogueSequence(null);
    private int pageIndex;
    private int visibleChars;
    private double typeTimer;
    private boolean lineFinished;
    private double charsPerSecond = 42.0;

    public void start(DialogueSequence sequence) {
        this.sequence = sequence == null ? new DialogueSequence(null) : sequence;
        this.pageIndex = 0;
        this.visibleChars = 0;
        this.typeTimer = 0.0;
        this.lineFinished = this.sequence.size() == 0;
    }

    public void update(double deltaSeconds) {
        if (lineFinished || sequence.size() == 0) {
            return;
        }
        typeTimer += deltaSeconds;
        String text = getCurrentPage().getText();
        while (typeTimer >= 1.0 / charsPerSecond && visibleChars < text.length()) {
            typeTimer -= 1.0 / charsPerSecond;
            visibleChars++;
        }
        if (visibleChars >= text.length()) {
            lineFinished = true;
        }
    }

    public boolean advance() {
        if (sequence.size() == 0) {
            return true;
        }
        if (!lineFinished) {
            visibleChars = getCurrentPage().getText().length();
            lineFinished = true;
            return false;
        }
        if (pageIndex < sequence.size() - 1) {
            pageIndex++;
            visibleChars = 0;
            typeTimer = 0.0;
            lineFinished = false;
            return false;
        }
        return true;
    }

    public DialoguePage getCurrentPage() {
        return sequence.get(pageIndex);
    }

    public String getVisibleText() {
        String text = getCurrentPage().getText();
        int n = Math.max(0, Math.min(visibleChars, text.length()));
        return text.substring(0, n);
    }

    public boolean isLineFinished() {
        return lineFinished;
    }
}
