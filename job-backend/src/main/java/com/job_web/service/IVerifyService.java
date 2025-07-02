package com.job_web.service;

public interface IVerifyService {
	public boolean containsKey(String key);

	public void add(String key, String value, long secondTimeout);

	public void add(String key, String value);

	public Object getValue(String key);
	
	public void delete(String key);
}
