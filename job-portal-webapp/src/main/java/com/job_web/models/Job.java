package com.job_web.models;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "job")
@NoArgsConstructor
public class Job {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private double salary;
	private String time;
	@Column(columnDefinition = "text")
	private String requireDetails;
	private String address;
	@Column(columnDefinition = "text")
	private String description;
	private Instant expiredDate;
	private Instant createDate;
	private Instant modifiedDate;
	private String title;
	@Column(columnDefinition = "text")
	private String skill;
	@ManyToOne
	@JoinColumn(name = "hirer_id")
	@JsonIgnore
	private Hirer hirer;
	@OneToMany(mappedBy = "job", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
	@JsonIgnore
	private List<Apply> applies;
	public Job(double salary, String time, String requireDetails, String address, String description,
			Instant expiredDate, Instant createDate, Instant modifiedDate, String title) {
		super();
		this.salary = salary;
		this.time = time;
		this.requireDetails = requireDetails;
		this.address = address;
		this.description = description;
		this.expiredDate = expiredDate;
		this.createDate = createDate;
		this.modifiedDate = modifiedDate;
		this.title = title;
	}

	public void addApplication(Apply apply) {
		if(applies == null) {
			applies = new LinkedList<>();
		}
		applies.add(apply);
	}
	

}
