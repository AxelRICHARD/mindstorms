package fr.obeo.dsl.mindstorms;

import java.util.concurrent.ThreadLocalRandom;

import lejos.hardware.Button;
import lejos.hardware.Key;
import lejos.hardware.KeyListener;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.Color;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.robotics.geometry.Point;
import lejos.robotics.localization.OdometryPoseProvider;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.Navigator;
import lejos.robotics.navigation.Pose;
import lejos.utility.Delay;
import lejos.utility.Timer;
import lejos.utility.TimerListener;

public abstract class AbstractRobot {
	private static final int TRACK_WIDTH = 120;
	private static final int WHEEL_DIAMETER = 56;

	private RegulatedMotor grabMotor = new EV3MediumRegulatedMotor(MotorPort.A);
	private RegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
	private RegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);
	private EV3UltrasonicSensor sonar = new EV3UltrasonicSensor(SensorPort.S4);
	private EV3TouchSensor touch = new EV3TouchSensor(SensorPort.S3);
	private EV3ColorSensor color = new EV3ColorSensor(SensorPort.S2);
	private DifferentialPilot pilot = new DifferentialPilot(WHEEL_DIAMETER, TRACK_WIDTH, leftMotor, rightMotor);
	private Navigator navigator = new Navigator(pilot);
	private OdometryPoseProvider poseProvider = new OdometryPoseProvider(pilot);
	private Pose goal;
	private final int gameTime; 
	
	public enum PlaygroundArea {
		BASE, NEUTRAL, OPPONENT_BASE
	}
	
	public AbstractRobot() {
		this(30000); // 30 seconds : default game time
	}
	
	public AbstractRobot(int gameTime) {
		this.gameTime = gameTime;
	}

	public abstract void execute();

	public void waitForStartSignal() {
		System.out.println("Ready...");
		Button.waitForAnyPress();
	}
	
	public void terminateProgramOnExitButton() {
		Button.ESCAPE.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(Key k) {
				if (Button.ESCAPE.equals(k)) {
					System.exit(0);
				}
			}

			@Override
			public void keyReleased(Key k) {
			}
		});
	}

	public void run() {
		// Wait for start signal
		this.waitForStartSignal();

		// Whenever the user push the escape button during the game time, it will exit the program.
		this.terminateProgramOnExitButton();
		
		// Initialize timer
		Timer timer = new Timer(gameTime, new TimerListener() {
			@Override
			public void timedOut() {
				System.out.println("Stop");
				pilot.stop();
				leftMotor.close();
				rightMotor.close();
				sonar.close();
				color.close();
				touch.close();
				System.exit(0);
			}
		});

		// Start timer
		timer.start();

		// Launch robot execution
		try {
			this.execute();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		timer.stop();
	}

	// Actions
	
	public void goForward() {
		pilot.forward();
	}
	
	public void goForward(int distance) {
		pilot.travel(distance*10);
	}
	
	public void goBackward() {
		pilot.backward();
	}
	
	public void goBackward(int distance) {
		pilot.travel(-distance*10);
	}

	public void delay(int ms) {
		Delay.msDelay(ms);
	}
	
	public void grab() {
		grabMotor.backward();
		while (!grabMotor.isStalled())
			Thread.yield();
	}

	public void release() {
		grabMotor.forward();
		while (!grabMotor.isStalled())
			Thread.yield();
	}
	
	public void rotate(double angle) {
		pilot.rotate(angle);
	}
	
	public void rotateRandomly() {
		rotate(ThreadLocalRandom.current().nextDouble(0, 360 + 1));
	}
	
	public void returnToBase() {
		goTo(0, 0);
	}
	
	public void goTo(float x, float y) {
		Pose currentPose = poseProvider.getPose();
		float heading = currentPose.relativeBearing(new Point(x, y));
		float distance = currentPose.distanceTo(new Point(x, y));
		pilot.rotate(heading);
		pilot.travel(distance);
	}
	
	// Sensors
	
	public boolean touchSensorIsPressed() {
		try {
			SampleProvider sampleProvider = touch.getTouchMode();
			float[] sample = new float[1];
			sampleProvider.fetchSample(sample, 0);
			return sample[0] == 1;
		} catch (IndexOutOfBoundsException ex) {
			return false;
		}
	}
	
	public float getDistance() {
		try {
			SampleProvider sampleProvider = sonar.getDistanceMode();
			float[] sample = new float[1];
			sampleProvider.fetchSample(sample, 0);
			return sample[0];
		} catch (IndexOutOfBoundsException ex) {
			return 0;
		}
	}
	
	public int getColor() {
		return color.getColorID();
	}
	
	// Misc.
	
	public void explore(double distance, double angle) {
		pilot.rotate(angle);
		pilot.travel(distance);
	}

	public EV3ColorSensor getColorSensor() {
		return color;
	}

	public void stop() {
		pilot.stop();
	}

	public DifferentialPilot getPilot() {
		return pilot;
	}

	public Navigator getNavigator() {
		return navigator;
	}
	
	public Pose getGoal() {
		return goal;
	}

	public Pose getCurrentPosition() {
		return poseProvider.getPose();
	}

	public boolean obstacleDetected() {
		try {
			SampleProvider sampleProvider = sonar.getDistanceMode();
			float[] sample = new float[1];
			sampleProvider.fetchSample(sample, 0);
			return sample[0] < 0.30; // 30 cm
		} catch (IndexOutOfBoundsException ex) {
			return false;
		}
	}

	public boolean isMoving() {
		return pilot.isMoving();
	}

	public float getColoredPoint() {
		try {
			SampleProvider sampleProvider = color.getColorIDMode();
			float[] sample = new float[1];
			sampleProvider.fetchSample(sample, 0);
			return sample[0];
		} catch (IndexOutOfBoundsException ex) {
			return Color.NONE;
		}
	}
}
