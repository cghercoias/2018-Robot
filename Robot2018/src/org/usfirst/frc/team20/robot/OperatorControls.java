package org.usfirst.frc.team20.robot;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;

public class OperatorControls {
	Collector collector;
	Elevator elevator;
	Zenith ob;
	boolean override, flipOver, timeStarted, resetting, intakeMode, maxHeight, downTimerStarted, flipPositionSet, flipped, positionChange;
	double startTime;
	int toRun;
	private final double FLIP_WAIT_TIME = 0.75;
	
	public OperatorControls(Collector c, Elevator e, Zenith o){
		collector = c;
		elevator = e;
		ob = o;
		override = flipOver = timeStarted = resetting = intakeMode = maxHeight = downTimerStarted = flipPositionSet = flipped = positionChange = false;
		startTime = 0;
		toRun = 0;
	}
	
	/**
	 * runs all of the controls of the operator
	 */
	public void operatorControlsPS4(){
		//PRINT LINES
//		System.out.println("Elevator Position: " + ob.elevatorMaster.getSelectedSensorPosition(0)); 
//		System.out.println("Elevator Setpoint: " + elevator.getSetPosition());
//		System.out.println("Elevator Master: " + ob.elevatorMaster.getMotorOutputVoltage()); 
//		System.out.println("Elevator Follower: " + ob.elevatorFollower.getMotorOutputVoltage()); 
		System.out.println("Elevator Current Draw: " + ob.elevatorMaster.getOutputCurrent());
		//CONTROLLER
		if(ob.operatorJoy4.getXButton()){
			collector.intake(0.5);
			collector.open();
			intakeMode = true;
			ob.operatorJoy4.setRumble(1.0, 1.0);
		}
		if(ob.operatorJoy4.getCircleButton()){
			collector.stopRollers();
			ob.operatorJoy4.setRumble(0.0, 0.0);
			intakeMode = false;
		}
		if(ob.operatorJoy4.getTriButton()){
			collector.outtake();
			ob.operatorJoy4.setRumble(100, 100);
		}
		if(ob.operatorJoy4.getButtonDUp()){
			positionChange = true;
			elevator.setScaleHigh();
			collector.arm45();
		}
		if(ob.operatorJoy4.getOptionsButton() && ob.operatorJoy4.getShareButton()) {
			resetting = true;
		}
		if(ob.operatorJoy4.getButtonDLeft()){
			positionChange = true;
			elevator.setScaleMid();
		}
		if(ob.operatorJoy4.getButtonDDown()) {
			positionChange = true;
			elevator.setScaleLow();
		}
		if(ob.operatorJoy4.getButtonDRight()) {
			toRun = 0;
			if(elevator.aboveThreshold()){
				collector.armIntakePosition();
				startTime = Timer.getFPGATimestamp();
				downTimerStarted = true;
			} else {
 				runFunction();
			}
		}
		if(ob.operatorJoy4.getRightYAxis() < -0.5) {
			collector.arm45();
		}		
		if(ob.operatorJoy4.getRightYAxis() > 0.5){
			collector.armIntakePosition();
		}
		if(ob.operatorJoy4.getLeftBumperButton()) {
			toRun = 2;
			if(flipped){
				collector.armIntakePosition();
				startTime = Timer.getFPGATimestamp();
				downTimerStarted = true;
			}
			if(!downTimerStarted){
				runFunction();
			}
		}
		if(Math.abs(ob.operatorJoy4.getLeftYAxis()) > 0.1 && ob.operatorJoy4.getTrackpadButton()){
			if(!maxHeight){
				double speed = 0;
				if(ob.operatorJoy4.getLeftYAxis() < -0.1){
					speed = 1.11*(ob.operatorJoy4.getLeftYAxis() + 0.1);
				} else {
					speed = 1.11*(ob.operatorJoy4.getLeftYAxis() - 0.1);					
				}
				elevator.moveSpeed(speed/2);
			} else {
				System.out.println("AT MAX HEIGHT");
				elevator.moveSpeed(-0.06);
			}
			override = true;
		} else {
			if(override){
				elevator.stop();
				elevator.moveSpeed(0.0);
				override = false;
			}
		}
		if(ob.operatorJoy4.getLeftYAxis() < -0.5 && !ob.operatorJoy4.getLeftStickButton() && !override){
			elevator.upIncrement();
		}
		if(ob.operatorJoy4.getLeftYAxis() > 0.5 && !ob.operatorJoy4.getLeftStickButton() && !override){
			elevator.downIncrement();
		}
		if(ob.operatorJoy4.getLeftTriggerAxis() > 0.1){
			if(!flipPositionSet){
				elevator.flipPosition();	
				flipPositionSet = true;
			}
			collector.arm45();
			flipOver = true;
		}
		if(ob.operatorJoy4.getLeftTriggerAxis() == 0){
			flipPositionSet = false;
		}
		if(ob.operatorJoy4.getRightBumperButton()){
			collector.open();
		}
		if(ob.operatorJoy4.getRightTriggerAxis() > 0.1){
			collector.close();
		}
		if(ob.operatorJoy4.getSquareButton()){
			toRun = 1;
			if(flipped){
				collector.arm100();
				startTime = Timer.getFPGATimestamp();
				downTimerStarted = true;
			} else {
				runFunction();
			}
		}
		if(ob.operatorJoy4.getOptionsButton()){
			elevator.stop();
			collector.stopRollers();
		}
		//SAFETY INFORMATION
		if(ob.elevatorMaster.getSelectedSensorPosition(0) < Constants.ELEVATOR_MAX_POSITION){
			maxHeight = true;
		} else {
			maxHeight = false;
		}
		if(ob.doubleUp.get() == Value.kForward && ob.singleUp.get() == Value.kForward){
			flipped = true;
		} else {
			flipped = false;
		}
		//BOOLEANS
		if(intakeMode){ //don't go down unless we do not have a cube
			if(!ob.cubeSensor.get()){
				if(!timeStarted){
					startTime = Timer.getFPGATimestamp();
					collector.intake(1.0);
					timeStarted = true;
				}
				collector.close();
				ob.updateCube(true);
			} else {
				ob.updateCube(false);
			}
			if((Timer.getFPGATimestamp() - startTime) > 0.5 && timeStarted){ //time to wait until closes
				collector.intake(0.0);
				intakeMode = false;
				timeStarted = false;
			}
		}
		if(downTimerStarted){
			if(flipWaitTime()){
				runFunction();
				downTimerStarted = false;
			}
		}
		if(resetting){
			if(elevator.reset()){
				resetting = false;
			}
		}
		if(flipOver && elevator.aboveThreshold()){
			collector.arm180();
			flipOver = false;
		}
		if(flipped && positionChange){
			elevator.flipPosition();
			positionChange = false;
		}
	}	
	/**
	 * @return true if the elevator has waiting long enough to flip the manipulator
	 */
	private boolean flipWaitTime(){
		if(Math.abs(startTime - Timer.getFPGATimestamp()) > FLIP_WAIT_TIME){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * runs the function designated by the integer toRun
	 * toRun = 0; sets the elevator to switch height
	 * toRun = 1; sets the elevator to intake position with the arm at the 100 degree position
	 * toRun = 2; sets the elevator and arm to intake position and into intake mode
	 */
	private void runFunction(){
		switch(toRun){
		case 0: 
			elevator.setSwitch();
			break;
		case 1:
			elevator.setIntake();
			break;
		case 2: 
			elevator.setIntake();
			collector.armIntakePosition();
			collector.intake(0.5);
			intakeMode = true;
			break;
		}
	}
}