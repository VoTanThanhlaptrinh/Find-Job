package com.job_web.models;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

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
	private String title;
	@Column(columnDefinition = "text")
	private String skill;
	@ManyToOne
	@JoinColumn(name = "hirer_id")
	@JsonIgnore
	private Hirer hirer;
	@CreatedDate
	@Column(nullable = false)
	private Instant createDate;
	@LastModifiedDate
	@Column(nullable = true, updatable = true)
	private Instant modifiedDate;
	@OneToMany(mappedBy = "job", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
	@JsonIgnore
	private List<Apply> applies;
	@Lob
	private byte[] logo;
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
