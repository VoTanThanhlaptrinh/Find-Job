package com.job_web.models;

import java.time.Instant;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Hirer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@OneToOne
	@JoinColumn(name = "user_id",referencedColumnName = "id")
	private User user;
	private String companyName;
	private int amountEmp;
	private String fullName;
	private String position;
	private String phoneNumber;
	private String email;
	private String taxNumber;
	private Instant createDate;
	private Instant modifiedDate;
	@OneToMany(mappedBy = "hirer")
	private List<Job> jobsPost;
	public Hirer(User user, String companyName, int amountEmp, String fullName, String position,
			String phoneNumber, String email, String taxNumber, Instant createDate, Instant modifiedDate) {
		super();
		this.user = user;
		this.companyName = companyName;
		this.amountEmp = amountEmp;
		this.fullName = fullName;
		this.position = position;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.taxNumber = taxNumber;
		this.createDate = createDate;
		this.modifiedDate = modifiedDate;
	}
}
