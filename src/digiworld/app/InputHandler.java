package digiworld.app;

import digiworld.app.*;
import digiworld.battle.*;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.ui.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

public class InputHandler implements KeyListener {
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> justPressedKeys = new HashSet<>();

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (!pressedKeys.contains(code)) {
            justPressedKeys.add(code);
        }
        pressedKeys.add(code);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    public boolean isPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    public boolean consumeJustPressed(int keyCode) {
        return justPressedKeys.remove(keyCode);
    }

    public void endFrame() {
        justPressedKeys.clear();
    }
}
