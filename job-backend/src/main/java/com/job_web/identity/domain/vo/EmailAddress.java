package com.job_web.identity.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.regex.Pattern;
import com.job_web.shared.domain.exception.BadRequestException;
import com.job_web.shared.utils.MessageUtils;

@Embeddable
@Getter
@EqualsAndHashCode
public class EmailAddress {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    private static final Pattern PATTERN = Pattern.compile(EMAIL_PATTERN);

    @Column(name = "email", length = 255)
    private String value;

    protected EmailAddress() {
        // JPA requires default constructor
    }

    public EmailAddress(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(MessageUtils.getMessage("validation.email.required"));
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new BadRequestException("Invalid email format");
        }
        this.value = value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
