package edu.mines.zapcraft.FuelBehavior;


public interface DataProvider {
	public DataHandler getDataHandler();
	public DbAdapter getDbAdapter();
	public DataLogger getDataLogger();

	public void startLogging();
	public void stopLogging();
	public void resumeLogging();
	public boolean isLogging();
	boolean canResumeLogging();

	public void displayMessage(String message);
}
