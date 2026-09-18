package com.nlu.applicationProcess.domain.model;

import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.model.BaseEntity;
import com.nlu.recruitment.domain.model.Address;
import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("record_status <> 'DELETED'")
public class Resume extends BaseEntity {
	@Id
	@Setter
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JsonIgnore
	private User user;
	private int yoe;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "address_id")
	private Address address;
	private String keyCf;
	private String fileName;
	private String title;

	@Setter
    @Column(columnDefinition = "text")
	private String rawText;

	@Enumerated(EnumType.STRING)
	@Column(name = "resume_status", nullable = false)
	private ResumeStatus status = ResumeStatus.UPLOADED;

	public void markUploaded() {
		this.status = ResumeStatus.UPLOADED;
	}

	public void startAnalysis() {
		if (this.status == ResumeStatus.READY) {
			throw new IllegalStateException("Resume is already analyzed and ready");
		}
		if (this.status == ResumeStatus.ANALYZING) {
			throw new IllegalStateException("Resume is currently being analyzed");
		}
		this.status = ResumeStatus.ANALYZING;
	}

	public void markReady() {
		if (this.status != ResumeStatus.ANALYZING) {
			throw new IllegalStateException("Cannot mark ready from status: " + this.status);
		}
		this.status = ResumeStatus.READY;
	}

	public void markAnalysisFailed() {
		if (this.status != ResumeStatus.ANALYZING) {
			throw new IllegalStateException("Cannot mark analysis failed from status: " + this.status);
		}
		this.status = ResumeStatus.ANALYSIS_FAILED;
	}

	public boolean isAnalyzed() {
		return this.status == ResumeStatus.READY;
	}

	public void setUser(User user) {
		if (user == null) {
			throw new BadRequestException("user is null");
		}
		this.user = user;
	}

	public void setKeyCf(String key) {
		if (key == null) {
			throw new BadRequestException("keyCf is null");
		}
		this.keyCf = key;
	}

	public void setFileName(String originalFilename) {
		if (originalFilename == null) {
			throw new BadRequestException("keyCf is null");
		}
		this.fileName = originalFilename;
	}
}
