package com.tektonpartners.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DatabaseRow implements Map{
	HashMap<String,Object> data_ = new HashMap<String, Object>() ;
	
	DatabaseRow () {
		}
	
	public DatabaseRow(HashMap<String,Object> hashMap) {
		data_ = hashMap ;
		}

	public String getString( String name ) {
		return (String) data_.get(name) ;
		}
	
	public Integer getInteger( String name ) {
		return (Integer) data_.get(name) ;		
		}

	public void clear() {
		data_.clear() ;		
		}

	public boolean containsKey(Object arg0) {
		return data_.containsKey(arg0);
		}

	public boolean containsValue(Object arg0) {
		return data_.containsValue(arg0);
		}

	public Set entrySet() {
		return data_.entrySet();
		}

	public Object get(Object arg0) {
		return data_.get(arg0);
		}

	public boolean isEmpty() {
		return data_.isEmpty();
		}

	public Set keySet() {
		return data_.keySet();
		}

	public Object put(Object arg0, Object arg1) {
		return data_.put((String)arg0, arg1);
		}

	public void putAll(Map arg0) {
		data_.putAll(arg0) ;
		}

	public Object remove(Object arg0) {
		return data_.remove(arg0);
		}

	public int size() {
		return data_.size();
		}

	public Collection values() {
		return data_.values();
		}
	
}
