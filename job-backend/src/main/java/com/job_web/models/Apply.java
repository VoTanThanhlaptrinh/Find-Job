package com.job_web.models;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("status <> 'DELETED'")
public class Apply extends StatusEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn( nullable = false)
	@JsonIgnore
	private Job job;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	@JsonIgnore
	private Resume resume;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JsonIgnore
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

    @Setter
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime applyDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifyDate;

    public void setUser(User user) {
		if(user == null){
			throw new NullPointerException("user is null");
		}
		this.user = user;
	}

	public void setResume(Resume resume) {
		if(resume == null){
			throw new NullPointerException("Resume is null");
		}
		this.resume = resume;
	}

	public void setJob(Job job) {
		if(job == null){
			throw new NullPointerException("Job is null");
		}
		this.job = job;
	}
}


