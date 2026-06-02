package com.job_web.content.domain.model;

import com.job_web.identity.domain.model.User;
import com.job_web.shared.domain.exception.BadRequestException;
import com.job_web.shared.domain.model.StatusEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@lombok.EqualsAndHashCode(callSuper = true)
@Table(name = "user_like")
@SQLRestriction("status <> 'DELETED'")
public class Like extends StatusEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn( nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Blog blog;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;

    public void setUser(User user) {
        if(user == null){
            throw new BadRequestException("user is null");
        }
        this.user = user;
    }

    public void setBlog(Blog blog) {
        if(blog == null){
            throw new BadRequestException("user is null");
        }
        this.blog = blog;
    }
}


