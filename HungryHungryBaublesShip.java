import java.awt.Color;
import java.util.*;

import ihs.apcs.spacebattle.*;
import ihs.apcs.spacebattle.commands.*;

public class HungryHungryBaublesShip extends BasicSpaceship {
   
   //Fields
   private int height;
   private int width;
   private int backupCounter; 
   private boolean backupInitialized; 

   public static void main(String[] args) {
      TextClient.run("169.254.123.0", new HungryHungryBaublesShip());      // Issaquah High School
   }

   @Override
   public RegistrationData registerShip(int numImages, int worldWidth, int worldHeight) {
      
      height = worldHeight;
      width= worldWidth;
      backupCounter = 0;
      backupInitialized = false; 
      
      return new RegistrationData("Macaroni+ (Bryan)", new Color(0, 255, 127), 1);
    
   }

   @Override
   public ShipCommand getNextCommand(BasicEnvironment env) {
      
      //gets game info from environment for bauble location 
      BasicGameInfo bgi = env.getGameInfo(); 
      
      //ship status 
      ObjectStatus shipStatus = env.getShipStatus(); 
      
      //ship position point 
      Point shipPosition = shipStatus.getPosition(); 
      
      //actual bauble location point
      Point realBauble = bgi.getObjectiveLocation(); 
      
      //closest mapped bauble location point
      Point bauble = shipPosition.getClosestMappedPoint(realBauble, width, height); 
      
      //angle to turn between orientation and angle to mapped bauble
      int turningAngle = shipPosition.getAngleTo(bauble) - shipStatus.getOrientation(); 
    
      //idles ship when out of energy
      if( (shipStatus.getEnergy() < 1) && !backupInitialized){
         return new IdleCommand(4); 
      }
      
      //keeps turningAngle positive
      if(turningAngle < 0)
         turningAngle += 360; 
      
      //idles if ship is on correct path
      if( (Math.abs(shipStatus.getMovementDirection() - shipPosition.getAngleTo(bauble)) < 10) && (shipStatus.getSpeed() >= 40) && !backupInitialized)
          return new IdleCommand(1);
      
      
      //speeds ship up in direction most aligned with intended path
      if( (shipStatus.getSpeed() < 67) && (shipStatus.getEnergy() >= 10) && !backupInitialized){
            return accelerate(turningAngle);       
      }
         
      //backup in-case ship is low on energy and slow: brakes, rotates to bauble, and accelerates
         if( (shipStatus.getSpeed() < 25) && (shipStatus.getEnergy() < 25) ){ 
            
            System.out.println("EMERGENCY: BACKUP INITIALIZED");
            
            backupInitialized = true; 
            if(backupCounter == 0){
               backupCounter++; 
               return new BrakeCommand(0);         
            }
            else if(backupCounter == 1){
               backupCounter++;
               return new RotateCommand(turningAngle);          
            }
            else{
               backupCounter = 0; 
               backupInitialized = false; 
               return new ThrustCommand('B',10,1); 
            }
            
         
         }     
         
      //steers ship towards bauble objective
         
         int steerAngle = (shipPosition.getAngleTo(bauble) - (int)shipStatus.getMovementDirection()); 
         
         //makes angle between -180 and 180 for efficiency
         if(steerAngle > 180)
            steerAngle -= 360;
         else if(steerAngle < -180)
            steerAngle += 360;
         
         //if within certain distance, allows non-blocking steers to avoid orbit
         if(shipPosition.getDistanceTo(bauble) < 131.8){
            return new SteerCommand(steerAngle,false);
         }
         
         //actual steer command
         return new SteerCommand(steerAngle);
  } 
  
  
  public ShipCommand accelerate(int turningAngle){
         
         if( (turningAngle >= 315) || (turningAngle <= 45) ){
            return new ThrustCommand('B',.1,1,false); 
         }
         else if( (turningAngle >= 45) && (turningAngle < 135) ){
            return new ThrustCommand('R',.1,1,false);
         }
         else if( (turningAngle >= 135) && (turningAngle < 215) ){
            return new ThrustCommand('F',.1,1,false);
         }
         else if( (turningAngle >= 215) && (turningAngle < 315) ){
            return new ThrustCommand('L',.1,1,false);
         }

         return new IdleCommand(0);
     }
  }
  