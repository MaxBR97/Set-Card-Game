package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterfaceImpl;
import bguspl.set.Util;
import bguspl.set.UtilImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;

//@ExtendWith(MockitoExtension.class)
public class DealerTest {
    


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
    Config config;
    @Mock
    Env env;
    @Mock
    Player  targetPlayer;
    @Mock
    Logger logger;

    @BeforeEach
    public void setUp() {
        logger = Logger.getAnonymousLogger();
        Properties properties = new Properties();
        properties.put("Rows", "3");
        properties.put("Columns", "4");
        properties.put("FeatureSize", "3");
        properties.put("FeatureCount", "4");
        properties.put("TableDelaySeconds", "0");
        properties.put("PlayerKeys1", "81,87,69,82");
        properties.put("PlayerKeys2", "85,73,79,80");
        config = new Config(logger, properties);
        slotToCard = new Integer[config.tableSize];
        cardToSlot = new Integer[config.deckSize];
        ui = new UserInterfaceImpl(config);
        util = new UtilImpl(config);
        env = new Env(logger, config, ui, util);
        table = new Table(env, slotToCard, cardToSlot);
        players = new Player[env.config.players];
        dealer = new Dealer(env, table, players);
        for (int i = 0; i < players.length; i++)
            players[i] = new Player(env, dealer, table, i, i < env.config.humanPlayers);
        targetPlayer = players[0];
        placeCardsOnTable();

            
    }

    private void placeCardsOnTable(){ 
        
        for(int i=0; i<config.tableSize; i++ ){
        table.placeCard(i, i);
        }
    }

    private void placeAllTokensOnValidSet(int player){
        List<int[]> set = env.util.findSets(dealer.getDeck(), 1);
        for(int i = 0; i< set.get(0).length; i++){
            if(table.cardToSlot[(Integer)set.get(0)[i]] != null){
               table.removeCard(table.cardToSlot[(Integer)set.get(0)[i]]);
            }
            if(table.slotToCard[i] != null){
                table.removeCard(i);
            }
            table.placeCard(set.get(0)[i], i);
        }
       table.placeToken(player,0 );
       table.placeToken(player,1 );
       table.placeToken(player,2 );
    }
    private void placeAllTokens(int player){
        if(slotToCard[0] == null)
           table.placeCard(6, 0);
        table.placeToken(player,0 );
        if(slotToCard[1] == null)
           table.placeCard(18, 1);
        table.placeToken(player,1 );
        if(slotToCard[3] == null)
           table.placeCard(19, 3);
        table.placeToken(player,3 );
     }
    @Test
    void testPointing(){ // verifies pointing/penalizing system
        placeAllTokensOnValidSet(players[0].getId()); //tokens on a valid set
        dealer.appendRequest(players[0]);
        int expected = players[0].getScore() +1;
        Thread t = new Thread(()->{
            dealer.removeCardsFromTable();
        });
        t.start();
        synchronized( players[0].getRequestLock()) { try{players[0].getRequestLock().wait();} catch (InterruptedException ig){}}
        t.interrupt();
        assertEquals(expected, players[0].getScore());
        for(Integer token : table.getTokens(players[0].getId())){
           table.removeToken(players[0].getId(), token);
        }
        

        placeAllTokens(players[0].getId()); //tokens on non-set
        dealer.appendRequest(players[0]);
        t = new Thread(()->{
            dealer.removeCardsFromTable();
        });
        t.start();
        synchronized( players[0].getRequestLock()) { try{players[0].getRequestLock().wait();} catch (InterruptedException ig){}}
        t.interrupt();
        assertEquals(expected, players[0].getScore());

    }

    @Test
    void testAnnounceWinners(){ // tests judgment of winners
        for(int i=0; i<env.config.players; i++){
            players[i].point();
        }
        players[0].point();
        int[] winners = new int[1];
        winners[0] = players[0].getId();

        assertArrayEquals(dealer.getWinning(),winners);
    }
}

