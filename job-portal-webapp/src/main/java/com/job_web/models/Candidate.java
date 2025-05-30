package com.job_web.models;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import lombok.Data;

@Entity
@Data
public class Candidate {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@OneToOne
	@JoinColumn(name = "third_user_id")
	private User user;
	private String fullName;
	private Timestamp birth;
	private String address;
	private String phoneNumber;
	private String email;
	private Type type;
	private Timestamp createDate;
	private Timestamp modifiedDate;
	
	public Candidate(User user, String fullName, Timestamp birth, String address, String phoneNumber, String email,
			Type type, Timestamp createDate, Timestamp modifiedDate) {
		super();
		this.user = user;
		this.fullName = fullName;
		this.birth = birth;
		this.address = address;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.type = type;
		this.createDate = createDate;
		this.modifiedDate = modifiedDate;
	}

	public static enum Type {
		MALE, FEMALE, LGBT;
	}
	
}
