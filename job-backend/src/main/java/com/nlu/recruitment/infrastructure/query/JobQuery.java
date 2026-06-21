package com.nlu.recruitment.infrastructure.query;

import com.nlu.recruitment.api.dto.AddressJobCount;
import com.nlu.recruitment.api.dto.JobCardView;
import com.nlu.recruitment.api.dto.JobResponse;
import com.nlu.recruitment.domain.vo.EmploymentType;
import com.nlu.recruitment.domain.model.QJob;
import com.nlu.recruitment.domain.model.QAddress;
import com.nlu.recruitment.domain.model.QRecruitment;
import com.nlu.identity.domain.model.QUser;
import com.nlu.applicationProcess.domain.model.QJobApplication;
import com.nlu.shared.domain.model.EntityStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JobQuery {
    private static final EntityStatus ACTIVE_STATUS = EntityStatus.ACTIVE;
    private static final QJob job = QJob.job;
    private static final QAddress address = QAddress.address;
    private static final QRecruitment hirer = QRecruitment.recruitment;
    private static final QUser user = QUser.user;
    private static final QJobApplication apply = QJobApplication.jobApplication;

    private final JPAQueryFactory queryFactory;

    public Page<JobCardView> getListJobNewest(int page, int amount) {
        Pageable pageable = PageRequest.of(page, amount, Sort.by("createdAt").descending());

        JPAQuery<JobCardView> contentQuery = queryFactory
                .select(jobCardProjection(job, address))
                .from(job)
                .leftJoin(job.address, address)
                .where(
                        job.recordStatus.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                )
                .orderBy(job.createdAt.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(job.count())
                .from(job)
                .leftJoin(job.address, address)
                .where(
                        job.recordStatus.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                );

        return fetchPage(pageable, contentQuery, countQuery);
    }

    public int getAmount() {
        QJob job = QJob.job;
        Long total = queryFactory
                .select(job.count())
                .from(job)
                .where(job.recordStatus.eq(ACTIVE_STATUS))
                .fetchOne();
        return total == null ? 0 : total.intValue();
    }

    public List<AddressJobCount> getAddressJobCount() {
        return queryFactory
                .select(Projections.constructor(AddressJobCount.class,
                        address.city.coalesce("Chưa xác định"), // Xử lý nếu city bị null do leftJoin
                        job.id.count()
                ))
                .from(job)
                .leftJoin(job.address, address)
                .where(
                        job.recordStatus.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                )
                .groupBy(address.city)
                .fetch();
    }

    public Page<JobCardView> getListJobByAddress(String city, int page, int amount) {
        Pageable pageable = PageRequest.of(page, amount, Sort.by("createdAt").descending());

        JPAQuery<JobCardView> contentQuery = queryFactory
                .select(jobCardProjection(job, address))
                .from(job)
                .leftJoin(job.address, address)
                .where(
                        job.recordStatus.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address),
                        city == null ? null : address.city.eq(city)
                )
                .orderBy(job.createdAt.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(job.count())
                .from(job)
                .leftJoin(job.address, address)
                .where(
                        job.recordStatus.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address),
                        city == null ? null : address.city.eq(city)
                );

        return fetchPage(pageable, contentQuery, countQuery);
    }

    public Page<JobCardView> findJobsBySalaryAddressAndEmploymentTypes(int pageIndex, int pageSize, int min, int max, List<String> cities, List<EmploymentType> times, String title) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("createdAt").descending());

        BooleanBuilder filterBuilder = new BooleanBuilder();
//        filterBuilder.and(job.salary.between((double) min, (double) max));
        if (cities != null && !cities.isEmpty()) {
            filterBuilder.and(address.city.in(cities));
        }
        if (times != null && !times.isEmpty()) {
            filterBuilder.and(job.time.in(times));
        }
        if (title != null) {
            var lower = title.toLowerCase();
            filterBuilder.and(job.title.lower().containsIgnoreCase(lower));
        }

        JPAQuery<JobCardView> contentQuery = queryFactory
                .select(jobCardProjection(job, address))
                .from(job)
                .leftJoin(job.address, address)
                .where(filterBuilder)
                .orderBy(job.createdAt.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(job.count())
                .from(job)
                .leftJoin(job.address, address)
                .where(filterBuilder);

        return fetchPage(pageable, contentQuery, countQuery);
    }

    public Page<JobResponse> getHirerJobPost(int pageIndex, int pageSize, String email) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("createdAt").descending());

        JPAQuery<JobResponse> contentQuery = queryFactory
                .select(com.querydsl.core.types.Projections.constructor(
                        JobResponse.class,
                        job.id,
                        job.title,
                        job.description,
                        address.city,
                        job.salary,
                        job.time,
                        apply.id.count().intValue(),
                        job.headcount,
                        job.isAnalyzed
                ))
                .from(job)
                .join(job.recruitment, hirer)
                .join(hirer.user, user)
                .leftJoin(job.address, address)
                .leftJoin(job.applies, apply).on(apply.recordStatus.eq(ACTIVE_STATUS))
                .where(
                        user.email.value.eq(email),
                        job.recordStatus.eq(ACTIVE_STATUS),
                        hirer.recordStatus.eq(ACTIVE_STATUS),
                        user.recordStatus.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                )
                .groupBy(job.id, job.title, job.description, address.city, job.salary, job.time, job.createdAt, job.headcount, job.isAnalyzed)
                .orderBy(job.createdAt.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(job.count())
                .from(job)
                .join(job.recruitment, hirer)
                .join(hirer.user, user)
                .leftJoin(job.address, address)
                .where(
                        user.email.value.eq(email),
                        job.recordStatus.eq(ACTIVE_STATUS),
                        hirer.recordStatus.eq(ACTIVE_STATUS),
                        user.recordStatus.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                );

        return fetchPage(pageable, contentQuery, countQuery);
    }

    public long countHirerJobPost(String email) {
        Long total = queryFactory
                .select(job.count())
                .from(job)
                .join(job.recruitment, hirer)
                .join(hirer.user, user)
                .leftJoin(job.address, address)
                .where(
                        user.email.value.eq(email),
                        job.recordStatus.eq(ACTIVE_STATUS),
                        hirer.recordStatus.eq(ACTIVE_STATUS),
                        user.recordStatus.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                )
                .fetchOne();

        return total == null ? 0L : total;
    }

    public Page<JobCardView> listJobUserApplied(Pageable pageable, String email) {
        JPAQuery<JobCardView> contentQuery = queryFactory
                .select(jobCardProjection(job,address))
                .from(apply)
                .join(apply.user, user)
                .join(apply.job, job)
                .leftJoin(job.address, address)
                .where(
                        user.email.value.eq(email),
                        apply.recordStatus.eq(ACTIVE_STATUS),
                        job.recordStatus.eq(ACTIVE_STATUS),
                        user.recordStatus.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                )
                .orderBy(apply.createdAt.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(apply.count())
                .from(apply)
                .join(apply.user, user)
                .join(apply.job, job)
                .leftJoin(job.address, address)
                .where(
                        user.email.value.eq(email),
                        apply.recordStatus.eq(ACTIVE_STATUS),
                        job.recordStatus.eq(ACTIVE_STATUS),
                        user.recordStatus.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                );

        return fetchPage(pageable, contentQuery, countQuery);
    }

    private ConstructorExpression<JobCardView> jobCardProjection(QJob job, QAddress address) {
        return Projections.constructor(
                JobCardView.class,
                job.id,
                job.title,
                address.city,
                job.salary,
                job.time
        );
    }

    private BooleanExpression addressIsActiveOrMissing(QAddress address) {
        return address.id.isNull().or(address.recordStatus.eq(ACTIVE_STATUS));
    }

    private <T> Page<T> fetchPage(Pageable pageable, JPAQuery<T> contentQuery, JPAQuery<Long> countQuery) {
        List<T> content = contentQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long total = countQuery.fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }
}
