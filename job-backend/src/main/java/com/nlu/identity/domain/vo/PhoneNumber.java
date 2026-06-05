package com.nlu.identity.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.regex.Pattern;
import com.nlu.shared.domain.exception.BadRequestException;

@Embeddable
@Getter
@EqualsAndHashCode
public class PhoneNumber {

    private static final Pattern PATTERN = Pattern.compile("^\\d{10,11}$");

    @Column(name = "phone_number", length = 20)
    private String value;

    protected PhoneNumber() {
    }

    public PhoneNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException("Phone number is required");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new BadRequestException("Invalid phone number format");
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
