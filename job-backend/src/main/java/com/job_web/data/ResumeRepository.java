package com.job_web.data;

import com.job_web.dto.application.ResumeDTO;
import com.job_web.models.Resume;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends CrudRepository<Resume, Long> {
    @Query(value = """
            select c.id as id, c.fileName as fileName, c.create_date as createDate
            from resume as c
            join user as u on c.user_id = u.id
            where u.email = ?1
           """, nativeQuery = true)
    List<ResumeDTO> findAllByUser(String email);
}


