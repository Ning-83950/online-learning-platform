package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_group")
public class StudentGroup {
    @TableId
    private Long id;
    private Long teacherId;
    private Long courseId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
