package digiworld.app;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            ScreenTitle titleScreen = new ScreenTitle();
            titleScreen.setVisible(true);
        });
    }
}
