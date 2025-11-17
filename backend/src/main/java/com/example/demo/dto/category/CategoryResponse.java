package com.example.demo.dto.category;

import java.time.LocalDateTime;

import com.example.demo.domain.Category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {

	private Long id;
	private String name;
	private String description;
	private String icon;
	private Boolean isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public static CategoryResponse fromEntity(Category category) {
		if (category == null) {
			return null;
		}
		return CategoryResponse.builder()
				.id(category.getId())
				.name(category.getName())
				.description(category.getDescription())
				.icon(category.getIcon())
				.isActive(category.getIsActive())
				.createdAt(category.getCreatedAt())
				.updatedAt(category.getUpdatedAt())
				.build();
	}
}
