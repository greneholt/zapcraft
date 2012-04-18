package edu.mines.zapcraft.FuelBehavior;


public interface ArduinoListener {
	public void readRpm(int rpm);
	public void readMpg(float mpg);
	public void readThrottle(int throttle);
	public void readSpeed(int speed);
	public void readAcceleration(float xAccel, float yAccel, float zAccel);
}
