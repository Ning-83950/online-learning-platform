package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("assignment_submission")
public class Submission {
    @TableId
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String answer;
    private String fileUrl;
    private BigDecimal score;
    private String status;
    private String comment;
    private LocalDateTime submittedAt;
    private LocalDateTime gradedAt;
}
