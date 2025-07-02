package com.job_web.models;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class HiringDecision {
	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY)
	private long id;
	private long jobId;
	private long candidateId;
	private String result;
	private Timestamp createDate;
	private Timestamp modifiedDate;
	public HiringDecision(long jobId, long candidateId, String result, Timestamp createDate, Timestamp modifiedDate) {
		super();
		this.jobId = jobId;
		this.candidateId = candidateId;
		this.result = result;
		this.createDate = createDate;
		this.modifiedDate = modifiedDate;
	}
	public HiringDecision() {
		super();
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getJobId() {
		return jobId;
	}
	public void setJobId(long jobId) {
		this.jobId = jobId;
	}
	public long getCandidateId() {
		return candidateId;
	}
	public void setCandidateId(long candidateId) {
		this.candidateId = candidateId;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
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
