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
	public String getTime() {
		SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		StringBuilder builder = new StringBuilder();
		builder.append(sdf3.format(createDate));
		return builder.toString();
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public int getAmountLike() {
		return amountLike;
	}
	public void setAmountLike(int amountLike) {
		this.amountLike = amountLike;
	}
	public Instant getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Instant createDate) {
		this.createDate = createDate;
	}
	public Instant getModifiedDate() {
		return modifiedDate;
	}
	public void setModifiedDate(Instant modifiedDate) {
		this.modifiedDate = modifiedDate;
	}
	
}
