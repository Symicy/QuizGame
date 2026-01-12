package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Category;
import com.example.demo.dto.category.CategoryResponse;
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
