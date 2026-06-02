package com.job_web.application_process.infrastructure.query;

import com.job_web.application_process.api.dto.ResumeView;
import com.job_web.shared.domain.model.EntityStatus;
import com.job_web.models.QResume;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ResumeDSL {
    private static final String ACTIVE_STATUS = EntityStatus.ACTIVE.name();
    private final JPAQueryFactory queryFactory;

    public List<ResumeView> getListResumeOfUser(String email) {
        QResume resume = QResume.resume;
        return queryFactory.select(Projections.constructor(ResumeView.class, resume.id, resume.fileName, resume.createDate)).from(resume).where(resume.user.email.value.eq(email)).orderBy(resume.createDate.desc()).fetch();
    }
}
