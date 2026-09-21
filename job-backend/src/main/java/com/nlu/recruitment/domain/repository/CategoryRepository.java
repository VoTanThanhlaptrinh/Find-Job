package com.nlu.recruitment.domain.repository;

import com.nlu.recruitment.domain.model.Category;
import com.nlu.shared.domain.model.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByRecordStatus(EntityStatus recordStatus);
}
