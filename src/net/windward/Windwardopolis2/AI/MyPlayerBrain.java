/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * As long as you retain this notice you can do whatever you want with this
 * stuff. If you meet an employee from Windward some day, and you think this
 * stuff is worth it, you can buy them a beer in return. Windward Studios
 * ----------------------------------------------------------------------------
 */

package net.windward.Windwardopolis2.AI;

import com.sun.xml.internal.ws.api.pipe.PipelineAssembler;
import net.windward.Windwardopolis2.api.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import net.windward.Windwardopolis2.AI.PlayerAIBase.STATUS;

import static net.windward.Windwardopolis2.api.MapSquare.*;

/**
 * The sample C# AI. Start with this project but write your own code as this is a very simplistic implementation of the AI.
 */
public class MyPlayerBrain implements net.windward.Windwardopolis2.AI.IPlayerAI {
    private static String NAME = "Team 1";

    public static String SCHOOL = "CSM";

    private static Logger log = Logger.getLogger(IPlayerAI.class);

    /**
     * The name of the player.
     */
    private String privateName;
    private boolean gettingCoffee = false;

    public final String getName() {
        return privateName;
    }

    private void setName(String value) {
        privateName = value;
    }

    /**
     * The game map.
     */
    private Map privateGameMap;

    public final Map getGameMap() {
        return privateGameMap;
    }

    private void setGameMap(Map value) {
        privateGameMap = value;
    }

    /**
     * All of the players, including myself.
     */
    private java.util.ArrayList<Player> privatePlayers;

    public final java.util.ArrayList<Player> getPlayers() {
        return privatePlayers;
    }

    private void setPlayers(java.util.ArrayList<Player> value) {
        privatePlayers = value;
    }

    /**
     * All of the companies.
     */
    private java.util.ArrayList<Company> privateCompanies;

    public final java.util.ArrayList<Company> getCompanies() {
        return privateCompanies;
    }

    private void setCompanies(java.util.ArrayList<Company> value) {
        privateCompanies = value;
    }

    /**
     * All of the passengers.
     */
    private java.util.ArrayList<Passenger> privatePassengers;

    public final java.util.ArrayList<Passenger> getPassengers() {
        return privatePassengers;
    }

    private void setPassengers(java.util.ArrayList<Passenger> value) {
        privatePassengers = value;
    }

    /**
     * All of the coffee stores.
     */
    private java.util.ArrayList<CoffeeStore> privateStores;

    public final ArrayList<CoffeeStore> getCoffeeStores() { return privateStores; }

    private void setCoffeeStores(ArrayList<CoffeeStore> value) { privateStores = value; }

    /**
     * The power up deck
     */
    private ArrayList<PowerUp> privatePowerUpDeck;

    public final ArrayList<PowerUp> getPowerUpDeck() { return privatePowerUpDeck; }

    private void setPowerUpDeck(ArrayList<PowerUp> value) { privatePowerUpDeck = value; }


    /**
     * My power up hand
     */
    private ArrayList<PowerUp> privatePowerUpHand;

    public final ArrayList<PowerUp> getPowerUpHand() { return privatePowerUpHand; }

    private void setPowerUpHand(ArrayList<PowerUp> value) { privatePowerUpHand = value; }

	private PowerUp powerPlaying;
	public final PowerUp getPlaying() { return powerPlaying; }

	private void setPlaying(PowerUp value) { powerPlaying = value; }

    /**
     * Me (my player object).
     */
    private Player privateMe;

    public final Player getMe() {
        return privateMe;
    }

    private void setMe(Player value) {
        privateMe = value;
    }

    /**
     * My current passenger
     */
    private Passenger privateMyPassenger;

	private Passenger whosNext;

    public final Passenger getMyPassenger() { return privateMyPassenger; }

    private void setMyPassenger(Passenger value) { privateMyPassenger = value; }


	public final Passenger getNext() { return whosNext; }

	private void setNext(Passenger value) { whosNext = value; }

    private PlayerAIBase.PlayerOrdersEvent sendOrders;

    private PlayerAIBase.PlayerCardEvent playCards;

    /**
     * MINES
     * BEGIN ADDED FIELDS
     */

    // Target Passenger
    private Passenger target;

    // Pickup priority list
    private ArrayList<Passenger> pickup = new ArrayList<Passenger>();

    // Intended path to destination
    private ArrayList<Point> myPath = new ArrayList<Point>();

    // ENUM values for intersections
    private ArrayList<DIRECTION> inters = new ArrayList<DIRECTION>(Arrays.asList(DIRECTION.INTERSECTION, DIRECTION.T_NORTH, DIRECTION.T_EAST, DIRECTION.T_SOUTH, DIRECTION.T_WEST));

    /**
     * END ADDED FIELDS
     */

    /**
     * The maximum number of trips allowed before a refill is required.
     */
    private static final int MAX_TRIPS_BEFORE_REFILL = 3;

    private static final java.util.Random rand = new java.util.Random();

    public MyPlayerBrain(String name) {
        setName(!net.windward.Windwardopolis2.DotNetToJavaStringHelper.isNullOrEmpty(name) ? name : NAME);
        privatePowerUpHand = new ArrayList<PowerUp>();
    }

    /**
     * The avatar of the player. Must be 32 x 32.
     */
    public final byte[] getAvatar() {
        try {
            // open image
            InputStream stream = getClass().getResourceAsStream("/net/windward/Windwardopolis2/res/MyAvatar.png");

            byte [] avatar = new byte[stream.available()];
            stream.read(avatar, 0, avatar.length);
            return avatar;

        } catch (IOException e) {
            log.warn("error reading image");
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Called at the start of the game.
     *
     * @param map         The game map.
     * @param me          You. This is also in the players list.
     * @param players     All players (including you).
     * @param companies   The companies on the map.
     * @param passengers  The passengers that need a lift.
     * @param ordersEvent Method to call to send orders to the server.
     */
    public final void Setup(Map map, Player me, java.util.ArrayList<Player> players, java.util.ArrayList<Company> companies, ArrayList<CoffeeStore> stores,
                            java.util.ArrayList<Passenger> passengers, ArrayList<PowerUp> powerUps, PlayerAIBase.PlayerOrdersEvent ordersEvent, PlayerAIBase.PlayerCardEvent cardEvent) {

        try {
            setGameMap(map);
            setPlayers(players);
            setMe(me);
            setCompanies(companies);
            setPassengers(passengers);
            setCoffeeStores(stores);
            setPowerUpDeck(powerUps);
            sendOrders = ordersEvent;
            playCards = cardEvent;

            java.util.ArrayList<Passenger> pickup = AllPickups(me, passengers);

            // get the path from where we are to the dest.
            java.util.ArrayList<Point> path = CalculatePathPlus1(me, pickup.get(0).getLobby().getBusStop());
            sendOrders.invoke("ready", path, pickup);

            // set logger to debug mode
            Logger.getRootLogger().setLevel(Level.DEBUG);
        } catch (RuntimeException ex) {
            log.fatal("setup(" + me == null ? "NULL" : me.getName() + ") Exception: " + ex.getMessage());
            ex.printStackTrace();

        }
    }

	public void handleCoffee(PlayerAIBase.STATUS status){
		if(gettingCoffee && status != STATUS.NO_PATH)
            return;

		Point ptDest = getNearestCoffeeStore();
		gettingCoffee = true;
		log.info("Making a b-line coffee!");

		doSend(status, ptDest, null);
	}


	public final void GameStatus(PlayerAIBase.STATUS status, Player plyrStatus) {

		// bugbug - Framework.cs updates the object's in this object's Players, Passengers, and Companies lists. This works fine as long
		// as this app is single threaded. However, if you create worker thread(s) or respond to multiple status messages simultaneously
		// then you need to split these out and synchronize access to the saved list objects.

		try {
			// bugbug - we return if not us because the below code is only for when we need a new path or our limo hit a bus stop.
			// if you want to act on other players arriving at bus stops, you need to remove this. But make sure you use Me, not
			// plyrStatus for the Player you are updatiing (particularly to determine what tile to start your path from).
			if (plyrStatus != getMe()) {
				return;
			}

			if(getMe().getLimo().getCoffeeServings() == 0){
				handleCoffee(status);
				return;
			}

			if(status == PlayerAIBase.STATUS.UPDATE) {
				MaybePlayPowerUp();
				return;
			}

			DisplayStatus(status, plyrStatus);

			if(log.isDebugEnabled())
				log.info("gameStatus( " + status + " )");

			Point ptDest = null;
			java.util.ArrayList<Passenger> pickup = new java.util.ArrayList<Passenger>();
			switch (status) {
				case NO_PATH:
				case PASSENGER_NO_ACTION:
					if (getMe().getLimo().getPassenger() == null) {
						pickup = AllPickups(plyrStatus, getPassengers());
						ptDest = pickup.get(0).getLobby().getBusStop();
					} else {
						ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
					}
					break;
				case PASSENGER_DELIVERED:
				case PASSENGER_ABANDONED:
					pickup = AllPickups(getMe(), getPassengers());
					ptDest = pickup.get(0).getLobby().getBusStop();
					break;
				case PASSENGER_REFUSED_ENEMY:
					//add in random so no refuse loop
					java.util.List<Company> comps = getCompanies();
					while(ptDest == null) {
						int randCompany = rand.nextInt(comps.size());
						if (comps.get(randCompany) != getMe().getLimo().getPassenger().getDestination()) {
							ptDest = comps.get(randCompany).getBusStop();
							break;
						}
					}
					break;
				case PASSENGER_DELIVERED_AND_PICKED_UP:
				case PASSENGER_PICKED_UP:
					pickup = AllPickups(getMe(), getPassengers());
					ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
					break;

			}

			// coffee store override
			switch (status)
			{
				case PASSENGER_DELIVERED_AND_PICKED_UP:
				case PASSENGER_DELIVERED:
				case PASSENGER_ABANDONED:
					if (getMe().getLimo().getCoffeeServings() <= 0) {
						java.util.List<CoffeeStore> cof = getCoffeeStores();
						int randCof = rand.nextInt(cof.size());
						ptDest = cof.get(randCof).getBusStop();
					}
					break;
				case PASSENGER_REFUSED_NO_COFFEE:
				case PASSENGER_DELIVERED_AND_PICK_UP_REFUSED:
					java.util.List<CoffeeStore> cof = getCoffeeStores();
					int randCof = rand.nextInt(cof.size());
					ptDest = cof.get(randCof).getBusStop();
					break;
				case COFFEE_STORE_CAR_RESTOCKED:
					gettingCoffee = false;
					pickup = AllPickups(getMe(), getPassengers());
					if (pickup.size() == 0)
						break;
					ptDest = pickup.get(0).getLobby().getBusStop();
					break;
			}

			// may be another status
			if(ptDest == null)
				return;

			doSend(status, ptDest, pickup);

		} catch (RuntimeException ex) {
			ex.printStackTrace();
		}
	}

    /**
     * Called to send an update message to this A.I. We do NOT have to send orders in response.
     *
     * @param status     The status message.
     * @param plyrStatus The player this status is about. THIS MAY NOT BE YOU.
     */
    public final void GameStatus2(PlayerAIBase.STATUS status, Player plyrStatus) {
        // bugbug - Framework.cs updates the object's in this object's Players, Passengers, and Companies lists. This works fine as long
        // as this app is single threaded. However, if you create worker thread(s) or respond to multiple status messages simultaneously
        // then you need to split these out and synchronize access to the saved list objects.

        try {
            // bugbug - we return if not us because the below code is only for when we need a new path or our limo hit a bus stop.
            // if you want to act on other players arriving at bus stops, you need to remove this. But make sure you use Me, not
            // plyrStatus for the Player you are updatiing (particularly to determine what tile to start your path from).


            if (!plyrStatus.getName().equals(getMe().getName())) {
                // MINES
                // IF THE GAME STATE CHANGES AT ALL, REEVALUATE OUR PATH
                // OVERLY AGGRESSIVE -- RESULTS IN WAFFLING
                //return;

                // If a player invalidates our delivery, recalculate
                if(getMe().getLimo().getPassenger() == null){
                    if(status == STATUS.PASSENGER_PICKED_UP && plyrStatus.getLimo().getPassenger().equals(target)){
                        status = STATUS.NO_PATH;
                    }
                }
                else if(status == STATUS.PASSENGER_DELIVERED ||
                        status == STATUS.PASSENGER_DELIVERED_AND_PICKED_UP ||
                        status == STATUS.POWER_UP_PLAYED){
                    for(Passenger other : getMe().getLimo().getPassenger().getEnemies()){
                        if(other.getLobby() == null){
                            continue;
                        }
                        if(other.getLobby().getName().equals(getMe().getLimo().getPassenger().getDestination().getName())){
                            status = STATUS.NO_PATH;
                        }
                    }
                }
                else{
                    return;
                }
            }

	        if (plyrStatus.getName().equals(getMe().getName()) && status != STATUS.UPDATE)
	            DisplayStatus(status, plyrStatus);

            if(log.isDebugEnabled())
                log.info("gameStatus( " + status + " )");

            Point ptDest = null;

            if(gettingCoffee && status != STATUS.NO_PATH){
                return;
            }

	        if(getMe().getLimo().getCoffeeServings() == 0){
		        ptDest = getNearestCoffeeStore();
		        gettingCoffee = true;
		        log.info("Making a b-line coffee!");
	        }

            if(status == PlayerAIBase.STATUS.UPDATE) {
                MaybePlayPowerUp();
                Point maybeCoffee = getNearestCoffeeStore();
                if(getMe().getLimo().getPassenger() == null && CalculatePathPlus1(getMe(), maybeCoffee).size() < (9 - 3 * getMe().getLimo().getCoffeeServings())){
                    ptDest = maybeCoffee;
                    gettingCoffee = true;
	                log.info("Making a detour for coffee!");
                } else
	                return;
            }

            java.util.ArrayList<Passenger> pickup = new java.util.ArrayList<Passenger>();

	        if(ptDest == null){

            switch (status) {
	            case PASSENGER_REFUSED_ENEMY:
                case NO_PATH:
                    if(getMe().getLimo().getPassenger() != null){
                        //System.out.println("Continue to passenger destination");
                        ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
                        boolean continueCurrentCourse = true;
                        for(Passenger waiting : getMe().getLimo().getPassenger().getDestination().getPassengers()){
                            continueCurrentCourse = !getMe().getLimo().getPassenger().getEnemies().contains(waiting);
                            if(!continueCurrentCourse){
                                log.info("(!) Enemy detected at destination!");
                                break;
                            }
                        }
                        if(continueCurrentCourse){
                            break;
                        }
                    }
                    target = null;
                    double currCost = Double.MAX_VALUE;
                    double tempCost = 0;
                    ArrayList<Point> tempPath1 = null;
                    ArrayList<Point> tempPath2 = null;
                    ArrayList<Point> finalPath = null;
                    boolean skip = false;
                    for (Passenger p : getPassengers()){
                        // Check if we've delivered them before or they're out of destinations or they're in transit
                        if(getMe().getPassengersDelivered().contains(p) || p.getDestination() == null || p.getLobby() == null){
                            continue;
                        }
                        // Check for enemies at destination
                        if (p.getEnemies().size() > 0){
                            for (Passenger e : p.getEnemies()){
                                if(p.getDestination() != null &&
                                        (( e.getLobby() != null && e.getLobby().equals(p.getDestination())) ||
                                                ( e.getLobby() == null && e.getDestination().equals(p.getDestination()) ) )
                                        ){
                                    skip = true;
                                    break;
                                }
                            }
                        }

	                    for (Player play : getPlayers()){
		                    if (play.getPickUp().contains(p)){
			                    if (play.getLimo().getPath().size() > 0 && play.getLimo().getPath().get(play.getLimo().getPath().size() - 1) == p.getLobby().getBusStop()){
				                    if (SimpleAStar.CalculatePath(privateGameMap, privateMe.getLimo().getMapPosition(), p.getLobby().getBusStop()).size() < SimpleAStar.CalculatePath(privateGameMap, play.getLimo().getMapPosition(), p.getLobby().getBusStop()).size()){
					                    skip = true;
				                    }
			                    }
		                    }
	                    }

                        if(skip){
                            skip = false; // reset flag
                            continue;
                        }
                        // Distance of path (shorter better) divided by value of target (higher better)
                        // Lower tempCost correlates to better target
                        tempPath1 = SimpleAStar.CalculatePath(privateGameMap, privateMe.getLimo().getMapPosition(), p.getLobby().getBusStop());
                        tempPath2 = SimpleAStar.CalculatePath(privateGameMap, p.getLobby().getBusStop(), p.getDestination().getBusStop());
                        // If we have a passenger and they have an enemy at the potential target's location, skip that target
                        if(getMe().getLimo().getPassenger() != null){
                            boolean enemyAtTarget = false;
                            for(Passenger waiting : p.getLobby().getPassengers()){
                                enemyAtTarget = getMe().getLimo().getPassenger().getEnemies().contains(waiting);
                            }
                            if(enemyAtTarget){
                                continue;
                            }
                        }
                        tempCost = (tempPath1.size()*2+tempPath2.size())/p.getPointsDelivered();
                        if (currCost > tempCost){
                            currCost = tempCost;
                            target = p;
                            finalPath = tempPath1;
                            finalPath.addAll(tempPath2);
                        }
                    }
                    if(target != null && target.getLobby() != null){
                        log.info("Chose a new target: " + target.getName() + " at " + target.getLobby());

                        myPath = finalPath;
                        //System.out.println(myPath.size());
                        ptDest = target.getLobby().getBusStop();
                        pickup.add(target);

	                    pickup.addAll(AllPickups(getMe(), getPassengers()));

                    }
                    log.info("No path, and no viable target found.");
                    break;
                case PASSENGER_NO_ACTION:
                    if (getMe().getLimo().getPassenger() == null) {
                        pickup = AllPickups(plyrStatus, getPassengers());
                        ptDest = pickup.get(0).getLobby().getBusStop();
                    } else {
                        ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
                    }
                    break;
                case PASSENGER_DELIVERED:
                    if(getMe().getLimo().getCoffeeServings() <= 0){
                        log.info("Delivered ... no coffee left!");
                        ptDest = getNearestCoffeeStore();
                    }
                    break;
                case PASSENGER_ABANDONED:
                    // TODO: Actually pick a passenger intelligently
                    pickup = AllPickups(getMe(), getPassengers());
                    ptDest = pickup.get(0).getLobby().getBusStop();
                    break;
               /* case PASSENGER_REFUSED_ENEMY:
                    //add in random so no refuse loop
                    // TODO: Pick a passenger smartly? Or drop off smartly.
                    *//*java.util.List<Company> comps = getCompanies();
                    while(ptDest == null) {
                        int randCompany = rand.nextInt(comps.size());
                        if (comps.get(randCompany) != getMe().getLimo().getPassenger().getDestination()) {
                            ptDest = comps.get(randCompany).getBusStop();
                            break;
                        }
                    }*//*

	                status = STATUS.NO_PATH;

                    break;*/
                case PASSENGER_DELIVERED_AND_PICKED_UP:
                case PASSENGER_PICKED_UP:
                    pickup = AllPickups(getMe(), getPassengers());

	                if(getMe().getLimo().getPassenger() != null)
                        ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
                    break;

            }

            // coffee store override
            switch (status)
            {
                case PASSENGER_DELIVERED_AND_PICKED_UP:
                case PASSENGER_DELIVERED:
                case PASSENGER_ABANDONED:
                    if (getMe().getLimo().getCoffeeServings() <= 0) {
                        log.info("Need coffee! Ran out on delivery.");
                        ptDest = getNearestCoffeeStore();
	                    gettingCoffee = true;
                    }
                    break;
                case PASSENGER_REFUSED_NO_COFFEE:
                case PASSENGER_DELIVERED_AND_PICK_UP_REFUSED:
                    ptDest = getNearestCoffeeStore();
                    log.info("Need coffee! Refused pickup/dropoff.");
	                gettingCoffee = true;
                    break;
                case COFFEE_STORE_CAR_RESTOCKED:
                    // TODO: Reeval pickups
                    log.info("Reacquired coffee!");
                    pickup = AllPickups(getMe(), getPassengers());
                    if (pickup.size() == 0)
                        break;
                    ptDest = pickup.get(0).getLobby().getBusStop();
	                gettingCoffee = false;
                    break;
            }
	        }

            // may be another status
            if(ptDest == null && pickup.size() == 0)
                return;

	        if(ptDest != null)
                DisplayOrders(ptDest);

            // get the path from where we are to the dest.
            java.util.ArrayList<Point> path = new ArrayList<Point>();
	        if(ptDest != null)
		        path = CalculatePathPlus1(getMe(), ptDest);

            if (log.isDebugEnabled())
            {
                log.debug(status + "; Path:" + (path.size() > 0 ? path.get(0).toString() : "{n/a}") + "-" + (path.size() > 0 ? path.get(path.size()-1).toString() : "{n/a}") + ", " + path.size() + " steps; Pickup:" + (pickup.size() == 0 ? "{none}" : pickup.get(0).getName()) + ", " + pickup.size() + " total");
            }

            // update our saved Player to match new settings
            if (path.size() > 0) {
                getMe().getLimo().getPath().clear();
                getMe().getLimo().getPath().addAll(path);
            }

	        whosNext = null;
            if (pickup.size() > 0) {
                getMe().getPickUp().clear();
	            whosNext = pickup.get(0);
                getMe().getPickUp().addAll(pickup);
            }
            if(getMe().getPickUp().size() < 1){
                log.debug("Sent Pickup<> with no entries!");
            }
            sendOrders.invoke("move", path, pickup);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
    }

	private void doSend(PlayerAIBase.STATUS status, Point ptDest, ArrayList<Passenger> pickup) {
		if(ptDest != null)
			DisplayOrders(ptDest);

		if(pickup == null)
			pickup = new ArrayList<Passenger>();

		// get the path from where we are to the dest.
		java.util.ArrayList<Point> path = new ArrayList<Point>();
		if(ptDest != null)
			path = CalculatePathPlus1(getMe(), ptDest);

		if (log.isDebugEnabled()) {
			log.debug(status + "; Path:" + (path.size() > 0 ? path.get(0).toString() : "{n/a}") + "-" + (path.size() > 0 ? path.get(path.size()-1).toString() : "{n/a}") + ", " + path.size() + " steps; Pickup:" + (pickup.size() == 0 ? "{none}" : pickup.get(0).getName()) + ", " + pickup.size() + " total");
		}

		// update our saved Player to match new settings
		if (path.size() > 0) {
			getMe().getLimo().getPath().clear();
			getMe().getLimo().getPath().addAll(path);
		}

		whosNext = null;
		if (pickup.size() > 0) {
			getMe().getPickUp().clear();
			whosNext = pickup.get(0);
			getMe().getPickUp().addAll(pickup);
		}
		if(getMe().getPickUp().size() < 1){
			log.debug("Sent Pickup<> with no entries!");
		}
		sendOrders.invoke("move", path, pickup);
	}

	private void MaybePlayPowerUp() {
		// can we play one?
		PowerUp pu2 = null;
		Passenger pass2 = null;
		Player play2 = null;
		boolean shouldBreak = false;
		for(PowerUp current : getPowerUpHand()) {
			if(current.isOkToPlay()) {
				if(current.getCard() == PowerUp.CARD.MULT_DELIVER_AT_COMPANY ||
						current.getCard() == PowerUp.CARD.MULT_DELIVERING_PASSENGER ||
						current.getCard() == PowerUp.CARD.MULT_DELIVERY_QUARTER_SPEED)  {

					Passenger pass = getNext();
					if(pass != null && Powerups.checkPowerUp(this, current, null, pass)){
						pu2 = current;
						pass2 = pass;
						play2 = null;
						break;
					}

				} else {
					for(Passenger pass : getPassengers()){
						for(Player play : getPlayers()){
							if(Powerups.checkPowerUp(this, current, play, pass)){
								pu2 = current;
								pass2 = pass;
								play2 = play;
								shouldBreak = true;
								break;
							}
						}

						if(shouldBreak)
							break;
					}

					if(shouldBreak)
						break;
				}
			}
		}

		if (pu2 != null)  {
			if (log.isInfoEnabled())
				log.info("Request play card " + pu2);

			Powerups.setAndHandlePowerUp(this, pu2, play2, pass2);
			playCards.invoke(PlayerAIBase.CARD_ACTION.PLAY, pu2);
			privatePowerUpHand.remove(pu2);
		}

		if (getPowerUpHand().size() < getMe().getMaxCardsInHand() && getPowerUpDeck().size() > 0) {
			for (int index = 0; index < getMe().getMaxCardsInHand() - getPowerUpHand().size() && getPowerUpDeck().size() > 0; index++) {
				// select a card
				PowerUp pu = getPowerUpDeck().get(0);
				privatePowerUpDeck.remove(pu);

				if(pu.getCard() == PowerUp.CARD.MULT_DELIVERY_QUARTER_SPEED){
					continue;
					//playCards.invoke(PlayerAIBase.CARD_ACTION.DISCARD, pu);
				}

				privatePowerUpHand.add(pu);
				playCards.invoke(PlayerAIBase.CARD_ACTION.DRAW, pu);
			}
		}
	}

    private Point getNearestCoffeeStore() {
        int currCost = Integer.MAX_VALUE;
        ArrayList<Point> chosen = null;
        for(CoffeeStore cs : getCoffeeStores()){
            ArrayList<Point> path = CalculatePathPlus1(getMe(), cs.getBusStop());
            if(path.size() < currCost){
                chosen = path;
                currCost = path.size();
            }
        }
        return chosen.get(chosen.size()-1);
    }

//    private Point getBestPickup(){
//        Point ptDest = null;
//        if(getMe().getLimo().getPassenger() != null){
//            //System.out.println("Continue to passenger destination");
//            ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
//            boolean continueCurrentCourse = true;
//            for(Passenger waiting : getMe().getLimo().getPassenger().getDestination().getPassengers()){
//                continueCurrentCourse = !getMe().getLimo().getPassenger().getEnemies().contains(waiting);
//                if(!continueCurrentCourse){
//                    log.info("(!) Enemy detected at destination!");
//                    break;
//                }
//            }
//            if(continueCurrentCourse){
//                ArrayList<Point> curr = getMe().getLimo().getPath();
//                return curr.get(curr.size()-1);
//            }
//        }
//        target = null;
//        double currCost = Double.MAX_VALUE;
//        double tempCost = 0;
//        ArrayList<Point> tempPath1 = null;
//        ArrayList<Point> tempPath2 = null;
//        ArrayList<Point> finalPath = null;
//        boolean skip = false;
//        for (Passenger p : getPassengers()){
//            // Check if we've delivered them before or they're out of destinations or they're in transit
//            if(getMe().getPassengersDelivered().contains(p) || p.getDestination() == null || p.getLobby() == null){
//                continue;
//            }
//            // Check for enemies at destination
//            if (p.getEnemies().size() > 0){
//                for (Passenger e : p.getEnemies()){
//                    if(p.getDestination() != null &&
//                            (( e.getLobby() != null && e.getLobby().equals(p.getDestination())) ||
//                                    ( e.getLobby() == null && e.getDestination().equals(p.getDestination()) ) )
//                            ){
//                        skip = true;
//                        break;
//                    }
//                }
//            }
//            if(skip){
//                skip = false; // reset flag
//                continue;
//            }
//            // Distance of path (shorter better) divided by value of target (higher better)
//            // Lower tempCost correlates to better target
//            tempPath1 = SimpleAStar.CalculatePath(privateGameMap, privateMe.getLimo().getMapPosition(), p.getLobby().getBusStop());
//            tempPath2 = SimpleAStar.CalculatePath(privateGameMap, p.getLobby().getBusStop(), p.getDestination().getBusStop());
//            // If we have a passenger and they have an enemy at the potential target's location, skip that target
//            if(getMe().getLimo().getPassenger() != null){
//                boolean enemyAtTarget = false;
//                for(Passenger waiting : p.getLobby().getPassengers()){
//                    enemyAtTarget = getMe().getLimo().getPassenger().getEnemies().contains(waiting);
//                }
//                if(enemyAtTarget){
//                    continue;
//                }
//            }
//            tempCost = (tempPath1.size()*2+tempPath2.size())/p.getPointsDelivered();
//            if (currCost > tempCost){
//                currCost = tempCost;
//                target = p;
//                finalPath = tempPath1;
//                finalPath.addAll(tempPath2);
//            }
//        }
//        if(target != null && target.getLobby() != null){
//            log.info("Chose a new target: " + target.getName() + " at " + target.getLobby());
//            myPath = finalPath;
//            //System.out.println(myPath.size());
//            ptDest = target.getLobby().getBusStop();
//            pickup.add(target);
//        }
//        log.info("No path, and no viable target found.");
//    }

	private void MaybePlayPowerUp2() {
        if ((getPowerUpHand().size() != 0) && (rand.nextInt(50) < 30))
            return;
        // not enough, draw
        if (getPowerUpHand().size() < getMe().getMaxCardsInHand() && getPowerUpDeck().size() > 0)
        {
            for (int index = 0; index < getMe().getMaxCardsInHand() - getPowerUpHand().size() && getPowerUpDeck().size() > 0; index++)
            {
                // select a card
                PowerUp pu = getPowerUpDeck().get(0);
                privatePowerUpDeck.remove(pu);
                privatePowerUpHand.add(pu);
                playCards.invoke(PlayerAIBase.CARD_ACTION.DRAW, pu);
            }
            return;
        }

        // can we play one?
        PowerUp pu2 = null;
        for(PowerUp current : getPowerUpHand()) {
            if(current.isOkToPlay()) {
                pu2 = current;
                break;
            }
        }

        if (pu2 == null)
            return;
        // 10% discard, 90% play
        if (rand.nextInt(10) == 0)
            playCards.invoke(PlayerAIBase.CARD_ACTION.DISCARD, pu2);
        else
        {
            if (pu2.getCard() == PowerUp.CARD.MOVE_PASSENGER) {
                Passenger toUseCardOn = null;
                for(Passenger pass : privatePassengers) {
                    if(pass.getCar() == null) {
                        toUseCardOn = pass;
                        break;
                    }
                }
                pu2.setPassenger(toUseCardOn);
            }
            if (pu2.getCard() == PowerUp.CARD.CHANGE_DESTINATION || pu2.getCard() == PowerUp.CARD.STOP_CAR)
            {
                java.util.ArrayList<Player> plyrsWithPsngrs = new ArrayList<Player>();
                for(Player play : privatePlayers) {
                    if(play.getGuid() != getMe().getGuid() && play.getLimo().getPassenger() != null) {
                        plyrsWithPsngrs.add(play);
                    }
                }

                if (plyrsWithPsngrs.size() == 0)
                    return;
                pu2.setPlayer(plyrsWithPsngrs.get(0));
            }
            if (log.isInfoEnabled())
                log.info("Request play card " + pu2);
            playCards.invoke(PlayerAIBase.CARD_ACTION.PLAY, pu2);
        }
        privatePowerUpHand.remove(pu2);
    }

    /**
     * A power-up was played. It may be an error message, or success.
     * @param puStatus - The status of the played card.
     * @param plyrPowerUp - The player who played the card.
     * @param cardPlayed - The card played.
     */
    public void PowerupStatus(PlayerAIBase.STATUS puStatus, Player plyrPowerUp, PowerUp cardPlayed)
    {
        // redo the path if we got relocated
        if ((puStatus == PlayerAIBase.STATUS.POWER_UP_PLAYED) && ((cardPlayed.getCard() == PowerUp.CARD.RELOCATE_ALL_CARS) ||
                ((cardPlayed.getCard() == PowerUp.CARD.CHANGE_DESTINATION) && (cardPlayed.getPlayer() != null ? cardPlayed.getPlayer().getGuid() : null) == getMe().getGuid())))
            GameStatus(PlayerAIBase.STATUS.NO_PATH, getMe());
    }

    private void DisplayStatus(PlayerAIBase.STATUS status, Player plyrStatus)
    {
        String msg = null;
        switch (status)
        {
            case PASSENGER_DELIVERED:
	            if(getMyPassenger() != null && getMyPassenger().getLobby() != null)
	                msg = getMyPassenger().getName() + " delivered to " + getMyPassenger().getLobby().getName();
                privateMyPassenger = null;
                break;
            case PASSENGER_ABANDONED:
                msg = getMyPassenger().getName() + " abandoned at " + getMyPassenger().getLobby().getName();
                privateMyPassenger = null;
                break;
            case PASSENGER_REFUSED_ENEMY:
                msg = plyrStatus.getLimo().getPassenger().getName() + " refused to exit at " +
                        plyrStatus.getLimo().getPassenger().getDestination().getName() + " - enemy there";
                break;
            case PASSENGER_DELIVERED_AND_PICKED_UP:
                msg = getMyPassenger().getName() + " delivered at " + getMyPassenger().getLobby().getName() + " and " +
                        plyrStatus.getLimo().getPassenger().getName() + " picked up";
                privateMyPassenger = plyrStatus.getLimo().getPassenger();
                break;
            case PASSENGER_PICKED_UP:
                msg = plyrStatus.getLimo().getPassenger().getName() + " picked up";
                privateMyPassenger = plyrStatus.getLimo().getPassenger();
                break;
            case PASSENGER_REFUSED_NO_COFFEE:
                msg = "Passenger refused to board limo, no coffee";
                break;
            case PASSENGER_DELIVERED_AND_PICK_UP_REFUSED:
                msg = getMyPassenger().getName() + " delivered at " + getMyPassenger().getLobby().getName() +
                        ", new passenger refused to board limo, no coffee";
                break;
            case COFFEE_STORE_CAR_RESTOCKED:
                msg = "Coffee restocked!";
                break;
            case UPDATE:
	            if(getMe().getLimo().getPassenger() != null){
		            log.debug(getMe().getLimo().getPassenger().getDestination().getBusStop());
		            log.debug(getMe().getLimo().getPassenger().getName());
		            log.debug(getMe().getLimo().getPath().get(getMe().getLimo().getPath().size()-1));
	            }

	            break;
        }
        if (msg != null && !msg.equals(""))
        {
            System.out.println(msg);
            if (log.isInfoEnabled())
                log.info(msg);
        }
    }

    private void DisplayOrders(Point ptDest)
    {
        String msg = null;
        CoffeeStore store = null;
        for(CoffeeStore s : getCoffeeStores()) {
            if(s.getBusStop() == ptDest) {
                store = s;
                break;
            }
        }

        if (store != null)
            msg = "Heading toward " + store.getName() + " at " + ptDest.toString();
        else
        {
            Company company = null;
            for(Company c : getCompanies()) {
                if(c.getBusStop() == ptDest) {
                    company = c;
                    break;
                }
            }

            if (company != null)
                msg = "Heading toward " + company.getName() + " at " + ptDest.toString();
        }
        if (msg != null && !msg.equals(""))
        {
            System.out.println(msg);
            if (log.isInfoEnabled())
                log.info(msg);
        }
    }

    private java.util.ArrayList<Point> CalculatePathPlus1(Player me, Point ptDest) {
        java.util.ArrayList<Point> path = SimpleAStar.CalculatePath(getGameMap(), me.getLimo().getMapPosition(), ptDest);
        // add in leaving the bus stop so it has orders while we get the message saying it got there and are deciding what to do next.
        if (path.size() > 1) {
            path.add(path.get(path.size() - 2));
        }
        return path;
    }

    private static java.util.ArrayList<Passenger> AllPickups(Player me, Iterable<Passenger> passengers) {
        java.util.ArrayList<Passenger> pickup = new java.util.ArrayList<Passenger>();

        for (Passenger psngr : passengers) {
            if ((!me.getPassengersDelivered().contains(psngr)) && (psngr != me.getLimo().getPassenger()) && (psngr.getCar() == null) && (psngr.getLobby() != null) && (psngr.getDestination() != null))
                pickup.add(psngr);
        }

	    Collections.shuffle(pickup);

        //add sort by random so no loops for can't pickup
        return pickup;
    }
}
