package com.onlinelearning.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("student_group_member")
public class StudentGroupMember {
    @TableId
    private Long id;
    private Long groupId;
    private Long studentId;
}
