package com.job_web.data.queryDSL;

import com.job_web.dto.admin.job.AdminJobListItem;
import com.job_web.models.EntityStatus;
import com.job_web.models.QAddress;
import com.job_web.models.QApply;
import com.job_web.models.QHirer;
import com.job_web.models.QJob;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminJobQuery {
    private final JPAQueryFactory queryFactory;

    public Page<AdminJobListItem> findJobs(String search, String category, String status, Pageable pageable) {
        QJob job = QJob.job;
        QHirer hirer = QHirer.hirer;
        QAddress address = QAddress.address;
        QApply apply = QApply.apply;

        BooleanBuilder where = new BooleanBuilder();
        where.and(job.status.ne(EntityStatus.DELETED.name()));

        if (StringUtils.hasText(search)) {
            where.and(job.title.containsIgnoreCase(search)
                    .or(hirer.companyName.containsIgnoreCase(search)));
        }

        if (StringUtils.hasText(status)) {
            where.and(job.status.eq(status.toUpperCase()));
        }

        JPAQuery<com.querydsl.core.Tuple> query = queryFactory
                .select(
                        job.id,
                        job.title,
                        hirer.companyName,
                        address.city,
                        job.status,
                        job.expiredDate,
                        job.applies.size()
                )
                .from(job)
                .leftJoin(job.hirer, hirer)
                .leftJoin(job.address, address)
                .where(where)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        JPAQuery<Long> countQuery = queryFactory
                .select(job.count())
                .from(job)
                .leftJoin(job.hirer, hirer)
                .where(where);

        List<com.querydsl.core.Tuple> tuples = query.fetch();
        Long total = countQuery.fetchOne();

        List<AdminJobListItem> content = tuples.stream()
                .map(tuple -> AdminJobListItem.builder()
                        .id(String.valueOf(tuple.get(job.id)))
                        .title(tuple.get(job.title))
                        .company(tuple.get(hirer.companyName))
                        .location(tuple.get(address.city))
                        .category(category != null ? category : "General") // Mock
                        .applications(tuple.get(job.applies.size()))
                        .newApplicationsToday(0) // Mock
                        .status(tuple.get(job.status))
                        .expiryDate(tuple.get(job.expiredDate))
                        .build())
                .toList();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }
}
