package net.windward.Windwardopolis2.AI;

import net.windward.Windwardopolis2.api.Company;
import net.windward.Windwardopolis2.api.Passenger;
import net.windward.Windwardopolis2.api.Player;
import net.windward.Windwardopolis2.api.PowerUp;

import java.awt.*;

/**
 * Class for all powerups functions
 */
public class Powerups {


	static void setAndHandlePowerUp(MyPlayerBrain brain, PowerUp up, Player player, Passenger pass){
		switch(up.getCard()){
			case MOVE_PASSENGER:
				handleMovePass(pass, up);
				break;
			case CHANGE_DESTINATION:
				handleChangeDest(player, up);
				break;
			case STOP_CAR:
				handleStopCar(pickSTOP_CAR(brain), up);
		}
	}

	static boolean checkPowerUp(MyPlayerBrain brain, PowerUp up, Player player, Passenger pass){
		switch(up.getCard()){
			case MOVE_PASSENGER:
				return MOVE_PASSENGER(brain, pass);
			case CHANGE_DESTINATION:
				return CHANGE_DESTINATION(player);
			case MULT_DELIVERY_QUARTER_SPEED:
				return MULT_DELIVERY_QUARTER_SPEED(brain);
			case ALL_OTHER_CARS_QUARTER_SPEED:
				return ALL_OTHER_CARS_QUARTER_SPEED();
			case STOP_CAR:
				return STOP_CAR(brain);
			case RELOCATE_ALL_CARS:
				return RELOCATE_ALL_CARS(brain);
			case RELOCATE_ALL_PASSENGERS:
				return RELOCATE_ALL_PASSENGERS(brain, up);
			case MULT_DELIVERING_PASSENGER:
				return MULT_DELIVERING_PASSENGER(brain, up, pass);
			case MULT_DELIVER_AT_COMPANY:
				return MULT_DELIVER_AT_COMPANY(brain, up, pass);
			default:
				return false;
		}
	}

	static void handleMovePass(Passenger p, PowerUp up){
		up.setPassenger(p);
	}

	static void handleChangeDest(Player p, PowerUp up){
		up.setPlayer(p);
	}

	static void handleStopCar(Player p, PowerUp up){
		up.setPlayer(p);
	}

	// some cards double coffee consumption
	static boolean checkCoffee(MyPlayerBrain brain){
		return brain.getCoffeeStores().size() > 1;
	}

	static boolean MULT_DELIVER_AT_COMPANY(MyPlayerBrain brain, PowerUp up, Passenger pass){
		if(!checkCoffee(brain))
			return false;

		Company card = up.getCompany();

		if(card == null)
			return false;

		Company dest = pass.getDestination();

		if(!card.getName().equals(dest.getName()))
			return false;

		return true;
	}

	static boolean MULT_DELIVERING_PASSENGER(MyPlayerBrain brain, PowerUp up, Passenger pass){
		if(!checkCoffee(brain))
			return false;
		Passenger power = up.getPassenger();

		if(power == null)
			return false;

		if(!power.getName().equals(pass.getName()))
			return false;

		return true;
	}

	// bugbug check for other player using MULT_DELIVER_AT_COMPANY powerup
	static boolean RELOCATE_ALL_PASSENGERS(MyPlayerBrain brain, PowerUp up){
		if(brain.getMyPassenger() == null)
			return false;

		return true;
	}

	// bugbug add if we across map carrying somebody
	static boolean RELOCATE_ALL_CARS(MyPlayerBrain brain){
		if(brain.getMyPassenger() != null)
			return false;

		return true;
	}

	static boolean STOP_CAR(MyPlayerBrain brain){
		return true;
	}

	static Player pickSTOP_CAR(MyPlayerBrain brain){
		float highest = 0;
		Player pp = null;
		for(Player p : brain.getPlayers()){
			if(p.getScore() > highest && !p.getName().equals(brain.getName())) {
				highest = p.getScore();
				pp = p;
			}
		}
		return pp;
	}

	// bugbug check for same dest/pickup
	static boolean ALL_OTHER_CARS_QUARTER_SPEED(){
		return true;
	}

	// bugbug check for small distance
	static boolean MULT_DELIVERY_QUARTER_SPEED(MyPlayerBrain brain){
		if(!checkCoffee(brain))
			return false;
		  return false;
	}

	// bugbug check for other MULT_DELIVER_AT_COMPANY
	static boolean CHANGE_DESTINATION(Player p){
		if(p.getLimo().getPassenger() == null)
			return false;

		Point src = p.getLimo().getMapPosition();
		Point dest = p.getLimo().getPassenger().getDestination().getBusStop();

		if(src.distance(dest) < 3.0)
			return false;

		return true;
	}

	// bugbug check for someone trying to pick someone up
	static boolean MOVE_PASSENGER(MyPlayerBrain brain, Passenger p){
		if(p.getCar() != null && (brain.getNext() == null || !p.getName().equals(brain.getNext().getName())))
			return false;

		return true;
	}
}
