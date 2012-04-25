package edu.mines.zapcraft.FuelBehavior;


public interface DataProvider {
	public DataHandler getDataHandler();
	public DbAdapter getDbAdapter();
	public DataLogger getDataLogger();
}
