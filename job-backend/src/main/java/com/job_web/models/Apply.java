package com.job_web.models;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Apply {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn( nullable = false)
	@JsonIgnore
	private Job job;
	
	@ManyToOne
	@JoinColumn(nullable = false)
	@JsonIgnore
	private Resume resume;
	
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


