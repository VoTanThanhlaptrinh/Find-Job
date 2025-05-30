package com.job_web.controllers;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.job_web.data.JobRepository;
import com.job_web.data.UserRepository;
import com.job_web.models.Job;

@Controller
@RequestMapping("/category")

public class UpdateJob {
	
	private JobRepository jobRepository;
	private UserRepository userRepository;

	public UpdateJob( JobRepository jobRepository, UserRepository userRepository) {
		super();
		this.jobRepository = jobRepository;
		this.userRepository = userRepository;
	}

	@GetMapping
	public String update(Model model, Principal principal) {
		// TODO: process POST request
		List<Job> jobs = new ArrayList<>();
		jobRepository.findAll().forEach(j -> jobs.add(j));
		model.addAttribute("jobs", jobs);
		try {
//			String username = principal.getName();
//			if (userRepository.findByUsername(username) != null) {
//				model.addAttribute("user", userRepository.findByUsername(username));
//			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return "category";
	}



	@GetMapping("/single/{id}")
	public String goToJobDetail(@PathVariable Long id, Model model) {

		Job job = jobRepository.findById(id).orElse(null);
		if (job == null) {
			return "category";
		}
		model.addAttribute("job", job);
		return "single";
	}

}
