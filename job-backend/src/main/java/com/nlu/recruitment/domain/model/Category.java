package com.nlu.recruitment.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nlu.shared.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.LinkedList;
import java.util.List;

@Entity
@Getter
@Table(name = "category")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("record_status <> 'DELETED'")
public class Category extends BaseEntity {

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String name;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Category> subCategories;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Job> jobs;

    public void addSubCategory(Category subCategory) {
        if (subCategories == null) {
            subCategories = new LinkedList<>();
        }
        subCategory.setParentCategory(this);
        subCategories.add(subCategory);
    }

    public void addJob(Job job) {
        if (jobs == null) {
            jobs = new LinkedList<>();
        }
        job.setCategory(this);
        jobs.add(job);
    }
}
