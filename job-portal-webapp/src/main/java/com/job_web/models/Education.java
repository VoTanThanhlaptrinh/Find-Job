package com.job_web.models;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

@Entity
@Data
public class Education {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private String certificate;
	private String skill;
	private Timestamp createDate;
	private Timestamp modifiedDate;
	
	public Education(String certificate, String skill, Timestamp createDate, Timestamp modifiedDate) {
		super();
		this.certificate = certificate;
		this.skill = skill;
		this.createDate = createDate;
		this.modifiedDate = modifiedDate;
	}
	public Education() {
		super();
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getCertificate() {
		return certificate;
	}
	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}
	public String getSkill() {
		return skill;
	}
	public void setSkill(String skill) {
		this.skill = skill;
	}
	public Timestamp getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}
	public Timestamp getModifiedDate() {
		return modifiedDate;
	}
	public void setModifiedDate(Timestamp modifiedDate) {
		this.modifiedDate = modifiedDate;
	}
	
}
