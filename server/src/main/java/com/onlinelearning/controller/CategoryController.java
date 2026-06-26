package com.onlinelearning.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.entity.Category;
import com.onlinelearning.mapper.CategoryMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    @Resource private CategoryMapper categoryMapper;

    @GetMapping("/page")
    public ApiResult<PageResult<Category>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                @RequestParam(defaultValue = "10") Long pageSize,
                                                @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Category::getName, keyword).or().like(Category::getDescription, keyword);
        }
        Page<Category> page = categoryMapper.selectPage(new Page<Category>(pageNum, pageSize), wrapper);
        return ApiResult.ok(new PageResult<Category>(page.getTotal(), pageNum, pageSize, page.getRecords()));
    }

    @GetMapping("/{id}")
    public ApiResult<Category> detail(@PathVariable Long id) {
        return ApiResult.ok(categoryMapper.selectById(id));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody Category item) {
        categoryMapper.insert(item);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody Category item) {
        categoryMapper.updateById(item);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return ApiResult.ok();
    }
}
