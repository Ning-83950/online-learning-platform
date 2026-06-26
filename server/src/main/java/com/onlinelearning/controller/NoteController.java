package com.onlinelearning.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.entity.Note;
import com.onlinelearning.mapper.NoteMapper;
import com.onlinelearning.service.LookupService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    @Resource private NoteMapper noteMapper;
    @Resource private LookupService lookupService;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) Long courseId,
                                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getStudentId, StpUtil.getLoginIdAsLong())
                .orderByDesc(Note::getCreatedAt);
        if (courseId != null) wrapper.eq(Note::getCourseId, courseId);
        if (StringUtils.hasText(keyword)) wrapper.like(Note::getContent, keyword);
        Page<Note> page = noteMapper.selectPage(new Page<Note>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (Note item : page.getRecords()) {
            Map<String, Object> map = lookupService.names(item.getCourseId(), item.getStudentId(), null, null);
            map.put("id", item.getId());
            map.put("studentId", item.getStudentId());
            map.put("courseId", item.getCourseId());
            map.put("content", item.getContent());
            map.put("createdAt", item.getCreatedAt());
            records.add(map);
        }
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody Note item) {
        item.setStudentId(StpUtil.getLoginIdAsLong());
        item.setCreatedAt(LocalDateTime.now());
        noteMapper.insert(item);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody Note item) {
        item.setStudentId(StpUtil.getLoginIdAsLong());
        noteMapper.updateById(item);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        noteMapper.deleteById(id);
        return ApiResult.ok();
    }
}
