package com.onlinelearning.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.entity.Review;
import com.onlinelearning.mapper.ReviewMapper;
import com.onlinelearning.service.LookupService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    @Resource private ReviewMapper reviewMapper;
    @Resource private LookupService lookupService;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) Long courseId,
                                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>().orderByDesc(Review::getCreatedAt);
        if (courseId != null) wrapper.eq(Review::getCourseId, courseId);
        if (StringUtils.hasText(keyword)) wrapper.like(Review::getContent, keyword);
        Page<Review> page = reviewMapper.selectPage(new Page<Review>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = toRecords(page.getRecords());
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) Long courseId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>().orderByDesc(Review::getCreatedAt);
        if (courseId != null) wrapper.eq(Review::getCourseId, courseId);
        return ApiResult.ok(toRecords(reviewMapper.selectList(wrapper)));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody Review item) {
        item.setStudentId(StpUtil.getLoginIdAsLong());
        item.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(item);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody Review item) {
        reviewMapper.updateById(item);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        reviewMapper.deleteById(id);
        return ApiResult.ok();
    }

    private List<Map<String, Object>> toRecords(List<Review> rows) {
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (Review item : rows) {
            Map<String, Object> map = lookupService.names(item.getCourseId(), item.getStudentId(), null, null);
            map.put("id", item.getId());
            map.put("studentId", item.getStudentId());
            map.put("courseId", item.getCourseId());
            map.put("rating", item.getRating());
            map.put("content", item.getContent());
            map.put("createdAt", item.getCreatedAt());
            records.add(map);
        }
        return records;
    }
}
