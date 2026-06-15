package com.nlu.recruitment.api.dto;

import com.nlu.recruitment.domain.vo.EmploymentType; // Import thêm dòng này
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JobResponse {
    private final Long id;
    private final String title;
    private final String description;
    private final String address;
    private final String salary;
    private final EmploymentType time; // Đổi từ String -> EmploymentType
    private final Integer applies;     // Đổi từ int -> Integer
    private final Integer headcount;   // Đổi từ int -> Integer
}