package com.job_web.data.queryDSL;

import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.JobApply;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobResponse;
import com.job_web.models.EntityStatus;
import com.job_web.models.Job;
import com.job_web.models.QAddress;
import com.job_web.models.QApply;
import com.job_web.models.QHirer;
import com.job_web.models.QJob;
import com.job_web.models.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JobQuery {
    private static final String ACTIVE_STATUS = EntityStatus.ACTIVE.name();

    private final JPAQueryFactory queryFactory;

    public List<JobCardView> findJobsByCity(String cityName) {
        QJob job = QJob.job;
        QAddress address = QAddress.address;

        return queryFactory
                .select(jobCardProjection(job, address))
                .from(job)
                .leftJoin(job.address, address)
                .where(
                        job.status.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address),
                        cityName == null ? null : address.city.eq(cityName)
                )
                .fetch();
    }

    public List<Job> findByTitle(String title) {
        QJob job = QJob.job;

        return queryFactory
                .selectFrom(job)
                .where(
                        job.status.eq(ACTIVE_STATUS),
                        StringUtils.hasText(title) ? job.title.containsIgnoreCase(title) : null
                )
                .fetch();
    }

    public Page<Job> findByTime(String time, Pageable pageable) {
        QJob job = QJob.job;

        JPAQuery<Job> contentQuery = queryFactory
                .selectFrom(job)
                .where(
                        job.status.eq(ACTIVE_STATUS),
                        time == null ? null : job.time.eq(time)
                )
                .orderBy(job.createDate.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(job.count())
                .from(job)
                .where(
                        job.status.eq(ACTIVE_STATUS),
                        time == null ? null : job.time.eq(time)
                );

        return fetchPage(pageable, contentQuery, countQuery);
    }

    public Page<JobCardView> getListJobNewest(int page, int amount) {
        QJob job = QJob.job;
        QAddress address = QAddress.address;
        Pageable pageable = PageRequest.of(page, amount, Sort.by("createDate").descending());

        JPAQuery<JobCardView> contentQuery = queryFactory
                .select(jobCardProjection(job, address))
                .from(job)
                .leftJoin(job.address, address)
                .where(
                        job.status.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                )
                .orderBy(job.createDate.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(job.count())
                .from(job)
                .leftJoin(job.address, address)
                .where(
                        job.status.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                );

        return fetchPage(pageable, contentQuery, countQuery);
    }

    public int getAmount() {
        QJob job = QJob.job;
        Long total = queryFactory
                .select(job.count())
                .from(job)
                .where(job.status.eq(ACTIVE_STATUS))
                .fetchOne();
        return total == null ? 0 : total.intValue();
    }

    public List<AddressJobCount> getAddressJobCount() {
        QJob job = QJob.job;
        QAddress address = QAddress.address;

        return queryFactory
                .select(Projections.constructor(AddressJobCount.class,
                        address.city.coalesce("Chưa xác định"), // Xử lý nếu city bị null do leftJoin
                        job.id.count()
                ))
                .from(job)
                .leftJoin(job.address, address)
                .where(
                        job.status.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                )
                .groupBy(address.city)
                .fetch();
    }

    public Page<JobCardView> getListJobByAddress(String city, int page, int amount) {
        QJob job = QJob.job;
        QAddress address = QAddress.address;
        Pageable pageable = PageRequest.of(page, amount, Sort.by("createDate").descending());

        JPAQuery<JobCardView> contentQuery = queryFactory
                .select(jobCardProjection(job, address))
                .from(job)
                .leftJoin(job.address, address)
                .where(
                        job.status.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address),
                        city == null ? null : address.city.eq(city)
                )
                .orderBy(job.createDate.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(job.count())
                .from(job)
                .leftJoin(job.address, address)
                .where(
                        job.status.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address),
                        city == null ? null : address.city.eq(city)
                );

        return fetchPage(pageable, contentQuery, countQuery);
    }

    public Page<JobCardView> filterBetterSalaryAndHasAddressAndInTimes(int pageIndex, int pageSize, int min, int max, List<String> cities, List<String> times, String title) {
        QJob job = QJob.job;
        QAddress address = QAddress.address;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("createDate").descending());

        BooleanBuilder filterBuilder = new BooleanBuilder();
//        filterBuilder.and(job.salary.between((double) min, (double) max));
        if (cities != null && !cities.isEmpty()) {
            filterBuilder.and(address.city.in(cities));
        }
        if (times != null && !times.isEmpty()) {
            filterBuilder.and(job.time.in(times));
        }
        if(title != null){
            var lower = title.toLowerCase();
            filterBuilder.and(job.title.lower().containsIgnoreCase(lower));
        }

        JPAQuery<JobCardView> contentQuery = queryFactory
                .select(jobCardProjection(job, address))
                .from(job)
                .leftJoin(job.address, address)
                .where(filterBuilder)
                .orderBy(job.createDate.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(job.count())
                .from(job)
                .leftJoin(job.address, address)
                .where(filterBuilder);

        return fetchPage(pageable, contentQuery, countQuery);
    }

    public Page<JobResponse> getHirerJobPost(int pageIndex, int pageSize, String email) {
        QJob job = QJob.job;
        QAddress address = QAddress.address;
        QHirer hirer = QHirer.hirer;
        QUser user = QUser.user;
        QApply apply = QApply.apply;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("createDate").descending());

        JPAQuery<JobResponse> contentQuery = queryFactory
                .select(com.querydsl.core.types.Projections.constructor(
                        JobResponse.class,
                        job.id,
                        job.title,
                        job.description,
                        address.city,
                        job.salary,
                        job.time,
                        apply.id.count().intValue()
                ))
                .from(job)
                .join(job.hirer, hirer)
                .join(hirer.user, user)
                .leftJoin(job.address, address)
                .leftJoin(job.applies, apply).on(apply.status.eq(ACTIVE_STATUS))
                .where(
                        user.email.eq(email),
                        job.status.eq(ACTIVE_STATUS),
                        hirer.status.eq(ACTIVE_STATUS),
                        user.status.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                )
                .groupBy(job.id, job.title, job.description, address.street, address.district, address.city, job.salary, job.time, job.createDate)
                .orderBy(job.createDate.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(job.count())
                .from(job)
                .join(job.hirer, hirer)
                .join(hirer.user, user)
                .leftJoin(job.address, address)
                .where(
                        user.email.eq(email),
                        job.status.eq(ACTIVE_STATUS),
                        hirer.status.eq(ACTIVE_STATUS),
                        user.status.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                );

        return fetchPage(pageable, contentQuery, countQuery);
    }

    public long countHirerJobPost(String email) {
        QJob job = QJob.job;
        QAddress address = QAddress.address;
        QHirer hirer = QHirer.hirer;
        QUser user = QUser.user;

        Long total = queryFactory
                .select(job.count())
                .from(job)
                .join(job.hirer, hirer)
                .join(hirer.user, user)
                .leftJoin(job.address, address)
                .where(
                        user.email.eq(email),
                        job.status.eq(ACTIVE_STATUS),
                        hirer.status.eq(ACTIVE_STATUS),
                        user.status.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                )
                .fetchOne();

        return total == null ? 0L : total;
    }

    public Page<JobApply> listJobUserApplied(Pageable pageable, String email) {
        QApply apply = QApply.apply;
        QJob job = QJob.job;
        QUser user = QUser.user;
        QAddress address = QAddress.address;

        JPAQuery<JobApply> contentQuery = queryFactory
                .select(com.querydsl.core.types.Projections.constructor(
                        JobApply.class,
                        job.id,
                        job.title,
                        job.description,
                        address.city,
                        job.salary,
                        job.time
                ))
                .from(apply)
                .join(apply.user, user)
                .join(apply.job, job)
                .leftJoin(job.address, address)
                .where(
                        user.email.eq(email),
                        apply.status.eq(ACTIVE_STATUS),
                        job.status.eq(ACTIVE_STATUS),
                        user.status.eq(ACTIVE_STATUS),
                        addressIsActiveOrMissing(address)
                )
                .orderBy(apply.applyDate.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(apply.count())
                .from(apply)
                .join(apply.user, user)
                .join(apply.job, job)
                .leftJoin(job.address, address)
                .where(
                        user.email.eq(email),
                        apply.status.eq(ACTIVE_STATUS),
                        job.status.eq(ACTIVE_STATUS),
                        user.status.eq(ACTIVE_STATUS),
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
        return address.id.isNull().or(address.status.eq(ACTIVE_STATUS));
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
