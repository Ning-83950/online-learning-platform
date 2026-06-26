package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("discussion")
public class Discussion {
    @TableId
    private Long id;
    private Long courseId;
    private Long userId;
    private Long parentId;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
