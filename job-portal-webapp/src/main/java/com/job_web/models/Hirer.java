package com.job_web.models;

import java.time.Instant;
import java.util.List;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hirer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@OneToOne
	@JoinColumn(name = "user_id",referencedColumnName = "id")
	private User user;
	private String companyName;
	@Column(columnDefinition = "TEXT")
	private String description;
	@Column(columnDefinition = "TEXT")
	private String socialLink;
	@CreatedDate
	@Column(nullable = false)
	private Instant createDate;
	@LastModifiedDate
	@Column(nullable = true, updatable = true)
	private Instant modifiedDate;
	@OneToMany(mappedBy = "hirer")
	private List<Job> jobsPost;
}
