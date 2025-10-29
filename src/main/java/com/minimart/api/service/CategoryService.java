package com.minimart.api.service;

import com.minimart.api.model.Category;
import com.minimart.api.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // Get all categories
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Get all categories ordered by name
    public List<Category> getAllCategoriesOrderedByName() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    // Get all categories ordered by creation date
    public List<Category> getAllCategoriesOrderedByDate() {
        return categoryRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get category by ID
    public Optional<Category> getCategoryById(Integer id) {
        return categoryRepository.findById(id);
    }

    // Get category by name
    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    // Search categories by keyword
    public List<Category> searchCategories(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCategories();
        }
        return categoryRepository.searchCategories(keyword.trim());
    }

    // Create new category
    public Category createCategory(Category category) {
        // Check if category name already exists
        if (categoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("Category with name '" + category.getName() + "' already exists");
        }
        return categoryRepository.save(category);
    }

    // Update category
    public Category updateCategory(Integer id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        // Check if new name conflicts with existing category (excluding current one)
        if (!category.getName().equals(categoryDetails.getName())) {
            if (categoryRepository.existsByName(categoryDetails.getName())) {
                throw new RuntimeException("Category with name '" + categoryDetails.getName() + "' already exists");
            }
        }

        // 🗑️ DELETE OLD IMAGE FROM CLOUDINARY if image changed
        String oldImage = category.getImage();
        String newImage = categoryDetails.getImage();

        if (oldImage != null && !oldImage.isEmpty() &&
                newImage != null && !newImage.equals(oldImage)) {
            try {
                fileStorageService.deleteFile(oldImage);
                System.out.println("✅ Old category image deleted from Cloudinary: " + oldImage);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to delete old category image: " + e.getMessage());
            }
        }

        category.setName(categoryDetails.getName());
        category.setImage(categoryDetails.getImage());

        return categoryRepository.save(category);
    }

    // Delete category
    public void deleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        // 🗑️ DELETE IMAGE FROM CLOUDINARY
        if (category.getImage() != null && !category.getImage().isEmpty()) {
            try {
                fileStorageService.deleteFile(category.getImage());
                System.out.println("✅ Category image deleted from Cloudinary: " + category.getImage());
            } catch (Exception e) {
                System.err.println("⚠️ Failed to delete category image: " + e.getMessage());
                // Continue with category deletion even if image delete fails
            }
        }

        categoryRepository.delete(category);
    }

    // Check if category exists
    public boolean categoryExists(Integer id) {
        return categoryRepository.existsById(id);
    }

    // Check if category name exists
    public boolean categoryNameExists(String name) {
        return categoryRepository.existsByName(name);
    }

    // Get total count of categories
    public long getCategoryCount() {
        return categoryRepository.count();
    }
}