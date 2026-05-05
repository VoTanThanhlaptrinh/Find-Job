package com.job_web.models;

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

	@Setter
	@CreatedDate
	private LocalDateTime createDate;
	@LastModifiedDate
	private LocalDateTime lastModifyDate;

	public void setUser(User user) {
		if (user == null) {
			throw new NullPointerException("user is null");
		}
		this.user = user;
	}

	public void setKeyCf(String key) {
		if (key == null) {
			throw new NullPointerException("keyCf is null");
		}
		this.keyCf = key;
	}

	public void setFileName(String originalFilename) {
		if (originalFilename == null) {
			throw new NullPointerException("keyCf is null");
		}
		this.fileName = originalFilename;
	}
}
