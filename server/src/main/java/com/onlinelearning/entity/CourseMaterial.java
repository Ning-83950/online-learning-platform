package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_material")
public class CourseMaterial {
    @TableId
    private Long id;
    private Long courseId;
    private String type;
    private String name;
    private String fileUrl;
    private Integer sort;
    private LocalDateTime createdAt;
}
