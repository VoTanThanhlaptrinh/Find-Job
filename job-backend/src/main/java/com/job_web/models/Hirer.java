package com.job_web.models;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("status <> 'DELETED'")
public class Hirer extends StatusEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id",referencedColumnName = "id")
	private User user;
	private String companyName;
	@Setter
	@Column(columnDefinition = "TEXT")
	private String description;
	@Embedded
	private com.job_web.models.vo.SocialLink socialLink;
	@Setter
	@CreatedDate
	@Column(nullable = false)
	private LocalDateTime createDate;
	@Setter
	@LastModifiedDate
	@Column(updatable = true)
	private LocalDateTime modifiedDate;
	@OneToMany(mappedBy = "hirer", fetch = FetchType.LAZY)
	private List<Job> jobsPost;
	@Setter
	@OneToMany(targetEntity = Address.class, fetch = FetchType.LAZY)
	List<Address> addresses;
	public boolean isExistAddress(Address address){
		return addresses.stream().anyMatch(a -> a.getId().equals(address.getId()));
	}

	public void setUser(User user) {
		if(user == null){
			throw new NullPointerException("user is null");
		}
		this.user = user;
	}

	public void setCompanyName(String companyName) {
		if (companyName == null || companyName.trim().isEmpty()) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.company.name.required"));
		}
		this.companyName = companyName;
	}

	public String getSocialLink() {
		return socialLink != null ? socialLink.getValue() : null;
	}

	public void setSocialLink(com.job_web.models.vo.SocialLink socialLink) {
		this.socialLink = socialLink;
	}
}


