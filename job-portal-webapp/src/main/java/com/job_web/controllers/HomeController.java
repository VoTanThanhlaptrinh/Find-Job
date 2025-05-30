package com.job_web.controllers;

import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.job_web.data.BlogRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.UserRepository;
import com.job_web.models.Blog;
import com.job_web.models.Change;
import com.job_web.models.Job;
import com.job_web.service.IVerifyService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class HomeController {
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private JobRepository jobRepository;
	@Autowired
	private BlogRepository blogRepository;
	@Autowired
	private PasswordEncoder encode;

	private IVerifyService verifyService;

	public HomeController(IVerifyService verifyService) {
		super();
		this.verifyService = verifyService;
	}

	@GetMapping("/")
	public String home(Model model, HttpSession session) {
		PageRequest jopBySalary = PageRequest.of(0, 7, Sort.by("salary").descending());
		PageRequest topJobBy = PageRequest.of(0, 5, Sort.by("createDate").ascending());
		PageRequest blogByTime = PageRequest.of(0, 3, Sort.by("amountLike").descending());
		try {
			model.addAttribute("jobs", jobRepository.findAll(jopBySalary));
			model.addAttribute("topJobs", jobRepository.findAll(topJobBy));
			model.addAttribute("blogs", blogRepository.findAll(blogByTime));
		} catch (Exception e) {
			return "error";
		}
		return "index";
	}

	@GetMapping("/single/{id}")
	public String goToJobDetail(@PathVariable Long id, Model model) {
		Job job = jobRepository.findById(id).orElse(null);
		if (job == null) {
			return "index";
		}
		model.addAttribute("job", job);
		return "single";
	}

	@PostMapping("/search")
	public String searchPage(Model model, @RequestParam String title) {
		try {
			// PageRequest jopBySalary = PageRequest.of(0, 7,
			// Sort.by("salary").descending());
			if (title == null) {
				PageRequest topJobBy = PageRequest.of(0, 5, Sort.by("createDate").ascending());
				PageRequest blogByTime = PageRequest.of(0, 3, Sort.by("amountLike").descending());
				model.addAttribute("jobs", jobRepository.findAll());
				model.addAttribute("topJobs", jobRepository.findAll(topJobBy));
				model.addAttribute("blogs", blogRepository.findAll(blogByTime));
			} else {
				List<Job> jobs = jobRepository.findByTitle(title);
				if (jobs.isEmpty()) {
					model.addAttribute("mess", "không tìm thấy công việc phù hợp");
				} else {
					model.addAttribute("jobs", jobs);
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			log.error(e.getMessage());
		}

		return "search";
	}

	@GetMapping("/blogHome")
	public String getBlogHome(Model model) {
		List<Blog> blogs = (List<Blog>) blogRepository.findAll();
		model.addAttribute("blogs", blogs);
		return "blogHome";
	}

	@GetMapping("/contact")
	public String getContact(Model model) {
		return "contact";
	}

	@GetMapping("/about")
	public String getAbout(Model model) {
		return "about";
	}

	@GetMapping("/price")
	public String getPrice() {
		return "price";
	}

	@GetMapping("/login")
	public String getLogin(Principal principal) {
		if (principal != null) {
			return "index";
		}
		return "login";
	}

	@GetMapping("/elements")
	public String getElement() {
		return "elements";
	}

	@GetMapping("/blog_single")
	public String getSingle() {
		return "blogSingle";
	}

	@GetMapping("/info")
	public String info(Model model, Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		try {
//			User user = userRepository.findByUsername(principal.getName());
//			log.info(user.getFullName());
//			model.addAttribute("user", user);
		} catch (Exception e) {
			// TODO: handle exception
			getError();
		}
		return "info";
	}

	@GetMapping("/rePass")
	public String getRePass(Principal principal, Model model) {
		if (principal == null) {
			return "redirect:/login";
		}
		return "changePass";
	}

	@PostMapping("/rePass")
	public String postRePass(Change c, Principal principal, Model model) {
		try {
//			User user = userRepository.findByUsername(principal.getName());
//			if (!encode.matches(c.getOldPass(), user.getPassword())) {
//				log.info("mật khẩu cũ không đúng");
//				return "changePass";
//			} else {
//				if (c.getNewPass().equals(c.getOldPass())) {
//					log.info("Mật khẩu cũ trùng với mật khẩu mới");
//					return "changePass";
//				}
//				user.setPassword(encode.encode(c.getNewPass()));
//				userRepository.save(user);
//				log.info("Đổi mật khẩu thành công");
//				model.addAttribute("user", user);
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "index";
	}

	@GetMapping("/forgot")
	public String forgotGet(Principal principal, Model model) {
		if (principal == null) {
			return "forgot";
		}
		return "index";
	}

	@PostMapping("/forgot")
	public String forgotPost(@RequestBody String entity) {
		return entity;
	}

	@GetMapping("/recovery")
	public String getRecoverPass(@RequestParam String token, Model model) {
		model.addAttribute("token", token);
		return "recoverPass";
	}

	@PostMapping("/recovery")
	public String postRecoverPass(@RequestParam String newPass, @RequestParam String token) {
		// Tìm user bên trong redis
//		User user = userRepository.findByUsername((String) verifyService.getValue("user-verify:" + token));
//		// Set lại password
//		user.setPassword(encode.encode(newPass));
//		// update user
//		userRepository.save(user);
//		// xoá token khi hoàn thành form
//		verifyService.delete("recovery:" + token);
		// redirect sang login
		return getLogin(null);
	}

	@GetMapping("/error")
	public String getError() {
		return "error";
	}

	@GetMapping("/apply/{id}")
	public String getApplyTemplate(@PathVariable long id, Model model, Principal principal) {
		model.addAttribute("jobId", id);
//		User user = userRepository.findByUsername(principal.getName());
//		model.addAttribute("user", user);
		return "apply";
	}

}
