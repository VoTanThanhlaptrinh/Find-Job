package com.nlu.recruitment.api.dto;

import com.nlu.recruitment.domain.vo.EmploymentType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class JobJsonDto implements Serializable {
    private String jobDescription;
    private String jobRequirement;
    private String jobSkill;
    private String moreDetail;
    private String title;
    private EmploymentType time;
    private Integer yearOfExperience;
    private Long addressId;
    private Long hirerId;
    private Long imageId;
    private Integer category;
}
