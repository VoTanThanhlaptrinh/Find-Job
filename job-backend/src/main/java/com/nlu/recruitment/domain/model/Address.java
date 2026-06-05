package com.nlu.recruitment.domain.model;

import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.model.StatusEntity;
import com.nlu.shared.utils.MessageUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("status <> 'DELETED'")
@EntityListeners(AuditingEntityListener.class)
public class Address extends StatusEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private String street;
    @Setter
    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createDate;
    
    @Setter
    @LastModifiedDate
    @Column()
    private LocalDateTime updateDate;

    public void setCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new BadRequestException(MessageUtils.getMessage("validation.address.city.required"));
        }
        this.city = city;
    }

    public void setStreet(String street) {
        if (street == null || street.trim().isEmpty()) {
            throw new BadRequestException(MessageUtils.getMessage("validation.address.street.required"));
        }
        this.street = street;
    }
}
