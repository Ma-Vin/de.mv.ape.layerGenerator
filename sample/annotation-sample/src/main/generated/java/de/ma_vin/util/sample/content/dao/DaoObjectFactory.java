package de.ma_vin.util.sample.content.dao;

public class DaoObjectFactory {

	private DaoObjectFactory() {
	}

	public static RootEntityDao createRootEntityDao() {
		return new RootEntityDao();
	}

	public static SubEntityDao createSubEntityDao() {
		return new SubEntityDao();
	}

}
