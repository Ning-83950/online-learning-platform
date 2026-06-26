package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_record")
public class ExamRecord {
    @TableId
    private Long id;
    private Long examId;
    private Long studentId;
    private String answer;
    private BigDecimal score;
    private String status;
    private LocalDateTime submittedAt;
}
