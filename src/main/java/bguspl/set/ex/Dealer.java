package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;

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
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    private final Object notifyDealerKey = new Object();

    AtomicInteger timer = new AtomicInteger(60);
    long lastTime ;


    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        lastTime = System.currentTimeMillis();
        while (!shouldFinish()) {
            placeCardsOnTable();
            //sleep, update timer display, remove and place new cards.
            timerLoop();

            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            if(timer.get() == 0) {
                timer.set(60);
                placeCardsOnTable();

            }
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0; //TODO: add condition that the set has no sets as well
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable(int[] cards) {
        // TODO implement

        for(int i=0; i<3; i++){
            table.removeCard(table.cardToSlot[cards[i]]);
            table.placeCard(deck.remove(0),table.cardToSlot[cards[i]] );

        }
    }
    private void removeCardsFromTable() {

        // TODO implement
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        Collections.shuffle(deck);
        System.out.println(deck);
        for(int i=0; i<12 && deck.size()>0; i++){
            table.placeCard(deck.remove(0), i);
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        try{
            sleep(8);
        }
        catch (InterruptedException e){
            System.out.println("Dealer was interrupted while sleeping.");
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if(System.currentTimeMillis() - lastTime >999){
            lastTime=System.currentTimeMillis();
            timer.decrementAndGet();
            env.ui.setCountdown(timer.get()* 1000L,timer.get()<11);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
        for (int i = 0; i < 12 ; i++){
            try{
                table.removeCard(i);
                deck.add(table.slotToCard[i]);
            }
            catch(Exception e ){
                System.out.println("No card in slot " + i +" to be removed.");
            }
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
    }

    public synchronized void notifyDealer(int playerID, Queue<Integer> keysPressed, long timeStamp){

            System.out.println("DEBUG (Dealer) : Player no "+ playerID +" notified dealer of finished set "+ keysPressed);


            int[] chosenSlots = convertQueueToSlots(keysPressed);
            int[] chosenCards = slotsToCards(chosenSlots);

            removeTokens(playerID, chosenSlots);

            if(env.util.testSet(chosenCards)){
                players[playerID].point();
                removeCardsFromTable(chosenCards);
                for(int i=0; i<3 ;i++){
                    table.placeCard(deck.remove(0),chosenSlots[i]);
                }


            }
            else
                players[playerID].penalty();


            notifyAll();
        }




    private void removeTokens (int playerID, int[] slots){
        for(int slot : slots)
            table.removeToken(playerID, slot);
    }

    private int[] convertQueueToSlots(Queue<Integer> keysPressed){
        int[] slots = new int[3];

        for(int i=0; i<3; i++)
            slots[i] = keysPressed.remove();

        return slots;



        }
    private int[] slotsToCards(int[] slots){
        int[] cards = new int[3];
        for(int i=0; i<3; i++)
            cards[i] = table.slotToCard[slots[i]];

        return cards;
    }



    }



