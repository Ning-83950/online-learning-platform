package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_review")
public class Review {
    @TableId
    private Long id;
    private Long courseId;
    private Long studentId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
