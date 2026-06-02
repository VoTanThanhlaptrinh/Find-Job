package com.job_web.recruiment.domain.model;


import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import com.job_web.application_process.domain.model.JobApplication;
import com.job_web.recruiment.domain.vo.EmploymentType;
import com.job_web.recruiment.domain.vo.ExperienceYears;
import com.job_web.shared.domain.exception.BadRequestException;
import com.job_web.shared.domain.model.StatusEntity;
import com.job_web.shared.utils.MessageUtils;
import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Getter
@Table(name = "job")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("status <> 'DELETED'")
public class Job extends StatusEntity {
	@Id
	@Setter
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private String salary;
	@Enumerated(EnumType.STRING)
	private EmploymentType time;
	@Column(columnDefinition = "text")
	private String requireDetails;
	@Column(columnDefinition = "text")
	private String description;
	@Column(columnDefinition = "text")
	private String skill;
	@Setter
	private LocalDateTime expiredDate;
	private String title;
	@Embedded
	private ExperienceYears yearOfExperience;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hirer_id")
	@JsonIgnore
	private Recruiment recruiment;
	@Setter
	@CreatedDate
	@Column(nullable = false)
	private LocalDateTime createDate;
	@LastModifiedDate
	@Column(nullable = true, updatable = true)
	private LocalDateTime modifiedDate;
	@OneToMany(mappedBy = "job", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
	@JsonIgnore
	private List<JobApplication> applies;
	@Column(columnDefinition = "text")
	private String moreDetail;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "address_id") // Khớp với cột address_id trong ảnh bạn gửi
	private Address address;
	@Setter
	private String logo;
	private Integer headcount;
	public void addApplication(JobApplication jobApplication) {
		if(applies == null) {
			applies = new LinkedList<>();
		}
		applies.add(jobApplication);
	}

	public void setTime(EmploymentType time) {
		if (time == null) {
			throw new BadRequestException(MessageUtils.getMessage("validation.job.type.required"));
		}
		this.time = time;
	}

	public void setDescription(String description) {
		if (description == null || description.trim().isEmpty()) {
			throw new BadRequestException(MessageUtils.getMessage("validation.job.description.required"));
		}
		this.description = description;
	}

	public void setRequireDetails(String requireDetails) {
		if (requireDetails == null || requireDetails.trim().isEmpty()) {
			throw new BadRequestException(MessageUtils.getMessage("validation.job.requirement.required"));
		}
		this.requireDetails = requireDetails;
	}

	public void setSalary(String salary) {
		if (salary == null || salary.trim().isEmpty()) {
			throw new BadRequestException(MessageUtils.getMessage("validation.job.salary.required"));
		}
		this.salary = salary;
	}

	public void setTitle(String title) {
		if (title == null || title.trim().isEmpty()) {
			throw new BadRequestException(MessageUtils.getMessage("validation.job.name.required"));
		}
		this.title = title;
	}

	public void setMoreDetail(String moreDetail) {
		this.moreDetail = moreDetail;
	}

	public void setHeadcount(Integer headcount) {
		if (headcount != null && headcount < 1) {
			throw new BadRequestException(MessageUtils.getMessage("validation.job.headcount.min"));
		}
		this.headcount = headcount;
	}

	public void setAddress(Address address) {
		if (address == null) {
			throw new BadRequestException(MessageUtils.getMessage("validation.job.address.required"));
		}
		this.address = address;
	}

	public void setRecruiment(Recruiment recruiment) {
		if (recruiment == null) {
			throw new BadRequestException(MessageUtils.getMessage("validation.job.hirer.required"));
		}
		this.recruiment = recruiment;
	}

	public void setSkill(String skill) {
		if (skill == null || skill.trim().isEmpty()) {
			throw new BadRequestException(MessageUtils.getMessage("validation.job.skill.required"));
		}
		this.skill = skill;
	}

	public Integer getYearOfExperience() {
		return yearOfExperience != null ? yearOfExperience.getValue() : 0;
	}

	public void setYearOfExperience(ExperienceYears yearOfExperience) {
		this.yearOfExperience = yearOfExperience;
	}
	public boolean isExpired() {
		return expiredDate != null && expiredDate.isBefore(LocalDateTime.now());
	}

	public boolean isOwnedBy(Recruiment h) {
		return recruiment != null && h != null && recruiment.getId() == h.getId();
	}
}


