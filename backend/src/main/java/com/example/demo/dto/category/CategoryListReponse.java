package com.example.demo.dto.category;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryListReponse {
	private List<CategoryResponse> categories;
}
