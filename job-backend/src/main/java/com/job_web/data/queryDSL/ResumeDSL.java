package com.job_web.data.queryDSL;

import com.job_web.dto.application.ResumeView;
import com.job_web.models.EntityStatus;
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
        return queryFactory.select(Projections.constructor(ResumeView.class, resume.id, resume.fileName, resume.createDate)).from(resume).where(resume.user.email.eq(email)).orderBy(resume.createDate.desc()).fetch();
    }
}
