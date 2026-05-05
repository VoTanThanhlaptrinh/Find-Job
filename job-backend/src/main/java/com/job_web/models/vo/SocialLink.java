package com.job_web.models.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.net.URL;
import java.net.MalformedURLException;
import com.job_web.exception.BadRequestException;

@Embeddable
@Getter
@EqualsAndHashCode
public class SocialLink {

    @Column(name = "social_link", columnDefinition = "TEXT")
    private String value;

    protected SocialLink() {
    }

    public SocialLink(String value) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                URL url = new URL(value);
                if (!"https".equalsIgnoreCase(url.getProtocol())) {
                    throw new BadRequestException("Social link must use HTTPS protocol");
                }
            } catch (MalformedURLException e) {
                throw new BadRequestException("Invalid URL format");
            }
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
