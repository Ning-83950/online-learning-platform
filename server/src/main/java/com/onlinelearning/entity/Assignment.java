package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("assignment")
public class Assignment {
    @TableId
    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private BigDecimal totalScore;
    private LocalDateTime dueTime;
    private LocalDateTime createdAt;
}
