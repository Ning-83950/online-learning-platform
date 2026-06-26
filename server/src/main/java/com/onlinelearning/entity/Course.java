package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {
    @TableId
    private Long id;
    private String title;
    private Long categoryId;
    private Long teacherId;
    private String level;
    private String status;
    private String cover;
    private String videoUrl;
    private String docUrl;
    private String description;
    private String objective;
    private String outline;
    private Integer hotScore;
    private Integer enrollCount;
    private Integer durationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
