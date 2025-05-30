package com.job_web;


import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.job_web.data.BlogRepository;
import com.job_web.data.HirerRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.UserRepository;
import com.job_web.models.Blog;
import com.job_web.models.Hirer;
import com.job_web.models.Job;
import com.job_web.models.User;

@Configuration
public class Init {
	@Bean
	CommandLineRunner commandLineRunner(UserRepository userRepository, PasswordEncoder encoder,
			JobRepository jobRepository, HirerRepository hirerRepository, BlogRepository newsRepository) {
		return new CommandLineRunner() {

			@Override
			public void run(String... args) throws Exception {
				// TODO Auto-generated method stub
				if(userRepository.findByEmail("vtthanh32004@gmail.com").isEmpty()) {
					User user = new User();
					user.setEmail("vtthanh32004@gmail.com");
					user.setPassword(encoder.encode("123"));
					user.setRole("USER ADMIN");
					user.setFullName("Võ Tấn Thành");
					user.setAddress("Hồ Chí Minh");
					user.setMobile("0123456789");
					user.setEnabled(true);
					userRepository.save(user);
					Instant timestamp = Instant.now();
					
					Hirer hirer = new Hirer(user, "ABC", 50, "Võ Tấn Thành", "Manager", "0796692184",
							"thanhABC@gmail.com", "abc123", timestamp, timestamp);
					
					hirerRepository.save(hirer);
					List<Job> jobList = new LinkedList<>();
					
					// 1. Backend Developer
					jobList.add(new Job(50000.00, "Full Time",
							"3+ years of experience in Java development, knowledge of Spring Boot, RESTful APIs", "Hà Nội",
							"Looking for a Backend Developer to build and maintain our server-side applications.",
							Instant.now(), timestamp, timestamp, "Backend Developer"));

					// 2. Frontend Developer
					jobList.add(new Job(45000.00, "Full Time",
							"2+ years of experience in Frontend development, proficient in HTML, CSS, JavaScript, React.js",
							"Hồ Chí Minh",
							"We are looking for a creative Frontend Developer to enhance our user interfaces.",
							Instant.now(), timestamp, timestamp, "Frontend Developer"));

					// 3. Data Analyst
					jobList.add(new Job( 55000.00, "Part Time",
							"3+ years of experience in data analysis, strong knowledge of SQL, Python, and data visualization tools",
							"Đà Nẵng",
							"Join our team as a Data Analyst to help us uncover insights from complex data sets.",
							Instant.now(), timestamp, timestamp, "Data Analyst"));

					// 4. UI/UX Designer
					jobList.add(new Job( 47000.00, "Full Time",
							"3+ years of experience in UI/UX design, knowledge of design tools like Figma, Sketch, Adobe XD",
							"Hà Nội", "We need a UI/UX Designer to create user-friendly designs for our applications.",
							Instant.now(), timestamp, timestamp, "UI/UX Designer"));

					// 5. DevOps Engineer
					jobList.add(new Job( 60000.00, "Full Time",
							"4+ years of experience in DevOps, knowledge of CI/CD pipelines, cloud platforms (AWS, Azure)",
							"Hồ Chí Minh", "Looking for a DevOps Engineer to manage our infrastructure and deployments.",
							Instant.now(), timestamp, timestamp, "DevOps Engineer"));

					// 6. Mobile App Developer
					jobList.add(new Job( 52000.00, "Part Time",
							"3+ years of experience in mobile development, knowledge of Android/iOS frameworks, Flutter",
							"Cần Thơ", "Seeking a Mobile App Developer to build high-quality mobile applications.",
							Instant.now(), timestamp, timestamp, "Mobile App Developer"));

					// 7. System Administrator
					jobList.add(new Job( 45000.00, "Full Time",
							"3+ years of experience in system administration, strong knowledge of Linux/Windows servers",
							"Hải Phòng", "We need a System Administrator to maintain our IT infrastructure.",
							Instant.now(), timestamp, timestamp, "System Administrator"));

					// 8. Database Administrator
					jobList.add(new Job( 50000.00, "Part Time",
							"4+ years of experience in database management, knowledge of SQL, Oracle, MySQL", "Hà Nội",
							"Hiring a Database Administrator to oversee our database systems.",
							Instant.now(), timestamp, timestamp, "Database Administrator"));

					// 9. Network Engineer
					jobList.add(new Job( 53000.00, "Full Time",
							"3+ years of experience in networking, knowledge of routers, switches, firewalls, VPNs",
							"Đà Nẵng", "Seeking a Network Engineer to manage our network infrastructure.",
							Instant.now(), timestamp, timestamp, "Network Engineer"));

					// 10. QA Engineer
					jobList.add(new Job( 48000.00, "Full Time",
							"3+ years of experience in software testing, knowledge of manual and automated testing tools",
							"Hồ Chí Minh",
							"We are looking for a QA Engineer to ensure the quality of our software products.",
							Instant.now(), timestamp, timestamp, "QA Engineer"));

					// 11. Project Manager
					jobList.add(new Job( 65000.00, "Part Time",
							"5+ years of experience in project management, knowledge of Agile methodologies", "Hà Nội",
							"Hiring a Project Manager to lead and manage our software development projects.",
							Instant.now(), timestamp, timestamp, "Project Manager"));

					// 12. Business Analyst
					jobList.add(new Job( 60000.00, "Full Time",
							"4+ years of experience in business analysis, knowledge of requirements gathering and documentation",
							"Hải Phòng", "Seeking a Business Analyst to help define and improve our business processes.",
							Instant.now(), timestamp, timestamp, "Business Analyst"));

					// 13. Content Writer
					jobList.add(new Job( 35000.00, "Part Time",
							"2+ years of experience in content writing, strong command of English and Vietnamese",
							"Cần Thơ",
							"We are looking for a Content Writer to create engaging content for our digital platforms.",
							Instant.now(), timestamp, timestamp, "Content Writer"));

					// 14. HR Specialist
					jobList.add(new Job( 45000.00, "Part Time",
							"3+ years of experience in human resources, knowledge of recruitment, employee relations",
							"Hồ Chí Minh",
							"Hiring an HR Specialist to manage our recruitment and employee engagement activities.",
							Instant.now(), timestamp, timestamp, "HR Specialist"));

					// 15. Graphic Designer
					jobList.add(new Job( 40000.00, "Part Time",
							"3+ years of experience in graphic design, proficiency in Adobe Creative Suite", "Hà Nội",
							"Looking for a Graphic Designer to create visual content for our marketing campaigns.",
							Instant.now(), timestamp, timestamp, "Graphic Designer"));

					// 16. IT Support Specialist
					jobList.add(new Job( 35000.00, "Full Time",
							"2+ years of experience in IT support, knowledge of hardware, software, troubleshooting",
							"Đà Nẵng", "We need an IT Support Specialist to assist our employees with technical issues.",
							Instant.now(), timestamp, timestamp, "IT Support Specialist"));

					// 17. Marketing Manager
					jobList.add(new Job( 60000.00, "Full Time",
							"4+ years of experience in marketing management, knowledge of digital marketing strategies",
							"Hồ Chí Minh", "Hiring a Marketing Manager to oversee our marketing campaigns and strategies.",
							Instant.now(), timestamp, timestamp, "Marketing Manager"));

					// 18. Sales Manager
					jobList.add(new Job( 62000.00, "Full Time",
							"5+ years of experience in sales management, knowledge of B2B sales and client relationship management",
							"Hà Nội", "We are looking for a Sales Manager to lead our sales team and drive revenue growth.",
							Instant.now(), timestamp, timestamp, "Sales Manager"));

					// 19. SEO Specialist
					jobList.add(new Job( 40000.00, "Part Time",
							"3+ years of experience in SEO, knowledge of on-page and off-page SEO techniques", "Đà Nẵng",
							"Seeking an SEO Specialist to improve our website's search engine ranking.",
							Instant.now(), timestamp, timestamp, "SEO Specialist"));

					// 20. Social Media Manager
					jobList.add(new Job( 45000.00, "Full Time",
							"3+ years of experience in social media management, knowledge of social media platforms and analytics tools",
							"Hồ Chí Minh", "Hiring a Social Media Manager to manage our online presence and engagement.",
							Instant.now(), timestamp, timestamp, "Social Media Manager"));

					// lưu job và set hirer
					jobList.forEach(j -> {
						jobRepository.save(j);
						j.setHirer(hirer);
					});
					
					
					
					Blog news1 = new Blog(user, "5 Essential Job Search Tips",
							"Job searching in 2024 requires a well-organized strategy, especially with the competitive job market. "
									+ "In this article, we discuss five essential tips that can help job seekers improve their chances of success. "
									+ "We dive deep into how tailoring your resume, networking effectively, and utilizing online platforms like "
									+ "LinkedIn and Indeed can open doors to opportunities. We also touch on the importance of practicing for interviews "
									+ "and following up after interviews, which can significantly improve your prospects.",
							"Job searching can be challenging, but with the right strategy, you can improve your chances of success. "
									+ "The key is to approach the process methodically and stay organized throughout. Here are five essential tips "
									+ "to help guide you in your job search this year. "
									+ "First, tailor your resume to each job you apply for. It's critical to match your skills and experience with the "
									+ "requirements listed in the job description. Use keywords from the job posting, and don't be afraid to customize "
									+ "your resume for every application. A one-size-fits-all resume might be easier, but it won’t impress potential employers. "
									+ "Second, networking is one of the most effective ways to find a job. Attend industry events, reach out to people in your "
									+ "field, and utilize online platforms like LinkedIn. Building relationships with professionals can lead to referrals and job "
									+ "leads that aren't posted publicly. Third, make sure to leverage online job search platforms like Indeed, LinkedIn, and Glassdoor. "
									+ "Create a profile on these platforms and actively engage with job postings. Many companies rely on these platforms to find new hires, "
									+ "and having a strong presence can increase your chances of being noticed. Fourth, practice for interviews. Interviewing is a skill that "
									+ "improves with practice. Research common interview questions for your field and practice answering them. Focus on highlighting your skills, "
									+ "accomplishments, and experience. Finally, always follow up after an interview. Send a thank-you email to the interviewer to show your "
									+ "appreciation and reinforce your interest in the position. It’s a small gesture that can leave a lasting impression. By implementing "
									+ "these five tips, you can improve your job search strategy and increase your chances of finding the right position.",
							120, timestamp, timestamp);

					// News 2: Career Trends for 2024
					Blog news2 = new Blog(user, "Top Career Trends in 2024",
							"In 2024, remote work and tech skills are shaping the job market like never before. This article explores "
									+ "how the global shift towards remote work is benefiting both companies and employees by increasing productivity "
									+ "and flexibility. Additionally, we highlight the importance of upskilling, as tech skills such as data analysis, "
									+ "cybersecurity, and AI are in high demand. The rise of the gig economy also offers new opportunities for freelancers "
									+ "and independent contractors, giving professionals the freedom to manage their careers on their terms.",
							"As we progress through 2024, several trends are becoming more evident in the job market. One of the major trends is the rise "
									+ "of remote work. After the COVID-19 pandemic, many companies realized that remote work is not only possible but also often beneficial. "
									+ "Remote work allows companies to tap into a global talent pool, increase productivity, and reduce overhead costs. For employees, remote work "
									+ "provides flexibility and a better work-life balance. However, this also means that job seekers need to develop strong remote working skills, such as "
									+ "self-discipline, time management, and communication via digital platforms. Another significant trend is the increasing demand for tech skills. "
									+ "With the rapid advancement of technology, roles in data science, artificial intelligence, cybersecurity, and software development are on the rise. "
									+ "Even for non-tech jobs, having a basic understanding of technology is becoming more and more important. Employers are looking for individuals who "
									+ "can leverage technology to improve efficiency and innovation. Upskilling in areas like coding, data analysis, and digital marketing can give you a "
									+ "competitive edge in the job market. Furthermore, the importance of continuous learning is more evident than ever. With the pace at which industries "
									+ "are evolving, employees need to be adaptable and willing to learn new skills throughout their careers. Platforms like Coursera, Udemy, and LinkedIn "
									+ "Learning are popular resources for professionals seeking to enhance their skills. Lifelong learning has become a key to staying relevant in the workforce. "
									+ "Moreover, the gig economy is growing, with more professionals opting for freelance or contract work rather than traditional full-time employment. "
									+ "This trend offers both freedom and flexibility but requires individuals to be proactive in managing their careers, finances, and benefits. Employers, in turn, "
									+ "are also hiring more freelancers to fulfill specialized tasks on a project basis. These career trends highlight the shifting dynamics of the modern job market, "
									+ "and understanding them can help job seekers position themselves for success.",
							98, timestamp, timestamp);

					// News 3: How to Write a Standout Resume
					Blog news3 = new Blog(user, "How to Write a Standout Resume",
							"A standout resume is key to getting noticed by potential employers in 2024. This article outlines the critical steps to "
									+ "crafting a resume that not only highlights your qualifications but also showcases your unique strengths. We emphasize "
									+ "the importance of customizing your resume for each job application and focusing on quantifiable achievements. With a well-organized "
									+ "layout, action verbs, and a strong summary at the top, your resume will catch the eye of recruiters. Proofreading and avoiding "
									+ "common mistakes are also essential to leave a positive impression.",
							"Your resume is often the first impression you make on potential employers, so it's essential to create one that stands out. "
									+ "A well-crafted resume not only highlights your qualifications but also showcases your professionalism. One of the most important aspects "
									+ "of writing a resume is tailoring it to the job you're applying for. Avoid using a generic resume for every job application. Instead, review "
									+ "the job description carefully and make sure your resume reflects the specific skills and experience that are required for the role. This can "
									+ "be as simple as adjusting your job titles, keywords, and bullet points to align with what the employer is looking for. Another key factor is "
									+ "to focus on accomplishments rather than just job duties. Use action verbs to describe how you added value to your previous positions. For example, "
									+ "instead of saying 'responsible for managing a team,' say 'led a team of 10 employees, improving productivity by 20% over one year.' These specific, "
									+ "quantifiable results show employers what you are capable of achieving. Additionally, make sure to include a strong summary at the top of your resume. "
									+ "This is your chance to provide a brief overview of your professional background and key strengths. It's also important to maintain a clean and organized "
									+ "layout. A resume that is too cluttered or difficult to read can be a turn-off for recruiters. Stick to a simple design with clear sections for your experience, "
									+ "skills, and education. Use bullet points to break up information and make it easy to scan. Lastly, proofread your resume carefully. Even a small typo can make "
									+ "a negative impression. It's a good idea to have someone else review your resume as well, to catch any mistakes you might have missed. By following these guidelines, "
									+ "you can create a resume that not only gets noticed but also sets you apart from the competition.",
							150, timestamp, timestamp);
					newsRepository.save(news1);
					newsRepository.save(news2);
					newsRepository.save(news3);
				}
			}
		};
	}
}
