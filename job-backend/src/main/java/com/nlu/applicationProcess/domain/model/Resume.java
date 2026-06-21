package com.nlu.applicationProcess.domain.model;

import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.model.BaseEntity;
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

	@Column(nullable = false, columnDefinition = "boolean default false")
	private boolean isAnalyzed = false;

    public void markAnalyzed() {
		this.isAnalyzed = true;
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
