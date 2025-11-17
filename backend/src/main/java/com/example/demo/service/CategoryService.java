package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Category;
import com.example.demo.dto.category.CategoryRequest;
import com.example.demo.dto.category.CategoryResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.external.opentrivia.OpenTriviaApiClient;
import com.example.demo.dto.question.OpenTriviaCategoryResponse;
import com.example.demo.repo.CategoryRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

	private final CategoryRepo categoryRepo;
	private final OpenTriviaApiClient openTriviaApiClient;

	@Transactional(readOnly = true)
	public List<CategoryResponse> getCategories(boolean onlyActive) {
		List<Category> categories = onlyActive
				? categoryRepo.findByIsActiveTrueOrderByNameAsc()
				: categoryRepo.findAll(Sort.by(Sort.Direction.ASC, "name"));

		return categories.stream()
				.map(CategoryResponse::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse getCategory(Long id) {
		return CategoryResponse.fromEntity(getCategoryEntity(id));
	}

	public CategoryResponse createCategory(CategoryRequest request) {
		categoryRepo.findByNameIgnoreCase(request.getName())
				.ifPresent(existing -> {
					throw new IllegalArgumentException("Category with this name already exists");
				});

		Category category = new Category();
		category.setName(request.getName());
		category.setDescription(request.getDescription());
		category.setIcon(request.getIcon());
		category.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE);

		Category saved = categoryRepo.save(category);
		return CategoryResponse.fromEntity(saved);
	}

	public CategoryResponse updateCategory(Long id, CategoryRequest request) {
		Category category = getCategoryEntity(id);

		if (!category.getName().equalsIgnoreCase(request.getName())
				&& categoryRepo.existsByNameIgnoreCase(request.getName())) {
			throw new IllegalArgumentException("Category with this name already exists");
		}

		category.setName(request.getName());
		category.setDescription(request.getDescription());
		category.setIcon(request.getIcon());
		if (request.getIsActive() != null) {
			category.setIsActive(request.getIsActive());
		}

		return CategoryResponse.fromEntity(category);
	}

	public void deleteCategory(Long id) {
		Category category = getCategoryEntity(id);
		category.setIsActive(false);
	}

	public Category getCategoryEntity(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Category id is required");
		}
		return categoryRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category", id));
	}

	public List<CategoryResponse> syncWithOpenTrivia() {
		List<OpenTriviaCategoryResponse> externalCategories = openTriviaApiClient.fetchCategories();
		if (externalCategories.isEmpty()) {
			return getCategories(true);
		}

		List<Category> newCategories = new ArrayList<>();
		externalCategories.forEach(external -> {
			boolean exists = categoryRepo.existsByNameIgnoreCase(external.name());
			if (!exists) {
				Category category = new Category();
				category.setName(external.name());
				category.setDescription("OpenTrivia ID " + external.id());
				category.setIsActive(true);
				newCategories.add(category);
			}
		});

		if (!newCategories.isEmpty()) {
			categoryRepo.saveAll(newCategories);
		}

		return categoryRepo.findByIsActiveTrueOrderByNameAsc().stream()
				.map(CategoryResponse::fromEntity)
				.toList();
	}
}
