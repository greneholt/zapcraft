package edu.mines.zapcraft.FuelBehavior;


public interface ViewChangeListener {
	public static int INSTANT = 0;
	public static int MAP = 1;
	public static int LOGS = 2;
	public static int SETTINGS = 3;

	public void onViewChange(int view);
}
