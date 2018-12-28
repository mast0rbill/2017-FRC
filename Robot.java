package org.usfirst.frc.team5428.robot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.GenericHID.Hand;

/*
 * Author: William Liu
 * Date: 2017
 */

/* CONTROLS
   joysticks = drive
   X = climb
   LB = gear down
   RB = gear up
   LT = decel
   RT = accel
 */

public class Robot extends SampleRobot {
	private final int UPDATE_HZ = 60;
	private final boolean TWO_DRIVER = true;
	private final int WHEEL_LEFT_FRONT = 0;
	private final int WHEEL_RIGHT_FRONT = 1;
	private final int WHEEL_LEFT_REAR = 2;
	private final int WHEEL_RIGHT_REAR = 3;
	private final int GEAR_IO = 4;
	private final int CLIMBER = 5;
	private final double DEAD_ZONE = 0.25;
	private final double NORMAL_SPEED = 0.45;
	private final double BOOST_SPEED = 1.0;
	private final double SLOWER_SPEED = 0.3;
	private final double TURN_MODIFIER = 0.7;
	private final double GEAR_IO_SPEED = 0.3;
	private final double CLIMBER_SPEED = 1.0;
	private int AUTON_MODE = 1; //0 = recorded, 1 = go forward
	private final String AUTON_SIDE = "center";
	
	private long lastUpdateTime = 0L;
	private long startTime = 0L;
	
	private XboxController controller = new XboxController(0);
	private XboxController controller2 = new XboxController(1);
	
	private VictorSP wheelLeftFront = new VictorSP(WHEEL_LEFT_FRONT);
	private VictorSP wheelRightFront = new VictorSP(WHEEL_RIGHT_FRONT);
	private VictorSP wheelLeftRear = new VictorSP(WHEEL_LEFT_REAR);
	private VictorSP wheelRightRear = new VictorSP(WHEEL_RIGHT_REAR);
	private VictorSP gearIO = new VictorSP(GEAR_IO);
	private VictorSP climber = new VictorSP(CLIMBER);
	
	//inputs
	private double leftStickY = 0.0, rightStickX = 0.0;
	private double leftTrigger = 0.0, rightTrigger = 0.0;
	private boolean leftB = false, rightB = false;
	private boolean yButton = false, bButton = false, aButton = false, xButton = false, startButton = false;
	
	//motor speeds
	private double wheelLeftSpeed = 0.0, wheelRightSpeed = 0.0;
	private double gearIOSpeed = 0.0;
	private double climberSpeed = 0.0;
	
	private boolean autonWritten = false;
	
	private int curFrame = 0;
	private ArrayList<String> inputArr = new ArrayList<String>();
	
	public Robot() {
	}

	@Override
	public void robotInit() {
		CameraServer.getInstance().startAutomaticCapture("USB Camera 1", 0);
	}

	@Override
	public void autonomous() {
		curFrame = 0;
		startTime = System.currentTimeMillis();
		double curSpeed = -1.0;
		
		if(AUTON_MODE == 0) {
			try {
				Scanner reader = new Scanner(new File("/home/lvuser/input_" + AUTON_SIDE + ".5428robotics"));
				
				while(reader.hasNextLine()) {					
					inputArr.add(reader.nextLine());
				}
			
				reader.close();
			} catch (FileNotFoundException e) {
				System.out.println("No input file detected!");
			}
		}
		
		while(isAutonomous() && isEnabled()) {
			if(System.currentTimeMillis() - lastUpdateTime >= (1000 / UPDATE_HZ)) {
				if(AUTON_MODE == 0) {
					HandleAutonInput();			
					curFrame++;
				} else if(AUTON_MODE == 1) {
					if(System.currentTimeMillis() - startTime <= 200) {
						leftStickY = curSpeed;
						
						if(curSpeed > -1.0) {
							curSpeed -= 0.1;
						}
					} else if(System.currentTimeMillis() - startTime <= 5000) {
						leftStickY = curSpeed;
						
						if(curSpeed <= -0.35){
							curSpeed += 0.0065;
						}
					} else {
						leftStickY = 0.0;
					}
					
					rightStickX = 0.0;
					leftTrigger = 0.0;
					rightTrigger = 0.0;
					yButton = false;
					bButton = false;
					aButton = false;
					xButton = false;
					leftB = false;
					rightB = false;
					startButton = false;
				}
				
				CalculateMotorSpeeds();
				ApplyMotorSpeeds();
				
				lastUpdateTime = System.currentTimeMillis();
			}
		}
		
		curSpeed = -1.0;
		inputArr.clear();
	}

	private void RecordAutonInput() {
		String curStr = "";
		curStr += (Math.round(leftStickY * 100.0) / 100.0) + " ";
		curStr += (Math.round(rightStickX * 100.0) / 100.0) + " ";
		curStr += (Math.round(leftTrigger * 100.0) / 100.0) + " ";
		curStr += (Math.round(rightTrigger * 100.0) / 100.0) + " ";
		curStr += yButton + " ";
		curStr += bButton + " ";
		curStr += aButton + " ";
		curStr += xButton + " ";
		curStr += leftB + " ";
		curStr += rightB + " ";
		curStr += startButton;
		inputArr.add(curStr);
	}
	
	private void WriteInputToFile() {
		try {
			PrintWriter writer = new PrintWriter(new File(("/home/lvuser/input_" + AUTON_SIDE + ".5428robotics")));
			
			for(String s : inputArr) {
				writer.println(s);
			}
			
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void HandleAutonInput() {
		if(curFrame < inputArr.size()) {
			String curStr = inputArr.get(curFrame);
			String[] split = curStr.split(" ");
			leftStickY = Double.parseDouble(split[0]);
			rightStickX = Double.parseDouble(split[1]);
			leftTrigger = Double.parseDouble(split[2]);
			rightTrigger = Double.parseDouble(split[3]);
			yButton = Boolean.parseBoolean(split[4]);
			bButton = Boolean.parseBoolean(split[5]);
			aButton = Boolean.parseBoolean(split[6]);
			xButton = Boolean.parseBoolean(split[7]);
			leftB = Boolean.parseBoolean(split[8]);
			rightB = Boolean.parseBoolean(split[9]);
			startButton = Boolean.parseBoolean(split[10]);
		} else {
			leftStickY = 0.0;
			rightStickX = 0.0;
			leftTrigger = 0.0;
			rightTrigger = 0.0;
			yButton = false;
			bButton = false;
			aButton = false;
			xButton = false;
			leftB = false;
			rightB = false;
			startButton = false;
		}
	}

	@Override
	public void operatorControl() {	
		startTime = System.currentTimeMillis();
		
		boolean needToRecord = !(new File("/home/lvuser/input_" + AUTON_SIDE + ".5428robotics")).exists();
		
		while (isOperatorControl() && isEnabled()) {		
			if(System.currentTimeMillis() - lastUpdateTime >= (1000 / UPDATE_HZ)) {				
				HandleInput();
				CalculateMotorSpeeds();
				ApplyMotorSpeeds();
				
				if(needToRecord) {
					if(System.currentTimeMillis() - startTime < 15000) {
						RecordAutonInput();
					} else {
						if(!autonWritten) {
							System.out.println("saving auton " + AUTON_SIDE);
							WriteInputToFile();
							autonWritten = true;
						}
					}
				}
				
				lastUpdateTime = System.currentTimeMillis();
			}
		}
		
		autonWritten = false;
	}
	
	@Override
	public void test() {
		autonWritten = false;
		startTime = System.currentTimeMillis();
		
		boolean needToRecord = !(new File("/home/lvuser/input_" + AUTON_SIDE + ".5428robotics")).exists();
		
		while(isTest() && isEnabled()) {
			if(System.currentTimeMillis() - lastUpdateTime >= (1000 / UPDATE_HZ)) {
				HandleInput();
				CalculateMotorSpeeds();
				ApplyMotorSpeeds();
				
				if(needToRecord) {
					if(System.currentTimeMillis() - startTime < 15000) {
						RecordAutonInput();
					} else {
						if(!autonWritten) {
							System.out.println("Saving auton " + AUTON_SIDE);
							WriteInputToFile();
							autonWritten = true;
						}
					}
				}
				
				lastUpdateTime = System.currentTimeMillis();
			}
		}
		
		autonWritten = false;
	}
	
	private void HandleInput() {
		leftStickY = controller.getY(Hand.kLeft);
		rightStickX = controller.getX(Hand.kRight);
		leftTrigger = controller.getTriggerAxis(Hand.kLeft);
		rightTrigger = controller.getTriggerAxis(Hand.kRight);
		
		if(!TWO_DRIVER) {
			yButton = controller.getYButton();
			bButton = controller.getBButton();
			aButton = controller.getAButton();
			xButton = controller.getXButton();
			leftB = controller.getBumper(Hand.kLeft);
			rightB = controller.getBumper(Hand.kRight);
			startButton = controller.getStartButton();
		} else {
			yButton = (controller.getYButton() || controller2.getYButton());
			bButton = (controller.getBButton() || controller2.getBButton());
			aButton = (controller.getAButton() || controller2.getAButton());
			xButton = (controller.getXButton() || controller2.getXButton());
			leftB = (controller.getBumper(Hand.kLeft) || controller2.getBumper(Hand.kLeft));
			rightB = (controller.getBumper(Hand.kRight) || controller2.getBumper(Hand.kRight));
			startButton = (controller.getStartButton() || controller2.getStartButton());
		}
	}
	
	private void CalculateMotorSpeeds() {
		double multiplierY = (Math.abs(leftStickY) > DEAD_ZONE) ? leftStickY : 0.0;
		double multiplierX = (Math.abs(rightStickX) > DEAD_ZONE) ? rightStickX : 0.0;
		double multiplierSpeed = NORMAL_SPEED;
		
		if(leftTrigger > DEAD_ZONE && rightTrigger <= DEAD_ZONE) {
			multiplierSpeed = BOOST_SPEED;
		} else if(leftTrigger <= DEAD_ZONE && rightTrigger > DEAD_ZONE) {
			multiplierSpeed = SLOWER_SPEED;
		}
		
		if(multiplierX == 0.0) {
			wheelLeftSpeed = -(multiplierY * multiplierSpeed);
			wheelRightSpeed = -(-multiplierY * multiplierSpeed);
		} else {
			wheelLeftSpeed = -Clamp(((-multiplierX * TURN_MODIFIER + multiplierY) * multiplierSpeed), -1.0, 1.0);
			wheelRightSpeed = -Clamp(((-multiplierX * TURN_MODIFIER - multiplierY) * multiplierSpeed), -1.0, 1.0);
		}
		
		gearIOSpeed = 0.0;
		if(leftB && !rightB) {
			gearIOSpeed = GEAR_IO_SPEED;
		} else if(!leftB && rightB) {
			gearIOSpeed = -GEAR_IO_SPEED;
		}
		
		climberSpeed = 0.0;
		if(xButton) {
			climberSpeed = -CLIMBER_SPEED;
		} else if(!xButton && startButton) {
			climberSpeed = CLIMBER_SPEED;
		}
	}
	
	private double Clamp(double val, double min, double max) {
		if(val > max) {
			return max;
		}
		
		if(val < min) {
			return min;
		}
		
		return val;
	}
	
	private void ApplyMotorSpeeds() {		
		wheelLeftFront.set(wheelLeftSpeed);
		wheelLeftRear.set(wheelLeftSpeed);
		wheelRightFront.set(wheelRightSpeed);
		wheelRightRear.set(wheelRightSpeed);
		gearIO.set(gearIOSpeed);
		climber.set(climberSpeed);
	}
}