package com.job_web.models;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import com.job_web.constant.EmploymentType;
import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import lombok.Data;
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
	private com.job_web.models.vo.ExperienceYears yearOfExperience;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hirer_id")
	@JsonIgnore
	private Hirer hirer;
	@Setter
	@CreatedDate
	@Column(nullable = false)
	private LocalDateTime createDate;
	@LastModifiedDate
	@Column(nullable = true, updatable = true)
	private LocalDateTime modifiedDate;
	@OneToMany(mappedBy = "job", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
	@JsonIgnore
	private List<Apply> applies;
	@Column(columnDefinition = "text")
	private String moreDetail;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "address_id") // Khớp với cột address_id trong ảnh bạn gửi
	private Address address;
	@Setter
	private String logo;
	private Integer headcount;
	public void addApplication(Apply apply) {
		if(applies == null) {
			applies = new LinkedList<>();
		}
		applies.add(apply);
	}

	public void setTime(EmploymentType time) {
		if (time == null) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.job.type.required"));
		}
		this.time = time;
	}

	public void setDescription(String description) {
		if (description == null || description.trim().isEmpty()) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.job.description.required"));
		}
		this.description = description;
	}

	public void setRequireDetails(String requireDetails) {
		if (requireDetails == null || requireDetails.trim().isEmpty()) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.job.requirement.required"));
		}
		this.requireDetails = requireDetails;
	}

	public void setSalary(String salary) {
		if (salary == null || salary.trim().isEmpty()) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.job.salary.required"));
		}
		this.salary = salary;
	}

	public void setTitle(String title) {
		if (title == null || title.trim().isEmpty()) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.job.name.required"));
		}
		this.title = title;
	}

	public void setMoreDetail(String moreDetail) {
		this.moreDetail = moreDetail;
	}

	public void setHeadcount(Integer headcount) {
		if (headcount != null && headcount < 1) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.job.headcount.min"));
		}
		this.headcount = headcount;
	}

	public void setAddress(Address address) {
		if (address == null) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.job.address.required"));
		}
		this.address = address;
	}

	public void setHirer(Hirer hirer) {
		if (hirer == null) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.job.hirer.required"));
		}
		this.hirer = hirer;
	}

	public void setSkill(String skill) {
		if (skill == null || skill.trim().isEmpty()) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.job.skill.required"));
		}
		this.skill = skill;
	}

	public Integer getYearOfExperience() {
		return yearOfExperience != null ? yearOfExperience.getValue() : 0;
	}

	public void setYearOfExperience(com.job_web.models.vo.ExperienceYears yearOfExperience) {
		this.yearOfExperience = yearOfExperience;
	}
}


