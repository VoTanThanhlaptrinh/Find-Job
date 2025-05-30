package com.job_web.models;


import java.text.SimpleDateFormat;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import lombok.Data;

@Data
@Entity
public class Blog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@OneToOne
	@JoinColumn(name = "first_user_id")
	private User user;
	private String title;
	@Column(columnDefinition = "TEXT")
	private String description;
	@Column(columnDefinition = "MEDIUMTEXT")
	private String content;
	private int amountLike;
	private Instant createDate;
	private Instant modifiedDate;
	
	public Blog(User user, String title, String description, String content, int amountLike, Instant createDate,
			Instant modifiedDate) {
		super();
		this.user = user;
		this.title = title;
		this.description = description;
		this.content = content;
		this.amountLike = amountLike;
		this.createDate = createDate;
		this.modifiedDate = modifiedDate;
	}
	public Blog() {
		super();
	}
//	public String getTime() {
//		SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		StringBuilder builder = new StringBuilder();
//		builder.append(sdf3.format(createDate));
//		return builder.toString();
//	}
}
