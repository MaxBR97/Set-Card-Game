package bguspl.set;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.net.URL;

import static java.lang.String.format;

/**
 * Java Swing implementation of the UserInterface interface.
 */
public class UserInterfaceImpl extends JFrame implements UserInterface {

    private final TimerPanel timerPanel;
    private final GamePanel gamePanel;
    private final PlayersPanel playersPanel;
    private final WinnerPanel winnerPanel;

    static String intInBaseToPaddedString(int n, int padding, int base) {
        return format("%" + padding + "s", Integer.toString(n, base)).replace(' ', '0');
    }

    public UserInterfaceImpl(Config config) {

        timerPanel = new TimerPanel(config);
        gamePanel = new GamePanel(config);
        playersPanel = new PlayersPanel(config);
        winnerPanel = new WinnerPanel(config);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        add(timerPanel, gbc);
        gbc.gridy++;
        add(gamePanel, gbc);
        gbc.gridy++;
        add(playersPanel, gbc);
        gbc.gridy++;
        add(winnerPanel, gbc);
        gbc.gridwidth = 1;

        setFocusable(true);
        requestFocusInWindow();

        setResizable(false);
        pack();

        setTitle("Set Card Game");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private static class TimerPanel extends JPanel {

        private final JLabel timerField;

        private String generateTime(long millies, boolean warn) {
            if (warn)
                return format("Remaining Time: %.2f", (double) millies / 1000.0f);
            else
                return format("Remaining Time: %d", millies / 1000L);
        }

        private TimerPanel(Config config) {
            timerField = new JLabel(config.turnTimeoutMillis < 0 ? "PLAY" : "GET READY...");

            // set fonts and color
            timerField.setFont(new Font("Serif", Font.BOLD, config.fontSize));
            timerField.setForeground(Color.BLACK);

            add(timerField);
        }

        private void setCountdown(long millies, boolean warn) {
            timerField.setText(generateTime(millies, warn));
            timerField.setBackground(warn ? Color.RED : Color.BLACK);
        }

        private void setElapsed(long millies) {
            timerField.setText("Elapsed time: " + millies / 1000);
        }
    }

    private static class GamePanel extends JLayeredPane {

        private final Config config;
        private final Image emptyCard;
        private final Image[] deck;
        private final Image[][] grid;
        private final boolean[][][] playerTokens;
        private final JLabel[][] tokenText;

        private Image loadImageResource(String filename) {
            URL imageResource = getClass().getClassLoader().getResource(filename);
            if (imageResource == null)
                throw new RuntimeException(new FileNotFoundException(filename));
            return new ImageIcon(imageResource).getImage();
        }

        private GamePanel(Config config) {

            this.config = config;

            setPreferredSize(new Dimension(config.columns * config.cellWidth, config.rows * config.cellHeight));

            // init deck and load all pictures from png files
            assert config.featureSize < 10; // otherwise there will be naming conflicts

            // load the image resources
            deck = new Image[config.deckSize];
            for (int i = 0; i < config.deckSize; ++i)
                deck[i] = loadImageResource("cards/" + intInBaseToPaddedString(i, config.featureCount, config.featureSize) + ".png");
            emptyCard = loadImageResource("cards/empty_card.png");

            grid = new Image[config.rows][config.columns];
            tokenText = new JLabel[config.rows][config.columns];
            playerTokens = new boolean[config.players][config.rows][config.columns];
            for (int row = 0; row < config.rows; row++) {
                for (int column = 0; column < config.columns; column++) {
                    // init the cards on the table grid as empty cards
                    grid[row][column] = emptyCard;

                    // init the JLabel selection overlay
                    tokenText[row][column] = new JLabel("");
                    tokenText[row][column].setVerticalAlignment(JLabel.TOP);
                    tokenText[row][column].setHorizontalAlignment(JLabel.CENTER);
                    tokenText[row][column].setOpaque(false);
                    tokenText[row][column].setBorder(BorderFactory.createLineBorder(Color.black));
                    tokenText[row][column].setBounds((column * config.cellWidth), (row * config.cellHeight), config.cellWidth, config.cellHeight);
                    add(tokenText[row][column]);
                }
            }
        }

        private void placeCard(int slot, int card) {
            int row = slot / config.columns;
            int column = slot % config.columns;
            grid[row][column] = deck[card];
            validate();
            repaint();
        }

        private void removeCard(int slot) {
            int row = slot / config.columns;
            int column = slot % config.columns;
            grid[row][column] = emptyCard;
            validate();
            repaint();
        }

        private void placeToken(int player, int slot) {
            int row = slot / config.columns;
            int column = slot % config.columns;
            playerTokens[player][row][column] = true;
            tokenText[row][column].setText(generatePlayersTokenText(row, column));
        }

        private void removeTokens() {
            for (int i = 0; i < config.tableSize; i++)
                removeTokens(i);
        }

        private void removeTokens(int slot) {
            int row = slot / config.columns;
            int column = slot % config.columns;
            for (int player = 0; player < playerTokens.length; player++) {
                playerTokens[player][row][column] = false;
                tokenText[row][column].setText(generatePlayersTokenText(row, column));
            }
        }

        private void removeToken(int player, int slot) {
            int row = slot / config.columns;
            int column = slot % config.columns;
            playerTokens[player][row][column] = false;
            tokenText[row][column].setText(generatePlayersTokenText(row, column));
        }

        private String generatePlayersTokenText(int row, int column) {
            String text = "";
            for (int player = 0; player < config.players; player++) {
                if (playerTokens[player][row][column])
                    text = text.concat(config.playerNames[player] + ", ");
            }
            if (text.length() < 2)
                return "";
            return text.substring(0, text.length() - 2);
        }

        @Override
        public void paintComponent(Graphics g) {

            // draw card images
            for (int row = 0; row < config.rows; row++)
                for (int column = 0; column < config.columns; column++)
                    g.drawImage(grid[row][column], (column * config.cellWidth), (row * config.cellHeight), this);
        }
    }

    private static class PlayersPanel extends JPanel {

        private final Config config;
        private final JLabel[][] playersTable;

        private PlayersPanel(Config config) {
            this.config = config;
            this.setLayout(new GridLayout(2, config.players));
            this.playersTable = new JLabel[2][config.players];
            for (int i = 0; i < config.players; i++) {
                this.playersTable[0][i] = new JLabel(config.playerNames[i]);
                this.playersTable[0][i].setFont(new Font("Serif", Font.BOLD, config.fontSize));
                this.playersTable[0][i].setHorizontalAlignment(JLabel.CENTER);
                this.add(playersTable[0][i]);
            }

            for (int i = 0; i < config.players; i++) {
                this.playersTable[1][i] = new JLabel("0");
                this.playersTable[1][i].setFont(new Font("Serif", Font.PLAIN, config.fontSize));
                this.playersTable[1][i].setHorizontalAlignment(JLabel.CENTER);
                this.add(playersTable[1][i]);
            }
        }

        private void setScore(int player, int score) {
            playersTable[1][player].setText(Integer.toString(score));
        }

        private void setFreeze(int player, long millies) {
            if (millies > 0) {
                this.playersTable[0][player].setText(config.playerNames[player] + " (" + millies / 1000 + ")");
                this.playersTable[0][player].setForeground(Color.RED);
            } else {
                this.playersTable[0][player].setText(config.playerNames[player]);
                this.playersTable[0][player].setForeground(Color.BLACK);
            }
        }
    }

    private static class WinnerPanel extends JPanel {

        private final Config config;
        private final JLabel winnerAnnouncement;

        public WinnerPanel(Config config) {
            this.config = config;
            this.setVisible(false);

            this.winnerAnnouncement = new JLabel();
            this.winnerAnnouncement.setFont(new Font("Serif", Font.BOLD, config.fontSize));
            this.winnerAnnouncement.setHorizontalAlignment(JLabel.CENTER);
            this.winnerAnnouncement.setSize(config.cellWidth, config.cellHeight);
            add(winnerAnnouncement);
        }

        private void announceWinner(int[] players) {
            if(players.length == 1)
                winnerAnnouncement.setText("THE WINNER IS: " + config.playerNames[players[0]] + "!!!");
            else {
                String text = "";
                for (int player : players)
                    text = text.concat(config.playerNames[player] + " AND ");
                text = text.substring(0, text.length() - 5);
                winnerAnnouncement.setText("IT IS A DRAW: " + text + " WON!!!");
            }
        }
    }

    @Override
    public void placeCard(int card, int slot) {
        gamePanel.placeCard(slot, card);
    }

    @Override
    public void removeCard(int slot) {
        gamePanel.removeCard(slot);
    }

    public void setCountdown(long millies, boolean warn) {
        timerPanel.setCountdown(millies, warn);
    }

    public void setElapsed(long millies) {
        timerPanel.setElapsed(millies);
    }

    @Override
    public void setScore(int player, int score) {
        playersPanel.setScore(player, score);
    }

    @Override
    public void setFreeze(int player, long millies) {
        playersPanel.setFreeze(player, millies);
    }

    @Override
    public void placeToken(int player, int slot) {
        gamePanel.placeToken(player, slot);
    }

    @Override
    public void removeTokens() {
        gamePanel.removeTokens();
    }

    @Override
    public void removeTokens(int slot) {
        gamePanel.removeTokens(slot);
    }

    @Override
    public void removeToken(int player, int slot) {
        gamePanel.removeToken(player, slot);
    }

    @Override
    public void announceWinner(int[] players) {
        playersPanel.setVisible(false);
        winnerPanel.announceWinner(players);
        winnerPanel.setVisible(true);
    }
}
