package com.zadsoft.service;

import com.zadsoft.dto.CategoryLimitDto;
import com.zadsoft.exception.ResourceNotFoundException;
import com.zadsoft.model.CategoryLimit;
import com.zadsoft.repository.CategoryLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryLimitService {

    private final CategoryLimitRepository categoryLimitRepository;

    @Transactional(readOnly = true)
    public List<CategoryLimitDto> getAllLimits() {
        return categoryLimitRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryLimitDto saveOrUpdateLimit(CategoryLimitDto dto) {
        Optional<CategoryLimit> existingLimit = categoryLimitRepository.findByCategoryIgnoreCase(dto.getCategory().trim());
        CategoryLimit limit;
        if (existingLimit.isPresent()) {
            limit = existingLimit.get();
            limit.setMonthlyLimit(dto.getMonthlyLimit());
        } else {
            limit = CategoryLimit.builder()
                    .category(dto.getCategory().trim())
                    .monthlyLimit(dto.getMonthlyLimit())
                    .build();
        }
        CategoryLimit saved = categoryLimitRepository.save(limit);
        return convertToDto(saved);
    }

    @Transactional
    public void deleteLimit(Long id) {
        CategoryLimit limit = categoryLimitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Limit o podanym ID nie istnieje: " + id));
        categoryLimitRepository.delete(limit);
    }

    private CategoryLimitDto convertToDto(CategoryLimit limit) {
        return CategoryLimitDto.builder()
                .id(limit.getId())
                .category(limit.getCategory())
                .monthlyLimit(limit.getMonthlyLimit())
                .build();
    }
}
