package com.nlu.content.domain.model;




import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.model.StatusEntity;
import com.nlu.shared.utils.MessageUtils;
import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("status <> 'DELETED'")
public class Blog extends StatusEntity {
	@Id
	@Setter
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JsonIgnore
	@JoinColumn(nullable = false)
	private User author;
	private String title;
	@Column(columnDefinition = "TEXT")
	private String description;
	@Column(columnDefinition = "TEXT")
	private String content;
	@Setter
	private int amountLike;

	@OneToMany(fetch = FetchType.LAZY)
	@JsonIgnore
	@Column(nullable = true)
	private List<Comment> comments;

	@OneToMany(fetch = FetchType.LAZY)
	@JsonIgnore
	@Column(nullable = true)
	private List<Comment> likes;

	@Setter
	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createDate;

	@LastModifiedDate
	@Column(insertable = false)
	private LocalDateTime lastModifiedDate;

	public String getTime() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
				.withZone(ZoneId.systemDefault());
		return formatter.format(createDate);
	}

	public void setAuthor(User user) {
		if(user == null){
			throw new BadRequestException("user is null");
		}
		this.author = user;
	}

	public void setTitle(String title) {
		if (title == null || title.trim().isEmpty()) {
			throw new BadRequestException(MessageUtils.getMessage("validation.blog.title.required"));
		}
		this.title = title;
	}

	public void setDescription(String description) {
		if (description == null || description.trim().isEmpty()) {
			throw new BadRequestException(MessageUtils.getMessage("validation.blog.description.required"));
		}
		this.description = description;
	}

	public void setContent(String content) {
		if (content == null || content.trim().isEmpty()) {
			throw new BadRequestException(MessageUtils.getMessage("validation.blog.content.required"));
		}
		this.content = content;
	}
}


