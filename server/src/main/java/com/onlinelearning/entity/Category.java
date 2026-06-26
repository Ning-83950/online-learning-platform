package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_category")
public class Category {
    @TableId
    private Long id;
    private String name;
    private String description;
    private Integer sort;
}
