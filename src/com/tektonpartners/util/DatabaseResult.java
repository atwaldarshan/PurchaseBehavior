package com.tektonpartners.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DatabaseResult extends HashMap<String, Object>
implements Map<String, Object>, Cloneable, Serializable
{

	public DataSet getDataSet() {
		return (DataSet)this.get("dataset");
	}

}
