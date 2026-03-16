package com.job_web.models;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Token {
	@Id
	@GeneratedValue
	private Long id;
	private String token;
	private LocalDateTime createAt;
	private LocalDateTime expiresAt;
	private LocalDateTime validatedAt;
	
	@ManyToOne
	@JoinColumn(name="userId", nullable = false)
	private User user;
	
}


