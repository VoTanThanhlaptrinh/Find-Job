package com.job_web.models;

public class Change {
	private String oldPass;
	private String newPass;
	private String rePass;
	public Change(String oldPass, String newPass, String rePass) {
		super();
		this.oldPass = oldPass;
		this.newPass = newPass;
		this.rePass = rePass;
	}
	public String getOldPass() {
		return oldPass;
	}
	public String getNewPass() {
		return newPass;
	}
	public String getRePass() {
		return rePass;
	}
	
}
