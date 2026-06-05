package com.nlu.shared.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.time.LocalDateTime;
import com.nlu.shared.domain.exception.BadRequestException;

@Embeddable
@Getter
@EqualsAndHashCode
public class DateRange {

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    protected DateRange() {
    }

    public DateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must be after start date");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate);
    }
}
