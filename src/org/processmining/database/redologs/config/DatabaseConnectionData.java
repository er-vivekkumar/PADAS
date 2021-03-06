package org.processmining.database.redologs.config;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;


public class DatabaseConnectionData implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5712587551466284618L;
	
	public String nameConnection = "";
	public DatabaseTypes type = null;
	public String hostname  = "";
	public Integer port = 0;
	public String username = "";
	public String password = "";
	public List<String> dbname = new Vector<>();
	public String service = "XE";
	public List<String> redologFiles = new Vector<>();
	public boolean isDictionaryOnline = true;
	public String dictionaryPath = "";
	public boolean switchRootContainer = false;
	public boolean isRedoLogFilesListOnline = true;
	public boolean newToOld = true;
		
	@Override
	public String toString() {
		return nameConnection;
	}
}
