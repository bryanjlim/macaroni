import java.awt.Color;
import java.util.*;

import ihs.apcs.spacebattle.*;
import ihs.apcs.spacebattle.games.*;
import ihs.apcs.spacebattle.commands.*;

public class BaubleHuntChampionShip	implements Spaceship<BaubleHuntGameInfo> { // BaubleHuntChampionShip (Author of This Class : Bryan Lim)
	
	// class-level global object for logging 
	Logger log = new Logger(true);
	
	// fields
	private int	height;
	private int	width;
	private boolean goHome;	
	
	public static void main(String[] args) {
		TextClient.run("10.40.30.53", new BaubleHuntChampionShip()); // Issaquah High School
	}

	@Override
	public RegistrationData	registerShip(int numImages, int worldWidth, int	worldHeight) {
		height = worldHeight;
		width = worldWidth;
		goHome = false; 

		return new RegistrationData("Macaroni", new Color(0, 255, 127), 1);
	}
	
	@Override
	public void	shipDestroyed(String s){
		// no operation
	}
	
	@Override
	public ShipCommand getNextCommand(Environment<BaubleHuntGameInfo>	env) { //getNextCommand

		// gets game info from environment for bauble location	
		BaubleHuntGameInfo bgi = env.getGameInfo();	
		
		// ship status 
		ObjectStatus shipStatus	= env.getShipStatus(); 
		
		// ship position point 
		Point shipPosition = shipStatus.getPosition(); 

		// home base point
		Point homeBase = shipPosition.getClosestMappedPoint(bgi.getObjectiveLocation(), width, height);
		
		// sends ship home with enough capacity
		if(bgi.getNumBaublesCarried() >= 2 || bgi.getBaublesCarriedValue() >= 8) {
			goHome = true;	
		}
		else {
			goHome = false;
		}

		// radar finder
		if(env.getRadar()	!=	null){
			ArrayList<ObjectStatus>	prioritizedObjects = getPrioritized(env.getRadar(), shipPosition); 
			
			// iterates through scanned	objects, acts in accordance to type
			for(ObjectStatus o: prioritizedObjects){

				// actual object location point
				Point realObject = o.getPosition();	
					 
				// closest mapped object location point
				Point navObject = shipPosition.getClosestMappedPoint(realObject, width,	height);	
				
				// distance to object
				double objectDistance =	shipPosition.getDistanceTo(navObject);
				
				// angle to turn between	orientation	and angle to mapped point
				int turningAngle = shipPosition.getAngleTo(navObject) - shipStatus.getOrientation(); 

				// keeps turningAngle positive
				if(turningAngle < 0)
					turningAngle += SpaceObject.FullRotation;
				
				// goes home if	close enough and with baubles
				if( shipPosition.getDistanceTo(homeBase) <= 200	&&	bgi.getNumBaublesCarried() > 0) {
					goHome = true;
				}
				
				// shields from TORPEDO
				if( o.getType().equals(SpaceObject.Torpedo) ){
					if	( Math.abs(objectDistance)	< 75 ) {
						return new RaiseShieldsCommand(1);
					}
				}
				
				// gets out of BLACK HOLE
				if((o.getType().equals(SpaceObject.Blackhole) || o.getType().equals(SpaceObject.Star)) && shipStatus.getSpeed() < 7) {
					if(objectDistance < 200)
						return new ThrustCommand(SpaceObject.Back, 5, 1, false);	
				}
				
				// collects BAUBLES 
				if(o.getType().equals(SpaceObject.Bauble)) {
					int baubleID =	o.getId();

					// collects bauble if close enough
					if( (shipPosition.getDistanceTo(navObject) < 34)){
						return new CollectCommand(baubleID);
					}
					
					// goes to and collects bauble if close enough
					if( (bgi.getNumBaublesCarried() < 7	) && objectDistance <= 100	&&	!goHome){
						return navigate(shipStatus, navObject,	shipPosition, turningAngle);
					} 
				}
				
				// shoots down ASTEROID or COMET if enough energy
				if( ((o.getType().equals(SpaceObject.Asteroid) || o.getType().equals(SpaceObject.Comet)) &&	shipStatus.getEnergy() >= 35)) {
					
					
               //only shoots if close enough and threat
					if(((objectDistance < 200) && (Math.abs(turningAngle-shipStatus.getMovementDirection())<=90)) || objectDistance < 100){
						log.d("Shooting asteroid");
                  return shoot(shipStatus, navObject,	shipPosition);
					}						
				}
				
				// avoids PLANETS and STARS if within a certain distance
				if((o.getType().equals(SpaceObject.Planet) ||  o.getType().equals(SpaceObject.Asteroid) 
                	|| o.getType().equals(SpaceObject.Comet) || o.getType().equals(SpaceObject.Wormhole))
                	&& objectDistance <= 200 && (shipPosition.getDistanceTo(homeBase) > objectDistance)) {
					
					// accelerates away from cellestial body if ship is slow enough			  
					if( (shipStatus.getSpeed()	< 25) && objectDistance	< 250) {
						return accelerate(turningAngle - 180);
					}
					
					// angle between ship movement direction and planet subtracted by 180 for opposite direction
               int planetAngle = shipPosition.getAngleTo(navObject) - (int) shipStatus.getMovementDirection();
					int avoidanceAngle =	turningAngle - 180;
					
					//keeps angle between -180 and 180 for steering efficiency
					if(avoidanceAngle >	SpaceObject.HalfRotation)
						avoidanceAngle -= SpaceObject.FullRotation;
					else if(avoidanceAngle	< -SpaceObject.HalfRotation)
						avoidanceAngle += SpaceObject.FullRotation;
				   
               		//avoids ship if on collision path
               		if(Math.abs(planetAngle) < 90) {
                  	log.d("AVOIDANCE ANGLE " + avoidanceAngle);
						return avoidCollision(shipStatus, navObject, shipPosition, avoidanceAngle);	
               		}
				}
				
				//avoids SHIPS
				if(o.getType().equals(SpaceObject.Ship)) {
					// accelerates away from other ship if own ship is slow enough			 
					if(Math.abs(shipStatus.getOrientation() - shipPosition.getAngleTo(navObject)) < 90	&& shipStatus.getSpeed() < 20) {
						return accelerate(turningAngle - 180);
					}
					
					// angle between ship movement direction and planet
					int avoidanceAngle = shipPosition.getAngleTo(navObject) - (int) shipStatus.getMovementDirection();

					// raises shield as last resort
					if(Math.abs(objectDistance) < 50 ) {
						return new RaiseShieldsCommand(1);
					}
								
					// avoids object if on trajectory towards it
					if(Math.abs(avoidanceAngle) <= 90 ) {
						return avoidCollision(shipStatus, navObject, shipPosition, avoidanceAngle); 
					}

					// shoots ship if close enough, energy permitting
					if(Math.abs(objectDistance) < 300 && shipStatus.getEnergy() > 20){
						return shoot(shipStatus, navObject,	shipPosition);
					}
				}
				
				// goes to and collects BAUBLES
				if(goHome || o.getType().equals(SpaceObject.Bauble)) {
					
					// if baubles carried above amount or value, goes back to home base
					if(goHome){
						navObject =	homeBase; 
						log.d("Going home");
					}
					
					// angle to turn between	orientation	and angle to mapped home point
					int homeAngle = shipPosition.getAngleTo(navObject) - shipStatus.getOrientation(); 

					// keeps turningAngle positive
					if(homeAngle <	0) {
						homeAngle += SpaceObject.FullRotation;
					}
					return navigate(shipStatus, navObject,	shipPosition, homeAngle); 
				}
				
				// avoids DRAGONS
				if(o.getType().equals(SpaceObject.Dragon) && objectDistance < 250) {
					turningAngle =	(shipPosition.getAngleTo(navObject)	- shipStatus.getOrientation()) - SpaceObject.HalfRotation; 
					log.d(shipStatus.getEnergy()+"");
		         
					// keeps turningAngle positive
					while(turningAngle < 0)
						turningAngle += SpaceObject.FullRotation;	
			
					//shoots dragon if close enough
               if(objectDistance < 150) {
                  return shoot(shipStatus, navObject,	shipPosition);
               }
               
               // angle to steer away from dragon
					int steerAngle	= (shipPosition.getAngleTo(navObject) - SpaceObject.HalfRotation - (int)shipStatus.getMovementDirection());	
			
					// makes angle between -180	and 180 for	efficiency
					if(steerAngle > SpaceObject.HalfRotation) {
						steerAngle -= SpaceObject.FullRotation;
					}
						
					else if(steerAngle < -SpaceObject.HalfRotation){
						steerAngle += SpaceObject.FullRotation;
					}
						
					// steers away
					return new SteerCommand(steerAngle);
				}	 
				 
				 // repairs instead of idiling
				if(( shipStatus.getHealth() <	100) && shipStatus.getEnergy() >	30){
					if( !(o.getType().equals(SpaceObject.Star) && shipPosition.getDistanceTo(navObject) < 60)) {
						log.d("Repairing");
						return new RepairCommand(2);
					}	
				}		
			}
		} 
			return new RadarCommand(4);
  	} // end of getNextCommand method
  
  	// method - navigates ship (steers	and accelerates) towards a	point
  	private ShipCommand navigate(ObjectStatus shipStatus, Point bauble, Point shipPosition, int turningAngle){ //navigate
	  
		System.out.println(shipStatus.getEnergy());

		// idles ship when out of energy
		if( (shipStatus.getEnergy() < 1)) {
			return new IdleCommand(4); 
		}
					
		// idles if ship is on correct path
		if((Math.abs(shipStatus.getMovementDirection() - shipPosition.getAngleTo(bauble)) <	10) && (shipStatus.getSpeed() >= 40) ) {
			return new IdleCommand(1);
		}
      
      // steers ship towards bauble objective	
		int steerAngle = (shipPosition.getAngleTo(bauble) - (int)shipStatus.getMovementDirection()); 
				
		// makes angle between -180 and 180 for efficiency
		if(steerAngle > SpaceObject.HalfRotation)
			steerAngle -= SpaceObject.FullRotation;
		else if(steerAngle < -SpaceObject.HalfRotation)
			steerAngle += SpaceObject.FullRotation;
		
      // if within certain distance and speed, allows non-blocking steers	
		if(shipPosition.getDistanceTo(bauble) < 75 && shipStatus.getSpeed() > 10) {
			return new SteerCommand(steerAngle,false);
		}
      
		// speeds ship up in direction most aligned with intended path
		if((shipStatus.getSpeed() < 67)){
			return accelerate(turningAngle);		  
		}  
		
		// if within certain distance, allows non-blocking steers to avoid orbit	
		if(shipPosition.getDistanceTo(bauble) < 155) {
			return new SteerCommand(steerAngle,false);
		}
				
		// actual steer command
			return new SteerCommand(steerAngle);
	} // end of navigate method
  
  	// method - shoots torpedo at point 
  	private ShipCommand shoot(ObjectStatus shipStatus, Point navObject, Point shipPosition) { //shoot

		int shootingAngle = (shipPosition.getAngleTo(navObject) -	shipStatus.getOrientation());
		log.d(shipStatus.getEnergy()+"");

		// makes shootingAngle between -180 and 180 for efficiency
		while(shootingAngle > SpaceObject.HalfRotation) {
			shootingAngle -= SpaceObject.FullRotation;
		}
		while(shootingAngle < -SpaceObject.HalfRotation){
			shootingAngle += SpaceObject.FullRotation;
		}

		// shoots when angle between ship and target is small enough
		if((Math.abs(shootingAngle) > 5) && Math.abs(shootingAngle) <= 90) { 
			return new RotateCommand(shootingAngle); 
		}
      //rotates to shoot with back shooters
      else if((Math.abs(shootingAngle) > 90)) {
         if(shootingAngle<0) {
            return new RotateCommand(shootingAngle+90);
         }
         else {
            return new RotateCommand(shootingAngle-90);
         }
      }
      
      //shoots with front torpedos
      if(Math.abs(shootingAngle) <= 5) {
         return new FireTorpedoCommand(SpaceObject.Front); 
      }
      //shoots with back torpedos
      else{
         return new FireTorpedoCommand(SpaceObject.Back);
      }
		
  	} // end of shoot method
  
    // method - avoidCollision
	private ShipCommand avoidCollision(ObjectStatus shipStatus, Point navObject, Point shipPosition, int avoidanceAngle) { // avoidCollision
	     // result
		SteerCommand result = (avoidanceAngle <= 90) ?
			 new SteerCommand(-(90-avoidanceAngle),false):
			 new SteerCommand(-(-90-avoidanceAngle),false);	

		return result;
    } // end of avoidCollision method
  
	// method - accelerates ship in	most clostly aligned direction 
	private ShipCommand accelerate(int turningAngle) { //accelerate
		// result
		ThrustCommand result = null;
      
		if( (turningAngle >= 315) || (turningAngle <= 45) ) {
			result = new ThrustCommand(SpaceObject.Back, .3,1, false);	
		}
		else if((turningAngle >= 45) &&	(turningAngle < 135)) {
			result = new ThrustCommand(SpaceObject.Right, .3,1, false);
		}
		else if((turningAngle >= 135) && (turningAngle	< 215)) {
			result = new ThrustCommand(SpaceObject.Front, .3,1, false);
		}
		else if((turningAngle >= 215) && (turningAngle	< 315)) {
			result = new ThrustCommand(SpaceObject.Left, .3,1, false);
		} 
		else {
			result = new ThrustCommand(SpaceObject.Back, 1, 1); 
		}
		return result;
	} // end of accelerate method
	
	// sorts scanned objects by priority
	private ArrayList<ObjectStatus> getPrioritized(ArrayList<ObjectStatus> input, Point shipPosition) { // getPrioritized
		// working object 
		SpaceObject spaceObject = new SpaceObject();
		// result
		ArrayList<ObjectStatus> results = new ArrayList<ObjectStatus>(); 

		// temp data structure to order stuff.
		ArrayList<ObjectStatus> distanceSubset = new ArrayList<ObjectStatus>(); 
		ArrayList<ObjectStatus> torpedos = new ArrayList<ObjectStatus>();
      	ArrayList<ObjectStatus> planets = new ArrayList<ObjectStatus>();
      	ArrayList<ObjectStatus> asteroids = new ArrayList<ObjectStatus>();
		ArrayList<ObjectStatus> baubles = new ArrayList<ObjectStatus>();

		for(ObjectStatus o: input) {
			// highest priority - torpedo
			if(spaceObject.IsObject(o, SpaceObject.Torpedo)) {
				torpedos.add(o);
				continue;
			}
			// priority 2 
			if(spaceObject.IsObject(o, SpaceObject.Planet)) {
				planets.add(o); 
				continue;
			}
			// priority 3 
			if(spaceObject.IsObject(o, SpaceObject.Asteroid) || 
				spaceObject.IsObject(o, SpaceObject.Comet)) {
				asteroids.add(o);
				continue;
			}
			// priority 4 
			if(spaceObject.IsObject(o, SpaceObject.Dragon)) {
				distanceSubset.add(o);
				continue;
			}	
			// priority 4 	
			if(spaceObject.IsObject(o, SpaceObject.Star) || 
				spaceObject.IsObject(o, SpaceObject.Wormhole)) {
				distanceSubset.add(o);
				continue;
			}
			// priority 4 
			if(spaceObject.IsObject(o, SpaceObject.Blackhole)) {
				distanceSubset.add(o);
				continue;
			}
			// priority 4 
			if(spaceObject.IsObject(o, SpaceObject.Ship)) {
				distanceSubset.add(o);
				continue;
			}
			// priority 4 
			if(spaceObject.IsObject(o, SpaceObject.Bauble)) {
				distanceSubset.add(o);
				continue;
			}

	    }

		// sorts distanceSubset (planets, asteroids, dragons, stars, and ships) by distance
		spaceObject.SortObject(shipPosition, distanceSubset);

		// sorts baubles by distance
		spaceObject.SortObject(shipPosition, baubles);

		// adds to results in prioritized order
		results.addAll(torpedos);
      	results.addAll(planets);
      	results.addAll(asteroids);
		results.addAll(distanceSubset);
		results.addAll(baubles);

		return results;
	} // end of getPrioritized

	// inner class for logging ship status
    private class Logger {
		private boolean isEnabled = false;
		
		// constructor to enable or disable logging
		Logger(boolean flag){
			setEnabled(flag);
		}

		void setEnabled(boolean flag){
			isEnabled = flag; 
		}

		void d(String msg) {
			if(isEnabled){
				System.out.println(msg);
			}
		}
	}
	
	 // inner class describing space objects and helper methods.
	 private class SpaceObject	{ // SpaceObject

	 	private static final int FullRotation = 360;
		private static final int HalfRotation = 180;

		private static final char Front = 'F';
		private static final char Back = 'B';
		private static final char Left = 'L';
		private static final char Right = 'R';

		private static final String Torpedo = "Torpedo";
		private static final String Blackhole = "Blackhole";
		private static final String Star = "Star";
		private static final String Bauble = "Bauble";
		private static final String Asteroid = "Asteroid";
		private static final String Comet = "Comet";
		private static final String Planet = "Planet";
		private static final String Wormhole = "Wormhole";
		private static final String Ship = "Ship";
		private static final String Dragon = "Dragon";

        boolean IsObject(ObjectStatus o, String spaceObject) {
		    return o.getType().equals(spaceObject);
	    }

	    void SortObject(final Point shipPosition, ArrayList list) {
		    Collections.sort(list, new Comparator<ObjectStatus>() {
				@Override
				public int compare(ObjectStatus one, ObjectStatus two){
					 if( shipPosition.getDistanceTo(one.getPosition())	> shipPosition.getDistanceTo(two.getPosition())) {
						  return 1;
					 }
					 return -1;
				}
		    });
	    }
    } // end of SpaceObject

} // end of BaubleHuntChampionShip
  