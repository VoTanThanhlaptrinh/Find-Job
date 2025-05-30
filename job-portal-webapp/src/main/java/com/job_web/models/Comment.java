package com.job_web.models;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Comment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private long user_id;
	private long news_id;
	private Timestamp createDate;
	private Timestamp modifiedDate;
	public Comment(long id, long user_id, long news_id, Timestamp createDate, Timestamp modifiedDate) {
		super();
		this.id = id;
		this.user_id = user_id;
		this.news_id = news_id;
		this.createDate = createDate;
		this.modifiedDate = modifiedDate;
	}
	public Comment() {
		super();
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getUser_id() {
		return user_id;
	}
	public void setUser_id(long user_id) {
		this.user_id = user_id;
	}
	public long getNews_id() {
		return news_id;
	}
	public void setNews_id(long news_id) {
		this.news_id = news_id;
	}
	public Timestamp getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}
	public Timestamp getModifiedDate() {
		return modifiedDate;
	}
	public void setModifiedDate(Timestamp modifiedDate) {
		this.modifiedDate = modifiedDate;
	}
	
}
