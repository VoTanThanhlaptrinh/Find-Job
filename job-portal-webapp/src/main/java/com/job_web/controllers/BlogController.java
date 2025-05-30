package com.job_web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.job_web.data.BlogRepository;
import com.job_web.models.Blog;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class BlogController {
	private BlogRepository newsRepository;
	
	public BlogController(BlogRepository newsRepository) {
		super();
		this.newsRepository = newsRepository;
	}

	@GetMapping("/blogHome/blogSingle/{id}")
	public String getSinglePost(@PathVariable Long id, Model model) {
		Blog blog = newsRepository.findById(id).orElse(null);
		if(blog != null) {
			model.addAttribute("blog",blog);
			
		}else {
			return "blogHome";
		}
		
		return "blogSingle";
	}
}
