package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("study_note")
public class Note {
    @TableId
    private Long id;
    private Long studentId;
    private Long courseId;
    private String content;
    private LocalDateTime createdAt;
}
