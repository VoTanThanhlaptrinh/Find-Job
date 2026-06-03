package com.job_web.admin.infrastructure.query;

import com.job_web.admin.api.dto.employer.EmployerListItem;
import com.job_web.identity.domain.model.QUser;
import com.job_web.recruiment.domain.model.QJob;
import com.job_web.recruiment.domain.model.QRecruitment;
import com.job_web.shared.domain.model.EntityStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class RecruitmentQuery {
    private final JPAQueryFactory queryFactory;

    public Page<EmployerListItem> findEmployers(String search, String kycStatus, String status, Pageable pageable) {
        QRecruitment hirer = QRecruitment.recruitment;
        QUser user = QUser.user;
        QJob job = QJob.job;

        BooleanBuilder where = new BooleanBuilder();
        where.and(hirer.status.ne(EntityStatus.DELETED.name()));

        if (StringUtils.hasText(search)) {
            where.and(hirer.companyName.containsIgnoreCase(search)
                    .or(user.email.value.containsIgnoreCase(search)));
        }

        if (StringUtils.hasText(status)) {
            where.and(hirer.status.eq(status.toUpperCase()));
        }

        JPAQuery<com.querydsl.core.Tuple> query = queryFactory
                .select(
                        hirer.id,
                        hirer.companyName,
                        hirer.createDate,
                        job.id.count(),
                        hirer.status
                )
                .from(hirer)
                .join(hirer.user, user)
                .leftJoin(hirer.jobsPost, job)
                .where(where)
                .groupBy(hirer.id, hirer.companyName, hirer.createDate, hirer.status)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        JPAQuery<Long> countQuery = queryFactory
                .select(hirer.count())
                .from(hirer)
                .join(hirer.user, user)
                .where(where);

        List<com.querydsl.core.Tuple> tuples = query.fetch();
        Long total = countQuery.fetchOne();

        List<EmployerListItem> content = tuples.stream()
                .map(tuple -> EmployerListItem.builder()
                        .id(String.valueOf(tuple.get(hirer.id)))
                        .name(tuple.get(hirer.companyName))
                        .industry("Technology") // Mock
                        .registrationDate(LocalDate.from(Objects.requireNonNull(tuple.get(hirer.createDate))))
                        .activeJobs(Objects.requireNonNull(tuple.get(job.id.count())).intValue())
                        .kycStatus(kycStatus != null ? kycStatus : "verified")
                        .accountStatus(tuple.get(hirer.status))
                        .avatarInitials(getInitials(tuple.get(hirer.companyName)))
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
