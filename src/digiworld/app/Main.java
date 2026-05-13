package digiworld.app;

import digiworld.app.*;
import digiworld.battle.*;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.ui.*;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            ScreenTitle titleScreen = new ScreenTitle();
            titleScreen.setVisible(true);
        });
    }
}
