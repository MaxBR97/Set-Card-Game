package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;
    /**
     * The time when the dealer countdown times out (at which point he must collect the cards and reshuffle the deck).
     */
    private long countdownUntil;

    private Queue<Player> pendingRequests;

    private boolean shuffle;

    private boolean isWarningTime;

    private Object threadCreationLock;

    private Stack<Thread> allPlayerThreads;

    private HashMap<Thread,Player> threadToPlayer;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        terminate = false;
        pendingRequests = new LinkedList<Player>();
        shuffle = false;
        isWarningTime = false;
        threadCreationLock = new Object();

    }
    public List<Integer> getDeck(){
        return deck;
    }

    public void notifyCreationLock(){
        synchronized (threadCreationLock){threadCreationLock.notifyAll();}
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        allPlayerThreads = new Stack<Thread>();
        threadToPlayer = new HashMap<Thread,Player>();
        for(Player p : this.players){
            Thread playerThread = new Thread(p);
            allPlayerThreads.push(playerThread);
            threadToPlayer.put(playerThread, p);
            playerThread.start();
            synchronized(threadCreationLock){try{threadCreationLock.wait();}catch(InterruptedException ig){}} //waits for thread to fully initialize
        }
        while (!shouldFinish()) {
            Collections.shuffle(deck);
            placeCardsOnTable();
            for(Player p :players){ // notify all players that rearrangement is done
                synchronized (p){p.notifyAll();}
            }
            countdownLoop();
                synchronized(table){ //judges last pending requests from players before redistributing cards
                  removeCardsFromTable();
                  removeAllCardsFromTable();
                }
               
         
        }
        terminate();
        announceWinners();
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void countdownLoop() {
        
        resetCountdown();   
        while (!terminate && System.currentTimeMillis() < countdownUntil) {
            updateCountdown();
            sleepUntilWokenOrTimeout();
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        System.out.println("Program terminates by order");
        while(!allPlayerThreads.isEmpty())
        {
            Thread t = allPlayerThreads.pop();
            Player p = threadToPlayer.get(t);
            p.terminate();
            try{t.join();} catch(InterruptedException ig){}
        }
        this.terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        List<Integer> onTable = new LinkedList<Integer>();
        for(int i=0; i<env.config.tableSize; i++){
            if(table.slotToCard[i]!=null)
              onTable.add(table.slotToCard[i]);
        }
        List<Integer> allCards = new LinkedList<Integer>();
        allCards.addAll(deck);
        allCards.addAll(onTable);
        return terminate || env.util.findSets(allCards, 1).size() == 0;
    }

    /**
     * Checks if any cards should be removed from the table and returns them to the deck.
     */
    public void removeCardsFromTable() {  // MADE PUBLIC SO THAT UNIT TEST COULD ACCESS IT
        while(!pendingRequests.isEmpty())
        {
            Player p;
            synchronized(pendingRequests){
               p = pendingRequests.remove();
            }
            List<Integer> tokens;
            synchronized (table){
            tokens = table.getTokens(p.getId());
            int[] cards = new int[env.config.featureSize];
            try{
            for(int i=0; i<tokens.size(); i++){
                cards[i] = table.slotToCard[tokens.get(i)];
            }
            boolean flag = env.util.testSet(cards);
            if(flag && tokens.size() == env.config.featureSize)
            {
                for(int i=0; i<tokens.size(); i++){
                    
                      table.removeCard(tokens.get(i));
                    
                }
                p.point();
                resetCountdown();
            }   
            else if(tokens.size() == env.config.featureSize){
                p.penalty();
            }
            } catch (Exception e) {synchronized(p.getRequestLock()){p.getRequestLock().notifyAll();} } // This exception occurs if someone was in queue first to claim the point of some card of some set and the second player is no longer relevant for it
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        if(shuffle){
            Collections.shuffle(deck);
            shuffle = false;
        }
        List<Integer> freeSlots = new LinkedList<Integer>();
        for(int i=0; i<env.config.tableSize;i++){
            if(table.slotToCard[i] == null)
               freeSlots.add(i);
        }
        Collections.shuffle(freeSlots);
            for(Integer i: freeSlots){
                if(!this.deck.isEmpty() && table.slotToCard[i] == null)
                   table.placeCard(this.deck.remove(0), i);
            }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     * @throws InterruptedException
     */
    private synchronized void sleepUntilWokenOrTimeout() {
        if(!isWarningTime){
        try{
           wait(1000);
        } catch (InterruptedException ignored){}
       }
    }

    /**
     * Update the countdown display.
     */
    private void updateCountdown() {
        int milis = (int)(countdownUntil - System.currentTimeMillis());
        if (milis > env.config.turnTimeoutWarningMillis)
           isWarningTime = false;
        else
           isWarningTime = true;

        env.ui.setCountdown(milis, isWarningTime);
    }     

    /**
     * Reset the countdown timer and update the countdown display.
     */
    private void resetCountdown() {
        if (env.config.turnTimeoutMillis > 0) {
            countdownUntil = System.currentTimeMillis() + env.config.turnTimeoutMillis;
            shuffle = true;
            updateCountdown();
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        List<Integer> freeSlots = new LinkedList<Integer>();
        for(int i=0; i<env.config.tableSize;i++){
            if(table.slotToCard[i] != null)
               freeSlots.add(i);
        }
        Collections.shuffle(freeSlots);
        for(Integer i : freeSlots)
        {
            Integer card = table.slotToCard[i];
            table.removeCard(i);
            if(card != null)
            this.deck.add(card);
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int maxScore = 0;
        int count = 0;
        for(Player p: players){
           if(p.getScore()>maxScore){
              maxScore = p.getScore();
              count = 1;
           }
           else if(p.getScore() == maxScore){
              count++;
           }
        }
        int[] winners = new int[count];
        int index = 0;
        for(Player p: players){
            if(p.getScore() == maxScore){
               winners[index] = p.id;
               index++; 
            }
        }
        env.ui.announceWinner(winners);
        
    }

    public synchronized void appendRequest(Player p) {
        synchronized (this.pendingRequests) {
           this.pendingRequests.add(p);
           this.pendingRequests.notify();
        }
        this.notify();
    }
    public int[] getWinning(){ //used for tests only
        int maxScore = 0;
        int count = 0;
        for(Player p: players){
           if(p.getScore()>maxScore){
              maxScore = p.getScore();
              count = 1;
           }
           else if(p.getScore() == maxScore){
              count++;
           }
        }
        int[] winners = new int[count];
        int index = 0;
        for(Player p: players){
            if(p.getScore() == maxScore){
               winners[index] = p.id;
               index++; 
            }
        }
        return winners;
    }
}
