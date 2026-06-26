package com.onlinelearning.common;

import java.util.HashMap;
import java.util.Map;

public class StatusText {
    private static final Map<String, String> TEXT = new HashMap<String, String>();

    static {
        TEXT.put("ACTIVE", "正常");
        TEXT.put("BANNED", "已封禁");
        TEXT.put("PENDING", "待审核");
        TEXT.put("APPROVED", "已通过");
        TEXT.put("REJECTED", "已驳回");
        TEXT.put("DRAFT", "草稿");
        TEXT.put("PUBLISHED", "已发布");
        TEXT.put("LEARNING", "学习中");
        TEXT.put("FINISHED", "已完成");
        TEXT.put("SUBMITTED", "已提交");
        TEXT.put("GRADED", "已批改");
        TEXT.put("OPEN", "未回复");
        TEXT.put("REPLIED", "已回复");
        TEXT.put("CLOSED", "已关闭");
        TEXT.put("OBJECTIVE", "客观题");
        TEXT.put("SUBJECTIVE", "主观题");
        TEXT.put("VIDEO", "视频");
        TEXT.put("DOCUMENT", "文档");
    }

    public static String of(String status) {
        if (status == null) {
            return "";
        }
        return TEXT.containsKey(status) ? TEXT.get(status) : status;
    }
}
