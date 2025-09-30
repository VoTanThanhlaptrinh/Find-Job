package com.job_web.models;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Data

public class Apply {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "job_id", nullable = false)
	@JsonIgnore
	private Job job;
	
	@ManyToOne
	@JoinColumn(name = "cv_id", nullable = false)
	@JsonIgnore
	private CV cv;
	
	@ManyToOne
	@JsonIgnore
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime applyDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifyDate;
}
