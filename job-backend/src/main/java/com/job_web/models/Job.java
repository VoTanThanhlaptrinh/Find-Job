package com.job_web.models;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Data
@Table(name = "job")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("status <> 'DELETED'")
public class Job extends StatusEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private String salary;
	private String time;
	@Column(columnDefinition = "text")
	private String requireDetails;
	@Column(columnDefinition = "text")
	private String description;
	@Column(columnDefinition = "text")
	private String skill;
	private LocalDateTime expiredDate;
	private String title;
	private int yearOfExperience;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hirer_id")
	@JsonIgnore
	private Hirer hirer;
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
	private String logo;
	public void addApplication(Apply apply) {
		if(applies == null) {
			applies = new LinkedList<>();
		}
		applies.add(apply);
	}
}


