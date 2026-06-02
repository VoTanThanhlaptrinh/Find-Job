package com.job_web.recruiment.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import com.job_web.shared.domain.exception.BadRequestException;

@Embeddable
@Getter
@EqualsAndHashCode
public class ExperienceYears {

    @Column(name = "year_of_experience")
    private int value;

    protected ExperienceYears() {
    }

    public ExperienceYears(int value) {
        if (value < 0) {
            throw new BadRequestException("Years of experience cannot be negative");
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
