package com.nlu.applicationProcess.domain.model;

import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.model.StatusEntity;
import com.nlu.recruitment.domain.model.Address;
import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("status <> 'DELETED'")
@EntityListeners(AuditingEntityListener.class)
public class Resume extends StatusEntity {
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

	@Column(columnDefinition = "text")
	private String rawText;

	@Column(nullable = false)
	private boolean isAnalyzed = false;

	public void setRawText(String rawText) {
		this.rawText = rawText;
	}

	public void markAnalyzed() {
		this.isAnalyzed = true;
	}

	@Setter
	@CreatedDate
	private LocalDateTime createDate;
	@LastModifiedDate
	private LocalDateTime lastModifyDate;

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
