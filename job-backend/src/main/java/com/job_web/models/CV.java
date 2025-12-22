package com.job_web.models;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CV {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@ManyToOne
	@JsonIgnore
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
	
	@Lob
	private byte[] data;

	private String fileName;

    @CreatedDate
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime lastModifyDate;

}
