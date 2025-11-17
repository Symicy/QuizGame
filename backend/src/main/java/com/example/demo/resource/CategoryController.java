package com.example.demo.resource;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.category.CategoryRequest;
import com.example.demo.dto.category.CategoryResponse;
import com.example.demo.service.CategoryService;

import jakarta.validation.Valid;
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

	@GetMapping("/{id}")
	public ResponseEntity<CategoryResponse> getCategory(@PathVariable Long id) {
		return ResponseEntity.ok(categoryService.getCategory(id));
	}

	@PostMapping
	public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
		return ResponseEntity.ok(categoryService.createCategory(request));
	}

	@PostMapping("/opentdb/sync")
	public ResponseEntity<List<CategoryResponse>> syncOpenTriviaCategories() {
		return ResponseEntity.ok(categoryService.syncWithOpenTrivia());
	}

	@PutMapping("/{id}")
	public ResponseEntity<CategoryResponse> updateCategory(
			@PathVariable Long id,
			@Valid @RequestBody CategoryRequest request) {
		return ResponseEntity.ok(categoryService.updateCategory(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
		categoryService.deleteCategory(id);
		return ResponseEntity.noContent().build();
	}
}
