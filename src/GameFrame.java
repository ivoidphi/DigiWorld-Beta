import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class GameFrame extends JFrame {
    private final GamePanel panel;

    public GameFrame() {
        setTitle("DigiWorld - The Oten Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setUndecorated(true);

        panel = new GamePanel();
        setContentPane(panel);
        pack();

        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.isFullScreenSupported()) {
            device.setFullScreenWindow(this);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    public void startGame() {
        SwingUtilities.invokeLater(() -> {
            panel.requestFocusInWindow();
            panel.startGame();
        });
    }
}
