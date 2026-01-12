package com.example.demo.resource;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.category.CategoryResponse;
import com.example.demo.service.CategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

	private final CategoryService categoryService;

	@GetMapping
	public ResponseEntity<List<CategoryResponse>> getCategories(
			@RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly) {
		return ResponseEntity.ok(categoryService.getCategories(activeOnly));
	}

	@PostMapping("/opentdb/sync")
	public ResponseEntity<List<CategoryResponse>> syncOpenTriviaCategories() {
		return ResponseEntity.ok(categoryService.syncWithOpenTrivia());
	}
}
