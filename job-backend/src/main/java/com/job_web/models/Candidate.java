package com.job_web.models;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("status <> 'DELETED'")
public class Candidate extends StatusEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "third_user_id")
	private User user;
	private String fullName;
	private LocalDate birth;
	private String address;
	@Embedded
	private com.job_web.models.vo.PhoneNumber phoneNumber;
	@Embedded
	private com.job_web.models.vo.EmailAddress email;
	private Type type;
	@Setter
	private LocalDateTime createDate;
	private LocalDateTime modifiedDate;
	
	public Candidate(User user, String fullName, LocalDate birth, String address, String phoneNumber, String email,
			Type type, LocalDateTime createDate, LocalDateTime modifiedDate) {
		super();
		this.user = user;
		this.fullName = fullName;
		this.birth = birth;
		this.address = address;
		this.phoneNumber = phoneNumber != null && !phoneNumber.trim().isEmpty() ? new com.job_web.models.vo.PhoneNumber(phoneNumber) : null;
		this.email = email != null && !email.trim().isEmpty() ? new com.job_web.models.vo.EmailAddress(email) : null;
		this.type = type;
		this.createDate = createDate;
		this.modifiedDate = modifiedDate;
	}

	public Candidate() {

	}
	public static enum Type {
		MALE, FEMALE, LGBT;
	}
	
	public void setFullName(String fullName) {
		if (fullName == null || fullName.trim().isEmpty()) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.fullname.required"));
		}
		this.fullName = fullName;
	}

	public String getEmail() {
		return email != null ? email.getValue() : null;
	}

	public String getPhoneNumber() {
		return phoneNumber != null ? phoneNumber.getValue() : null;
	}

	public void setEmail(com.job_web.models.vo.EmailAddress email) {
		this.email = email;
	}

	public void setPhoneNumber(com.job_web.models.vo.PhoneNumber phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
}


