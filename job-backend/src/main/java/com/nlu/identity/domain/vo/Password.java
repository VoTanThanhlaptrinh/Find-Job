package com.nlu.identity.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.utils.MessageUtils;

@Embeddable
@Getter
@EqualsAndHashCode
public class Password {

    @Column(name = "password", length = 255)
    private String value;

    protected Password() {
    }

    public Password(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(MessageUtils.getMessage("validation.password.required"));
        }
        if (value.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return "[PROTECTED]";
    }
}
