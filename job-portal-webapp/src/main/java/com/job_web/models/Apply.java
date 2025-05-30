package com.job_web.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.Data;

@Entity
@Data

public class Apply {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "job_id", nullable = false)
	private Job job;
	
	@ManyToOne
	@JoinColumn(name = "cv_id", nullable = false)
	private CV cv;
	
	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
}
