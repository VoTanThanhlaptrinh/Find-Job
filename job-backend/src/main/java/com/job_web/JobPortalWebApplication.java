package com.job_web;

import javax.persistence.EntityListeners;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
@SpringBootApplication
@EntityListeners(AuditingEntityListener.class)
@EnableJpaAuditing
@EnableJpaRepositories
@EntityScan
public class JobPortalWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobPortalWebApplication.class, args);
	}
	
}
