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

import java.util.Map;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.VictorSPXControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.math.MathUtil;
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
  CANSparkMax arm = new CANSparkMax(5, MotorType.kBrushless);
  VictorSPX intake = new VictorSPX(6); // neither has this one

  Joystick driverController = new Joystick(0);

  //Constants for controlling the arm. consider tuning these for your particular robot
  final double armHoldUp = 0.08;
  final double armHoldDown = 0.13;
  final double armTravel = 0.45;

  final double armTimeUp = 0.45;
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

  NetworkTableEntry leftSpeedEntry;
  NetworkTableEntry rightSpeedEntry;
  NetworkTableEntry maxTurnSpeed;
  NetworkTableEntry maxDriveSpeed;

  UsbCamera camera = CameraServer.startAutomaticCapture();
  NetworkTableEntry cameraFeed;



  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    //invert drivetrain from shuffleboard
    driveLeftFront.setInverted(false);
    driveLeftBack.setInverted(false);
    driveRightFront.setInverted(true);
    driveRightBack.setInverted(true);

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

    leftSpeedEntry = driveTab.add("leftSpeedEntry", 0)
    .withPosition(0, 1)
    .withSize(1, 1)
    .getEntry();

    rightSpeedEntry = driveTab.add("rightSpeedEntry", 0)
    .withPosition(0, 2)
    .withSize(1, 1)
    .getEntry();

    maxTurnSpeed = driveTab.add("maxTurnSpeed", .5)
    .withPosition(1, 1)
    .withSize(2, 1)
    .withWidget(BuiltInWidgets.kNumberSlider)
    .withProperties(Map.of("Max", 1, "Min", 0))
    .getEntry();

    maxDriveSpeed = driveTab.add("maxDriveSpeed", .5)
    .withPosition(3, 1)
    .withSize(2, 1)
    .withWidget(BuiltInWidgets.kNumberSlider)
    .withProperties(Map.of("Max", 1, "Min", 0))
    .getEntry();

    driveTab.add(camera)
    .withPosition(5, 0)
    .withSize(4, 4);
    
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
    
    //Set up arcade steer
    double rightX = MathUtil.applyDeadband(driverController.getRawAxis(4), .1) * maxTurnSpeed.getDouble(0);
    double rightY = -MathUtil.applyDeadband(driverController.getRawAxis(5), .1) * maxDriveSpeed.getDouble(0);
    double leftSpeed = rightX + rightY;
    double rightSpeed = -rightX + rightY;

    if(Math.abs(leftSpeed) > 1.0){
      rightSpeed /= leftSpeed  * ((rightSpeed < 0)?-1:1);
      leftSpeed = 1.0  * ((rightSpeed < 0)?-1:1);
    }

    if(Math.abs(rightSpeed) > 1.0){
      leftSpeed /= rightSpeed * ((rightSpeed < 0)?-1:1);
      rightSpeed = 1.0  * ((rightSpeed < 0)?-1:1);
    }

    leftSpeedEntry.setDouble(leftSpeed);
    rightSpeedEntry.setDouble(rightSpeed);

    driveLeftFront.set(leftSpeed);
    driveLeftBack.set(leftSpeed);
    driveRightFront.set(rightSpeed);
    driveRightBack.set(rightSpeed);

    //Intake controls
    if(driverController.getRawButton(6)){
      intake.set(VictorSPXControlMode.PercentOutput, 1);;
    }
    else if(driverController.getRawButton(5)){
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
  
    if(driverController.getRawButtonPressed(4) && !armUp){
      lastBurstTime = Timer.getFPGATimestamp();
      armUp = true;
    }
    else if(driverController.getRawButtonPressed(1) && armUp){
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