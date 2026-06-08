package com.zadsoft.controller;

import com.zadsoft.dto.CategoryLimitDto;
import com.zadsoft.service.CategoryLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category-limits")
@RequiredArgsConstructor
public class CategoryLimitController {

    private final CategoryLimitService categoryLimitService;

    @GetMapping
    public ResponseEntity<List<CategoryLimitDto>> getAllLimits() {
        return ResponseEntity.ok(categoryLimitService.getAllLimits());
    }

    @PostMapping
    public ResponseEntity<CategoryLimitDto> saveOrUpdateLimit(@Valid @RequestBody CategoryLimitDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryLimitService.saveOrUpdateLimit(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLimit(@PathVariable Long id) {
        categoryLimitService.deleteLimit(id);
        return ResponseEntity.noContent().build();
    }
}
