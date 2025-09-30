package com.job_web.data;

import com.job_web.dto.CVDTO;
import com.job_web.models.CV;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CVRepository extends CrudRepository<CV, Long> {
    @Query(value = """
            select c.id as id, c.fileName as fileName, c.create_date as createDate
            from cv as c
            join user as u on c.user_id = u.id
            where u.email = ?1
           """, nativeQuery = true)
    List<CVDTO> findAllByUser(String email);
}
