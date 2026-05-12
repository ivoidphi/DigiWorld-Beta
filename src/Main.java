public class Main {

    public static void main(String[] args) {

        javax.swing.SwingUtilities.invokeLater(() -> {

            // Open title screen first
            ScreenTitle titleScreen = new ScreenTitle();

            titleScreen.setVisible(true);

        });
    }
}