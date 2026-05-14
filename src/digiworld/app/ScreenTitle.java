package digiworld.app;

import digiworld.audio.SoundManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class ScreenTitle extends JFrame {
    private static final String GIF_NAME = "res/DIGIWORLD.gif";

    public ScreenTitle() {
        setTitle("Game Title Screen");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setResizable(false);

        SoundManager.getInstance().playTitleMusic();

        ImageIcon gifIcon = new ImageIcon(GIF_NAME);
        StretchLabel background = new StretchLabel(gifIcon);
        background.setLayout(new java.awt.GridBagLayout());

        MenuButton playButton = new MenuButton("PLAY");
        MenuButton creditsButton = new MenuButton("CREDITS");
        MenuButton exitButton = new MenuButton("EXIT");

        playButton.addActionListener(e -> {
            SoundManager.getInstance().stopMusic();
            dispose();
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
            SoundManager.getInstance().playWorldMusic("Hometown");
        });

        creditsButton.addActionListener(e -> {
            JFrame creditsFrame = new JFrame("Credits");
            creditsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            creditsFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            creditsFrame.setUndecorated(true);

            ImageIcon creditsIcon = new ImageIcon("res/ui/credits.gif");
            StretchLabel creditsLabel = new StretchLabel(creditsIcon);

            JButton backButton = new JButton("BACK");
            backButton.setFont(new Font("Arial", Font.BOLD, 30));
            backButton.setForeground(Color.WHITE);
            backButton.setBackground(new Color(0, 0, 0, 160));
            backButton.setBorderPainted(false);
            backButton.setFocusPainted(false);
            backButton.addActionListener(ev -> {
                creditsFrame.dispose();
                this.setVisible(true);
            });

            JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            backPanel.setOpaque(false);
            backPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 220, 0, 0));
            backPanel.add(backButton);

            creditsLabel.setLayout(new BorderLayout());
            creditsLabel.add(backPanel, BorderLayout.SOUTH);

            creditsFrame.setContentPane(creditsLabel);
            creditsFrame.setVisible(true);

            this.setVisible(false);
        });

        exitButton.addActionListener(e -> {
            SoundManager.getInstance().stopMusic();
            System.exit(0);
        });

        JPanel menuPanel = new JPanel(new java.awt.GridBagLayout());
        menuPanel.setOpaque(false);
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(10, 0, 10, 0);

        gbc.gridy = 0;
        menuPanel.add(playButton, gbc);
        gbc.gridy = 1;
        menuPanel.add(creditsButton, gbc);
        gbc.gridy = 2;
        menuPanel.add(exitButton, gbc);

        background.add(menuPanel);
        setContentPane(background);
    }

    static class StretchLabel extends JLabel {
        private final ImageIcon gifIcon;

        public StretchLabel(ImageIcon gifIcon) {
            this.gifIcon = gifIcon;
            setIcon(gifIcon);
            Timer timer = new Timer(33, e -> repaint());
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(gifIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
        }
    }

    static class MenuButton extends JButton {
        private boolean hovered = false;

        public MenuButton(String text) {
            super(text);
            setPreferredSize(new Dimension(220, 55));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new HoverListener());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovered ? new Color(255, 221, 87, 60) : new Color(0, 0, 0, 160));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(hovered ? new Color(255, 221, 87) : Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
            g2.setFont(new Font("Arial Black", Font.PLAIN, 18));

            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(getText())) / 2;
            int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }

        private class HoverListener extends MouseAdapter {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScreenTitle().setVisible(true));
    }
}
