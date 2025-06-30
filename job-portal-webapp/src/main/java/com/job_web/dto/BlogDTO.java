package com.job_web.dto;

import com.job_web.models.Blog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BlogDTO {

    public Blog toBlog(){
        return new Blog();
    }
}
