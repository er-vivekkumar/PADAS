package org.processmining.database.redologs.common;

import java.io.Serializable;

public class TraceIDPatternElement implements Serializable {

	private Column c = null;
	private Key k = null;
	
	private boolean isKey = false;
	
	public TraceIDPatternElement(Key k) {
		this.k = k;
		this.isKey = true;
	}
	
	public TraceIDPatternElement(Column c) {
		this.c = c;
		this.isKey = false;
	}
	
	public boolean isKey() {
		return isKey;
	}
	
	public boolean isColumn() {
		return !isKey;
	}
	
	public Key getKey() {
		return k;
	}
	
	public Column getColumn() {
		return c;
	}
	
	@Override
	public int hashCode() {
		if (isKey()) {
			return k.hashCode();
		} else {
			return c.hashCode();
		}
	}
}
