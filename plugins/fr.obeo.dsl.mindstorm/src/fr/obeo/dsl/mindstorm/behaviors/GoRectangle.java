package fr.obeo.dsl.mindstorm.behaviors;

import fr.obeo.dsl.mindstorm.AbstractRobot;

public class GoRectangle extends RobotBehavior {

	private int d;
	
	public GoRectangle(AbstractRobot robot) {
		super(robot);
		d = 10;
	}

	@Override
	public boolean takeControl() {
		return stop == false && true;
	}

	@Override
	public void action() {
		suppressed = false;
		while (d < 200) {			
			robot.travel(d);
			while (robot.isMoving() && !suppressed)Thread.yield();
			robot.rotate(-90);
			robot.travel(d);
			while (robot.isMoving() && !suppressed)Thread.yield();
			robot.rotate(-90);
			robot.travel(d);
			while (robot.isMoving() && !suppressed)Thread.yield();
			robot.rotate(-90);
			robot.travel(d);
			while (robot.isMoving() && !suppressed)Thread.yield();
			robot.rotate(-90);
			d += 10;
		}
		d = 10;
	}

	@Override
	public void suppress() {
		suppressed = true;
	}

}