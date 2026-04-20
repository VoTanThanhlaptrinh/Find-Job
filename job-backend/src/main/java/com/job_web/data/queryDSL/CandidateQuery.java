package com.job_web.data.queryDSL;

import com.job_web.dto.admin.seeker.JobSeekerListItem;
import com.job_web.models.EntityStatus;
import com.job_web.models.QCandidate;
import com.job_web.models.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CandidateQuery {
    private final JPAQueryFactory queryFactory;

    public Page<JobSeekerListItem> findJobSeekers(String search, String resumeStatus, Pageable pageable) {
        QCandidate candidate = QCandidate.candidate;
        QUser user = QUser.user;

        BooleanBuilder where = new BooleanBuilder();
        where.and(candidate.status.ne(EntityStatus.DELETED.name()));

        if (StringUtils.hasText(search)) {
            where.and(candidate.fullName.containsIgnoreCase(search)
                    .or(candidate.email.containsIgnoreCase(search)));
        }

        // Mapping to DTO
        JPAQuery<com.querydsl.core.Tuple> query = queryFactory
                .select(
                        candidate.id,
                        candidate.fullName,
                        candidate.email,
                        candidate.modifiedDate,
                        candidate.status
                )
                .from(candidate)
                .where(where)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        JPAQuery<Long> countQuery = queryFactory
                .select(candidate.count())
                .from(candidate)
                .where(where);

        List<com.querydsl.core.Tuple> tuples = query.fetch();
        Long total = countQuery.fetchOne();

        List<JobSeekerListItem> content = tuples.stream()
                .map(tuple -> JobSeekerListItem.builder()
                        .id("cand_" + tuple.get(candidate.id))
                        .fullName(tuple.get(candidate.fullName))
                        .email(tuple.get(candidate.email))
                        .profession("Job Seeker") // Mock
                        .resumeStatus(resumeStatus != null ? resumeStatus : "available")
                        .lastActiveAt(tuple.get(candidate.modifiedDate) != null ? 
                                tuple.get(candidate.modifiedDate).toLocalDateTime() : LocalDateTime.now())
                        .avatarInitials(getInitials(tuple.get(candidate.fullName)))
                        .build())
                .toList();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private String getInitials(String name) {
        if (!StringUtils.hasText(name)) return "??";
        String[] parts = name.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
