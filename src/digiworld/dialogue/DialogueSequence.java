package digiworld.dialogue;

import digiworld.app.*;
import digiworld.battle.*;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.ui.*;

import java.util.ArrayList;
import java.util.List;

public class DialogueSequence {
    private final List<DialoguePage> pages;

    public DialogueSequence(List<DialoguePage> pages) {
        this.pages = pages == null ? new ArrayList<>() : pages;
    }

    public int size() {
        return pages.size();
    }

    public DialoguePage get(int index) {
        if (index < 0 || index >= pages.size()) {
            return new DialoguePage("", "");
        }
        return pages.get(index);
    }
}
