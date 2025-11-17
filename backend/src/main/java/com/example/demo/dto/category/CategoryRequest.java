package com.example.demo.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

	@NotBlank(message = "Name is required")
	@Size(max = 100, message = "Name must be at most 100 characters")
	private String name;

	@Size(max = 255, message = "Description must be at most 255 characters")
	private String description;

	@Size(max = 100, message = "Icon must be at most 100 characters")
	private String icon;

	private Boolean isActive;
}
