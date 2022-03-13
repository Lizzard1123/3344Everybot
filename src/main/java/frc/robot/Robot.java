/*
  2022 everybot code
  written by carson graf 
  don't email me, @ me on discord
*/
// team number is 4842
/*
  This is catastrophically poorly written code for the sake of being easy to follow
  If you know what the word "refactor" means, you should refactor this code
*/

package frc.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.VictorSPXControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;

public class Robot extends TimedRobot {
  
  //Definitions for the hardware. Change this if you change what stuff you have plugged in
  CANSparkMax driveLeftFront = new CANSparkMax(1, MotorType.kBrushless);
  CANSparkMax driveLeftBack = new CANSparkMax(2, MotorType.kBrushless);
  CANSparkMax driveRightFront = new CANSparkMax(3, MotorType.kBrushless);
  CANSparkMax driveRightBack = new CANSparkMax(4, MotorType.kBrushless);
  /*
    spark max controlled and updated through the REV hardware client
    Victor SPX controlled and updated through the PHONEIX tuner 
  */
  CANSparkMax arm = new CANSparkMax(5, MotorType.kBrushless); // this motor controller has not been flashed
  VictorSPX intake = new VictorSPX(6); // neither has this one

  Joystick driverController = new Joystick(0);

  //Constants for controlling the arm. consider tuning these for your particular robot
  final double armHoldUp = 0.08;
  final double armHoldDown = 0.13;
  final double armTravel = 0.5;

  final double armTimeUp = 0.5;
  final double armTimeDown = 0.35;

  //Varibles needed for the code
  boolean armUp = true; //Arm initialized to up because that's how it would start a match
  boolean burstMode = false;
  double lastBurstTime = 0;

  double autoStart = 0;
  boolean goForAuto = false;

  //shuffleboard
  ShuffleboardTab driveTab = Shuffleboard.getTab("Driver");
  NetworkTableEntry autonEntry;
  NetworkTableEntry driveLeftFrontReversed;
  NetworkTableEntry driveLeftBackReversed;
  NetworkTableEntry driveRightFrontReversed;
  NetworkTableEntry driveRightBackReversed;

  NetworkTableEntry leftSpeed;
  NetworkTableEntry rightSpeed;



  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    //Configure motors to turn correct direction. You may have to invert some of your motors
    
    arm.setInverted(false);
    arm.setIdleMode(IdleMode.kBrake);
    arm.burnFlash();

    //add a thing on the dashboard to turn off auto if needed
    autonEntry = driveTab.add("Auton?", false)
    .withPosition(0, 0)
    .withSize(1, 1)
    .withWidget(BuiltInWidgets.kToggleButton)
    .getEntry();
    goForAuto = autonEntry.getBoolean(false); // set local variable

    driveLeftFrontReversed = driveTab.add("driveLeftFrontReversed", false)
    .withPosition(1, 0)
    .withSize(2, 1)
    .withWidget(BuiltInWidgets.kToggleButton)
    .getEntry();

    driveLeftBackReversed = driveTab.add("driveLeftBackReversed", false)
    .withPosition(1, 1)
    .withSize(2, 1)
    .withWidget(BuiltInWidgets.kToggleButton)
    .getEntry();

    driveRightFrontReversed = driveTab.add("driveRightFrontReversed", false)
    .withPosition(3, 0)
    .withSize(2, 1)
    .withWidget(BuiltInWidgets.kToggleButton)
    .getEntry();

    driveRightBackReversed = driveTab.add("driveRightBackReversed", false)
    .withPosition(3, 1)
    .withSize(2, 1)
    .withWidget(BuiltInWidgets.kToggleButton)
    .getEntry();

    leftSpeed = driveTab.add("LeftSpeed", 0)
    .withPosition(5, 0)
    .withSize(1, 1)
    .getEntry();

    rightSpeed = driveTab.add("RightSpeed", 0)
    .withPosition(5, 1)
    .withSize(1, 1)
    .getEntry();
    
  }

  @Override
  public void autonomousInit() {
    //get a time for auton start to do events based on time later
    autoStart = Timer.getFPGATimestamp();
    //check dashboard icon to ensure good to do auto
    goForAuto = autonEntry.getBoolean(false);
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {
    //arm control code. same as in teleop
    if(armUp){
      if(Timer.getFPGATimestamp() - lastBurstTime < armTimeUp){
        arm.set(armTravel);
      }
      else{
        arm.set(armHoldUp);
      }
    }
    else{
      if(Timer.getFPGATimestamp() - lastBurstTime < armTimeDown){
        arm.set(-armTravel);
      }
      else{
        arm.set(-armHoldUp);
      }
    }
    
    //get time since start of auto
    double autoTimeElapsed = Timer.getFPGATimestamp() - autoStart;
    if(goForAuto){
      //series of timed events making up the flow of auto
      if(autoTimeElapsed < 3){
        //spit out the ball for three seconds
        intake.set(ControlMode.PercentOutput, -1);
      }else if(autoTimeElapsed < 6){
        //stop spitting out the ball and drive backwards *slowly* for three seconds
        intake.set(ControlMode.PercentOutput, 0);
        driveLeftFront.set(-0.3);
        driveLeftBack.set(-0.3);
        driveRightFront.set(-0.3);
        driveRightBack.set(-0.3);
      }else{
        //do nothing for the rest of auto
        intake.set(ControlMode.PercentOutput, 0);
        driveLeftFront.set(0);
        driveLeftBack.set(0);
        driveRightFront.set(0);
        driveRightBack.set(0);
      }
    }
  }

  /** This function is called once when teleop is enabled. */
  @Override
  public void teleopInit() {}

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {
    //invert drivetrain from shuffleboard
    driveLeftFront.setInverted(driveLeftFrontReversed.getBoolean(false));
    driveLeftBack.setInverted(driveLeftBackReversed.getBoolean(false));
    driveRightFront.setInverted(driveRightFrontReversed.getBoolean(false));
    driveRightBack.setInverted(driveRightBackReversed.getBoolean(false));
    
    //Set up tank steer
    double leftForward = -driverController.getRawAxis(2);
    double rightForward = -driverController.getRawAxis(5);

    leftSpeed.setDouble(leftForward);
    rightSpeed.setDouble(rightForward);

    driveLeftFront.set(leftForward);
    driveLeftBack.set(leftForward);
    driveRightFront.set(rightForward);
    driveRightBack.set(rightForward);

    //Intake controls
    if(driverController.getRawButton(5)){
      intake.set(VictorSPXControlMode.PercentOutput, 1);;
    }
    else if(driverController.getRawButton(7)){
      intake.set(VictorSPXControlMode.PercentOutput, -1);
    }
    else{
      intake.set(VictorSPXControlMode.PercentOutput, 0);
    }

    //Arm Controls
    if(armUp){
      if(Timer.getFPGATimestamp() - lastBurstTime < armTimeUp){
        arm.set(armTravel);
      }
      else{
        arm.set(armHoldUp);
      }
    }
    else{
      if(Timer.getFPGATimestamp() - lastBurstTime < armTimeDown){
        arm.set(-armTravel);
      }
      else{
        arm.set(-armHoldDown);
      }
    }
  
    if(driverController.getRawButtonPressed(6) && !armUp){
      lastBurstTime = Timer.getFPGATimestamp();
      armUp = true;
    }
    else if(driverController.getRawButtonPressed(8) && armUp){
      lastBurstTime = Timer.getFPGATimestamp();
      armUp = false;
    }  

  }

  @Override
  public void disabledInit() {
    //On disable turn off everything
    //done to solve issue with motors "remembering" previous setpoints after reenable
    driveLeftFront.set(0);
    driveLeftBack.set(0);
    driveRightFront.set(0);
    driveRightBack.set(0);
    arm.set(0);
    intake.set(ControlMode.PercentOutput, 0);
  }
    
}