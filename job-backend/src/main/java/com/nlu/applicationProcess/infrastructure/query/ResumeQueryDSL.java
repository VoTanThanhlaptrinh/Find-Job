package com.nlu.applicationProcess.infrastructure.query;

import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.applicationProcess.domain.model.QResume;
import com.nlu.shared.domain.model.EntityStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ResumeQueryDSL {
    private static final String ACTIVE_STATUS = EntityStatus.ACTIVE.name();
    private final JPAQueryFactory queryFactory;

    public List<ResumeView> getListResumeOfUser(String email) {
        QResume resume = QResume.resume;
        return queryFactory.select(Projections.constructor(ResumeView.class, resume.id, resume.fileName, resume.createdAt, resume.isAnalyzed)).from(resume).where(resume.user.email.value.eq(email)).orderBy(resume.createdAt.desc()).fetch();
    }

    public List<ResumeView> getAnalyzedResumesOfUser(String email) {
        QResume resume = QResume.resume;
        return queryFactory.select(Projections.constructor(ResumeView.class, resume.id, resume.fileName, resume.createdAt, resume.isAnalyzed))
                .from(resume)
                .where(resume.user.email.value.eq(email).and(resume.isAnalyzed.isTrue()))
                .orderBy(resume.createdAt.desc())
                .fetch();
    }
}
