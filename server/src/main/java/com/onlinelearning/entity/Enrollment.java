package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("enrollment")
public class Enrollment {
    @TableId
    private Long id;
    private Long studentId;
    private Long courseId;
    private BigDecimal progress;
    private Integer learnMinutes;
    private String status;
    private LocalDateTime lastStudyTime;
    private LocalDateTime createdAt;
}
