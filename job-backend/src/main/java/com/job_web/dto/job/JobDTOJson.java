package com.job_web.dto.job;

import com.job_web.constant.EmploymentType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class JobDTOJson implements Serializable {
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
}
