package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam")
public class Exam {
    @TableId
    private Long id;
    private Long courseId;
    private String title;
    private String type;
    private String question;
    private String answer;
    private BigDecimal totalScore;
    private LocalDateTime createdAt;
}
