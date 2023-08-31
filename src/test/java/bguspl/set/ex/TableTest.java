package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.UserInterfaceImpl;
import bguspl.set.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class TableTest {

    @Mock
    Table table;
    @Mock
    private Integer[] slotToCard;
    @Mock
    private Integer[] cardToSlot;
    @Mock
    Player[] players;
    @Mock
    Dealer dealer;
    @Mock
    Util util;
    @Mock
    UserInterfaceImpl ui;
    @Mock
    Logger logger;

    @BeforeEach
    void setUp() {
        logger = Logger.getAnonymousLogger();
        Properties properties = new Properties();
        properties.put("Rows", "2");
        properties.put("Columns", "2");
        properties.put("FeatureSize", "3");
        properties.put("FeatureCount", "4");
        properties.put("TableDelaySeconds", "0");
        properties.put("PlayerKeys1", "81,87,69,82");
        properties.put("PlayerKeys2", "85,73,79,80");
        Config config = new Config(logger, properties);
        slotToCard = new Integer[config.tableSize];
        cardToSlot = new Integer[config.deckSize];
        ui = new UserInterfaceImpl(config);
        Env env = new Env(logger, config, ui, util);
        table = new Table(env, slotToCard, cardToSlot);
        players = new Player[env.config.players];
        dealer = new Dealer(env, table, players);
        for (int i = 0; i < players.length; i++)
            players[i] = new Player(env, dealer, table, i, i < env.config.humanPlayers);

            
    }

    private int fillSomeSlots() {
        slotToCard[1] = 3;
        slotToCard[2] = 5;
        cardToSlot[3] = 1;
        cardToSlot[5] = 2;

        return 2;
    }

    private void fillAllSlots() {
        for (int i = 0; i < slotToCard.length; ++i) {
            slotToCard[i] = i;
            cardToSlot[i] = i;
        }
    }

    private void placeSomeCardsAndAssert() {
        table.placeCard(8, 2);

        assertEquals(8, (int) slotToCard[2]);
        assertEquals(2, (int) cardToSlot[8]);
    }

    @Test
    void countCards_NoSlotsAreFilled() {

        assertEquals(0, table.countCards());
    }

    @Test
    void countCards_SomeSlotsAreFilled() {

        int slotsFilled = fillSomeSlots();
        assertEquals(slotsFilled, table.countCards());
    }

    @Test
    void countCards_AllSlotsAreFilled() {

        fillAllSlots();
        assertEquals(slotToCard.length, table.countCards());
    }

    @Test
    void placeCard_SomeSlotsAreFilled() {

        fillSomeSlots();
        placeSomeCardsAndAssert();
    }

    @Test
    void placeCard_AllSlotsAreFilled() {
        fillAllSlots();
        placeSomeCardsAndAssert();
    }
    @Test
     void placeTokensAndAssert() {// MY TEST---------------------------------------------------
        int tokenOnSlot = 3;
        placeCard_AllSlotsAreFilled();
        table.placeToken(players[0].getId(), tokenOnSlot);
        assertEquals(3 , table.getTokens(players[0].getId()).get(0));
       // verify(ui).placeToken(players[0].getId(), tokenOnSlot);
        table.placeToken(players[0].getId(), 1);
        table.placeToken(players[0].getId(), 2);
        assertEquals(true, table.allTokensPlaced(players[0].getId()));
    }
    @Test
    void removeCardAndAssert(){ //my test ---------------------------------------------
        placeCard_AllSlotsAreFilled();
        int slot = 3;
        int card = table.slotToCard[slot];
        table.removeCard(slot);
        assertEquals(null , table.slotToCard[slot]);
        assertEquals(null , table.cardToSlot[card]);
    }

    static class MockUserInterface implements UserInterface {
        @Override
        public void placeCard(int card, int slot) {
        }

        @Override
        public void removeCard(int slot) {
        }



        @Override
        public void setScore(int player, int score) {
        }


        @Override
        public void placeToken(int player, int slot) {
        }

        @Override
        public void removeTokens() {
        }

        @Override
        public void removeTokens(int slot) {
        }

        @Override
        public void removeToken(int player, int slot) {
        }

        @Override
        public void announceWinner(int[] players) {
        }

        @Override
        public void setCountdown(long millies, boolean warn) {
            
        }

        @Override
        public void setElapsed(long millies) {
            
        }

        @Override
        public void setFreeze(int player, long millies) {
            
        }

        @Override
        public void dispose() {
            // TODO Auto-generated method stub
            
        }
       
    };

    static class MockUtil implements Util {
        @Override
        public int[] cardToFeatures(int card) {
            return new int[0];
        }

        @Override
        public int[][] cardsToFeatures(int[] cards) {
            return new int[0][];
        }

        @Override
        public boolean testSet(int[] cards) {
            return false;
        }

        @Override
        public List<int[]> findSets(List<Integer> deck, int count) {
            return null;
        }

        @Override
        public void spin() {
            // TODO Auto-generated method stub
            
        }
    }
}
