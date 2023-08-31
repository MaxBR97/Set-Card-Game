package bguspl.set.ex;

import java.util.LinkedList;
import java.util.Queue;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

     // The game environment object.
    private final Env env;

    //Game entities.
    private final Table table;

     // The id of the player (starting from 0).
    public final int id;

     // The thread representing the current player.
    private Thread playerThread;

     // The thread of the AI (computer) player (an additional thread used to generate key presses).
    private Thread aiThread;

    // True iff the player is human (not a computer player).
    private final boolean human;

     // True iff game should be terminated due to an external event.
    private volatile boolean terminate;

     // The current score of the player.
    private int score;

    // Que of pending actions
    private Queue<Integer> actionOnSlot;
    // Is player penalized by dealer
    private boolean isPenalized;
    // Is player credited by dealer
    private boolean isCredited;
    // Did the player change tokens placement since last time he requested dealer's check?
    private boolean hadMovedSinceRequest;
    // The dealer
    private Dealer dealer;
    //locks progression of thread until dealer finishes judging its tokens
    private Object requestLock;


    /**
     * The class constructor.
     *
     * @param env    - the game environment object.
     * @param table  - the table object.
     * @param dealer - the dealer object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.dealer = dealer;
        this.table = table;
        this.id = id;
        this.human = human;
        terminate = false;

        this.actionOnSlot = new LinkedList<Integer>();
        isCredited = false;
        isPenalized = false;
        hadMovedSinceRequest = true;
        requestLock = new Object();
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        playerThread.setName("Player"+id);
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        if (!human) createArtificialIntelligence();
        dealer.notifyCreationLock(); // notifies dealer that this object is created succesfully
        
        while (!terminate) {
            
            if(isPenalized){
                this.applyPenalty();
            }
            if(isCredited){
                this.applyPoint();
            }
            performAction();
            if(table.allTokensPlaced(id) && hadMovedSinceRequest){ // if all tokens are placed, request dealer to check.
                dealer.appendRequest(this);
                hadMovedSinceRequest = false;
             }
             if(!hadMovedSinceRequest){
                synchronized(requestLock){ try{requestLock.wait(3);} catch(InterruptedException ignore){}} //Waits for dealer to finish making the decision. 3 miliseconds maximum - in case player have missed the notification from the dealer.
            }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
       // synchronized(requestLock){try{requestLock.wait(id);} catch(InterruptedException ig){}}
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
        dealer.notifyCreationLock();
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
            synchronized(this) {notifyAll(); }
            while (!terminate) {
             
                try {
                    int slot = 0;
                    synchronized(table){
                    if(table.allTokensPlaced(id)){
                    try{
                        slot = table.getTokens(id).get((int)(Math.random()*env.config.featureSize)); // chooses random token from tokens on table
                    } catch (Exception e) { slot = (int)(Math.random()*env.config.tableSize);}
                    } else{
                    slot = (int)(Math.random()*env.config.tableSize); // chooses random slot on table
                    }
                }
                    
                    keyPressed(slot);
                    synchronized (this) {wait(4);} //Waits for a key press to be requested again. This 4 milisecond wait is in case the thread missed the notify from the the performAction()
                } catch (InterruptedException ignored) {} 
            }
            keyPressed(-1);
            System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
        }, "computer-" + id);
        aiThread.setName("AI"+this.getId());
        aiThread.start();
        synchronized(this) {try{wait();} catch(InterruptedException ig){}}
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        terminate = true;
        if(human)
           keyPressed(-1); // to avoid waiting for input
    }

    public void performAction(){
        int slot; // perform action on slot
        synchronized (actionOnSlot){
           while (actionOnSlot.isEmpty() && !terminate){
            if(!human){
              synchronized (this){ this.notifyAll();}
            }
            try{actionOnSlot.wait();} catch(InterruptedException ignore){ System.out.println(" thread of player interrupted - "+this.id);} 
            synchronized(table){
            if((table.allTokensPlaced(id)) && actionOnSlot.peek() != null && !table.hasTokenOn(id, actionOnSlot.peek())){ //invalid move
               actionOnSlot.remove();
            }
        }
        
           }
            slot = actionOnSlot.remove();
        }
            if(table.hasTokenOn(id, slot)){
                if(table.removeToken(id, slot))
                   hadMovedSinceRequest = true;
            }
            else {
               if(table.placeToken(id, slot))
                  hadMovedSinceRequest = true;
            }
       
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) { //IMPLEMENTED
        synchronized (actionOnSlot){
        if(actionOnSlot.size() < env.config.featureSize)
           actionOnSlot.add(slot);
        actionOnSlot.notifyAll();
        }
    }

    public Queue<Integer> getActionOnSlot(){ // for tests
        return this.actionOnSlot;
    }

    /**
     * Award a point to a player and perform other related actions.
     * @throws InterruptedException
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        score++;
        isCredited = true;
        synchronized (requestLock){requestLock.notifyAll();}
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, score);
    }
    public void applyPoint(){
        isCredited = false;
                try{
                    int freezeTime = (int)(env.config.pointFreezeMillis);
                    env.ui.setFreeze(id, freezeTime);
                    while(freezeTime>=1000){
                      env.ui.setFreeze(id, freezeTime);
                      Thread.sleep(1000);
                      freezeTime -=1000;
                    }
                    Thread.sleep(freezeTime);
                    synchronized (actionOnSlot){this.actionOnSlot.clear();}
                    env.ui.setFreeze(id, 0);
                    int ignored = table.countCards(); // this part is just for demonstration in the unit tests
                    }catch (InterruptedException ignore){}
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
       isPenalized = true;
       synchronized (requestLock){requestLock.notifyAll();}
    }
    public void applyPenalty(){
        isPenalized = false;
                try{
                    int freezeTime = (int)(env.config.penaltyFreezeMillis);
                    env.ui.setFreeze(id, freezeTime);
                    while(freezeTime>=1000){
                      env.ui.setFreeze(id, freezeTime);
                      Thread.sleep(1000);
                      freezeTime -= 1000;
                    }
                    Thread.sleep(freezeTime);
                    synchronized (actionOnSlot){this.actionOnSlot.clear();}
                    env.ui.setFreeze(id, 0);
                    }catch (InterruptedException ignore){}

    }
    public Object getRequestLock(){
        return requestLock;
    }
    public int getScore() {
        return score;
    }

    public int getId() {
        return id;
    }
    public boolean getIsHuman(){
        return human;
    }

    public boolean isPenalized(){
        return isPenalized;
    }

    public boolean isCredited(){
        return isCredited;
    }

  /*   public int[] getTokensPlacement(){
        return this.tokensPlacement;
    }*/
}

/*synchronized(actionOnSlot){
            if(!this.actionOnSlot.isEmpty()){
                int slot = actionOnSlot.remove();
                boolean flag = true;
                int freeToken=-1;
                synchronized (tokensPlacement){
                   for(int i = 0; i<tokensPlacement.length; i++){
                      if(tokensPlacement[i] == slot){
                         table.removeToken(this.id, slot);
                         tokensPlacement[i] = -1;
                         flag = false;
                       }
                       else if(tokensPlacement[i] == -1)
                          freeToken = i;      
                    }
                    if(flag && freeToken!=-1){
                       table.placeToken(this.id, slot);
                       tokensPlacement[freeToken] = slot;
                    }
                
                hadMovedSinceRequest = true;
            }
            boolean allTokensPlaced = true;
            for(int i: tokensPlacement){
                if(i ==-1)
                   allTokensPlaced = false;
            }
            if(allTokensPlaced && hadMovedSinceRequest){
                dealer.appendRequest(this);
                hadMovedSinceRequest = false;
            }
        }

    } */