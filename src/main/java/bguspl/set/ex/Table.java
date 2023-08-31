package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

     // The game environment object.
    private final Env env;

     // Mapping between a slot and the card placed in it (null if none).
    protected final Integer[] slotToCard; // card per slot (if any)

     // Mapping between a card and the slot it is in (null if none).
    protected final Integer[] cardToSlot; // slot per card (if any)
     // All tokens by all players
    private List<Integer>[] playerTokens;

    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.playerTokens = new LinkedList[env.config.players];
        for (int i=0; i<env.config.players; i++){
            playerTokens[i] = new LinkedList<Integer>();
        }

    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        env.ui.placeCard(card, slot);
        cardToSlot[card] = slot;
        slotToCard[slot] = card;
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) { // should remove tokens off the removed card
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        synchronized (slotToCard){
            synchronized(cardToSlot){
              Integer card = slotToCard[slot];
              if(card!=null){
                 slotToCard[slot] = null;
                 cardToSlot[card] = null;
              }
            }
        }
        synchronized(playerTokens){
        for(int i=0; i<playerTokens.length;i++){
            if(this.hasTokenOn(i, slot)){
                this.removeToken(i, slot);
            }
        }
        }
        env.ui.removeCard(slot);
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public boolean placeToken(int player, int slot) {
        if(!allTokensPlaced(player) && slot>=0 && slot<env.config.tableSize && slotToCard[slot] != null){
           synchronized(playerTokens){this.playerTokens[player].add((Integer)slot);}
           env.ui.placeToken(player, slot);
           return true;
        }
        return false;
    }

    public boolean allTokensPlaced(int player){

           if(playerTokens[player].size() == env.config.featureSize)
              return true;
           return false;
        
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        boolean ans;
        synchronized(playerTokens){ ans = this.playerTokens[player].remove((Integer)slot);}
        if(ans){
            env.ui.removeToken(player, slot);
        }
        return ans;
    }

    public boolean hasTokenOn(int player, int slot){
           if(playerTokens[player] !=null)
              return playerTokens[player].contains((Integer)slot);
           return false;
    }
    
    public List<Integer> getTokens(int player){
           return this.playerTokens[player];
    }
}
