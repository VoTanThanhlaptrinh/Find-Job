package com.job_web.models;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name= "users",indexes = {
		@Index(name = "mulitIndex1", columnList = "id, email"),})
@SQLRestriction("status <> 'DELETED'")
public class User extends StatusEntity implements UserDetails, Principal {
	@Setter
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private String fullName;
	@Embedded
	private com.job_web.models.vo.Password password;
	private LocalDate dateOfBirth;
	private String role;
	@Column(unique = true)
	@Embedded
	private com.job_web.models.vo.EmailAddress email;
	private String address;
	@Embedded
	private com.job_web.models.vo.PhoneNumber mobile;
	@Setter
	private boolean accountLocked;
	private boolean enabled;
	private boolean active;
	@Setter
	private boolean oauth2Enabled;
	@Setter
	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createDate;
	@Setter
	@LastModifiedDate
	@Column(insertable = false)
	private LocalDateTime lastModifiedDate;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Apply> applies = new LinkedList<>();
	
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Resume> resumes = new ArrayList<>();

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		StringTokenizer tokenizer = new StringTokenizer(role, " ");
		List<SimpleGrantedAuthority> authorities = new LinkedList<>();
		while (tokenizer.hasMoreTokens()) {
			authorities.add(new SimpleGrantedAuthority(tokenizer.nextToken()));
		}
		return authorities;
	}

	@Override
	public String getPassword() {
		// TODO Auto-generated method stub
		return password.getValue();
	}

	@Override
	public boolean isAccountNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// TODO Auto-generated method stub
		return !accountLocked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return enabled;
	}
	public void addApplication(Apply apply) {
		if(applies == null) {
			applies = new LinkedList<>();
		}
		applies.add(apply);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return email.getValue();
	}

	@Override
	public String getUsername() {
		return email != null ? email.getValue() : null;
	}

	public String getEmail() {
		return email != null ? email.getValue() : null;
	}

	public String getMobile() {
		return mobile != null ? mobile.getValue() : null;
	}

	public void setPassword(com.job_web.models.vo.Password password) {
		this.password = password;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		if (dateOfBirth == null) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.dob.required"));
		}
		if (dateOfBirth.isAfter(LocalDate.now())) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.dob.past"));
		}
		this.dateOfBirth = dateOfBirth;
	}

	public void setEmail(com.job_web.models.vo.EmailAddress email) {
		this.email = email;
	}

	public void setFullName(String fullName) {
		if (fullName == null || fullName.trim().isEmpty()) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.fullname.required"));
		}
		this.fullName = fullName;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setRole(String role) {
		if (role == null || role.trim().isEmpty()) {
			throw new com.job_web.exception.BadRequestException(com.job_web.utils.MessageUtils.getMessage("validation.role.required"));
		}
		this.role = role;
	}

	public void setMobile(com.job_web.models.vo.PhoneNumber mobile) {
		this.mobile = mobile;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}


