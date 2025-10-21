package com.minimart.api.controller;

import com.minimart.api.model.Product;
import com.minimart.api.model.Category;
import com.minimart.api.service.ProductService;
import com.minimart.api.service.CategoryService;
import com.minimart.api.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    // Get all products
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer categoryId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Product> products;
            
            // Filter by category if provided
            if (categoryId != null) {
                if ("name".equalsIgnoreCase(sort)) {
                    products = productService.getProductsByCategoryOrderedByName(categoryId);
                } else {
                    products = productService.getProductsByCategory(categoryId);
                }
            } else {
                // Get all products with optional sorting
                if ("name".equalsIgnoreCase(sort)) {
                    products = productService.getAllProductsOrderedByName();
                } else if ("price_asc".equalsIgnoreCase(sort)) {
                    products = productService.getAllProductsOrderedByPrice("asc");
                } else if ("price_desc".equalsIgnoreCase(sort)) {
                    products = productService.getAllProductsOrderedByPrice("desc");
                } else if ("date".equalsIgnoreCase(sort)) {
                    products = productService.getAllProductsOrderedByDate();
                } else {
                    products = productService.getAllProducts();
                }
            }
            
            response.put("success", true);
            response.put("data", products);
            response.put("count", products.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving products: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get product by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
            
            response.put("success", true);
            response.put("data", product);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get products by category
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Map<String, Object>> getProductsByCategory(@PathVariable Integer categoryId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Product> products = productService.getProductsByCategory(categoryId);
            
            response.put("success", true);
            response.put("data", products);
            response.put("count", products.size());
            response.put("categoryId", categoryId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving products: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Search products
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(required = false) String keyword) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Product> products = productService.searchProducts(keyword);
            
            response.put("success", true);
            response.put("data", products);
            response.put("count", products.size());
            response.put("keyword", keyword != null ? keyword : "all");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error searching products: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Create product (with stock input at the same time)
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(
            @RequestParam("name") String name,
            @RequestParam(value = "detail", required = false) String detail,
            @RequestParam("price") BigDecimal price,
            @RequestParam(value = "stock", required = false, defaultValue = "0") Integer stock,
            @RequestParam("category_id") Integer categoryId,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {
        
        Map<String, Object> response = new HashMap<>();
        String uploadedFilename = null;
        String categoryName = null;
        
        try {
            System.out.println("📝 Creating product");
            System.out.println("   Product name: " + name);
            System.out.println("   Price: " + price);
            System.out.println("   Stock: " + stock);
            System.out.println("   Category ID: " + categoryId);
            System.out.println("   Has image: " + (imageFile != null && !imageFile.isEmpty()));
            
            // Get category
            Category category = categoryService.getCategoryById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
            
            categoryName = category.getName();
            
            // Create product object
            Product product = new Product();
            product.setName(name);
            product.setDetail(detail);
            product.setPrice(price);
            product.setCategory(category);
            
            // Handle optional image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                System.out.println("📤 Uploading image to category folder: " + categoryName);
                
                uploadedFilename = fileStorageService.storeFileInCategoryFolder(imageFile, categoryName);
                
                // Generate RELATIVE URL (without localhost)
                String imageUrl = "/api/files/products/" + sanitizeFolderName(categoryName) + "/" + uploadedFilename;
                
                product.setImage(imageUrl);
                System.out.println("✅ Image uploaded: " + imageUrl);
            } else {
                System.out.println("ℹ️ No image uploaded - product created without image");
            }
            
            // Create product with stock (both at the same time!)
            Product newProduct = productService.createProduct(product, stock);
            
            response.put("success", true);
            response.put("message", "Product created successfully with stock: " + stock);
            response.put("data", newProduct);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            
            // Rollback: delete uploaded image if exists
            if (uploadedFilename != null && categoryName != null) {
                try {
                    fileStorageService.deleteFileInCategoryFolder(uploadedFilename, categoryName);
                    System.out.println("🗑️ Rolled back uploaded image: " + uploadedFilename);
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete uploaded image: " + ex.getMessage());
                }
            }
            
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            System.err.println("❌ Error creating product: " + e.getMessage());
            e.printStackTrace();
            
            // Rollback: delete uploaded image if exists
            if (uploadedFilename != null && categoryName != null) {
                try {
                    fileStorageService.deleteFileInCategoryFolder(uploadedFilename, categoryName);
                    System.out.println("🗑️ Rolled back uploaded image: " + uploadedFilename);
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete uploaded image: " + ex.getMessage());
                }
            }
            
            response.put("success", false);
            response.put("message", "Error creating product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Update product (with stock input at the same time)
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable Integer id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "detail", required = false) String detail,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam(value = "stock", required = false) Integer stock,
            @RequestParam(value = "category_id", required = false) Integer categoryId,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {
        
        Map<String, Object> response = new HashMap<>();
        String uploadedFilename = null;
        String oldImageUrl = null;
        String oldCategoryName = null;
        String newCategoryName = null;
        
        try {
            System.out.println("📝 Updating product");
            System.out.println("   Product ID: " + id);
            System.out.println("   New name: " + name);
            System.out.println("   New price: " + price);
            System.out.println("   New stock: " + stock);
            System.out.println("   New category ID: " + categoryId);
            System.out.println("   Has new image: " + (imageFile != null && !imageFile.isEmpty()));
            
            // Get existing product
            Product existingProduct = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
            
            oldImageUrl = existingProduct.getImage();
            oldCategoryName = existingProduct.getCategory().getName();
            
            // Prepare update
            Product productDetails = new Product();
            
            // Use new values if provided, otherwise keep existing
            productDetails.setName(name != null ? name : existingProduct.getName());
            productDetails.setDetail(detail != null ? detail : existingProduct.getDetail());
            productDetails.setPrice(price != null ? price : existingProduct.getPrice());
            
            // Handle category
            if (categoryId != null) {
                Category newCategory = categoryService.getCategoryById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
                newCategoryName = newCategory.getName();
                productDetails.setCategory(newCategory);
            } else {
                productDetails.setCategory(existingProduct.getCategory());
                newCategoryName = oldCategoryName;
            }
            
            // Handle optional image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                System.out.println("📤 Uploading new image to category folder: " + newCategoryName);
                
                uploadedFilename = fileStorageService.storeFileInCategoryFolder(imageFile, newCategoryName);
                
                // Generate RELATIVE URL (without localhost)
                String imageUrl = "/api/files/products/" + sanitizeFolderName(newCategoryName) + "/" + uploadedFilename;
                
                productDetails.setImage(imageUrl);
                System.out.println("✅ New image uploaded: " + imageUrl);
            } else {
                // Keep existing image or handle category change
                if (!oldCategoryName.equals(newCategoryName) && oldImageUrl != null && !oldImageUrl.isEmpty()) {
                    // Category changed but no new image - need to move existing image
                    System.out.println("ℹ️ Category changed, moving existing image");
                    
                    // Extract old filename
                    String oldFilename = extractFilenameFromUrl(oldImageUrl);
                    
                    if (oldFilename != null) {
                        try {
                            // Read old file
                            String oldSanitizedCategory = sanitizeFolderName(oldCategoryName);
                            java.nio.file.Path oldPath = java.nio.file.Paths.get("uploads/products")
                                    .resolve(oldSanitizedCategory)
                                    .resolve(oldFilename);
                            
                            if (java.nio.file.Files.exists(oldPath)) {
                                // Copy to new category folder
                                String newSanitizedCategory = sanitizeFolderName(newCategoryName);
                                java.nio.file.Path newPath = java.nio.file.Paths.get("uploads/products")
                                        .resolve(newSanitizedCategory);
                                
                                java.nio.file.Files.createDirectories(newPath);
                                java.nio.file.Path newFilePath = newPath.resolve(oldFilename);
                                
                                java.nio.file.Files.copy(oldPath, newFilePath, 
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                
                                // Delete old file
                                java.nio.file.Files.deleteIfExists(oldPath);
                                
                                // Update URL (RELATIVE)
                                String newImageUrl = "/api/files/products/" + newSanitizedCategory + "/" + oldFilename;
                                
                                productDetails.setImage(newImageUrl);
                                System.out.println("✅ Image moved to new category folder");
                            } else {
                                productDetails.setImage(oldImageUrl);
                                System.out.println("⚠️ Old image not found, keeping URL");
                            }
                        } catch (Exception e) {
                            System.err.println("⚠️ Failed to move image: " + e.getMessage());
                            productDetails.setImage(oldImageUrl);
                        }
                    } else {
                        productDetails.setImage(oldImageUrl);
                    }
                } else {
                    productDetails.setImage(existingProduct.getImage());
                    System.out.println("ℹ️ Keeping existing image");
                }
            }
            
            // Update product with stock (both at the same time!)
            Product updatedProduct = productService.updateProduct(id, productDetails, stock);
            
            // Delete old image only after successful update and if new image was uploaded
            if (uploadedFilename != null && oldImageUrl != null && !oldImageUrl.isEmpty()) {
                String oldFilename = extractFilenameFromUrl(oldImageUrl);
                if (oldFilename != null && !oldFilename.isEmpty()) {
                    try {
                        fileStorageService.deleteFileInCategoryFolder(oldFilename, oldCategoryName);
                        System.out.println("🗑️ Deleted old image: " + oldFilename);
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to delete old image: " + e.getMessage());
                    }
                }
            }
            
            response.put("success", true);
            response.put("message", "Product updated successfully");
            response.put("data", updatedProduct);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            
         // Rollback: delete new uploaded image if exists
            if (uploadedFilename != null && newCategoryName != null) {
                try {
                    fileStorageService.deleteFileInCategoryFolder(uploadedFilename, newCategoryName);
                    System.out.println("🗑️ Rolled back uploaded image: " + uploadedFilename);
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete uploaded image: " + ex.getMessage());
                }
            }
            
            response.put("success", false);
            response.put("message", e.getMessage());
            
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            System.err.println("❌ Error updating product: " + e.getMessage());
            e.printStackTrace();
            
            // Rollback: delete new uploaded image if exists
            if (uploadedFilename != null && newCategoryName != null) {
                try {
                    fileStorageService.deleteFileInCategoryFolder(uploadedFilename, newCategoryName);
                    System.out.println("🗑️ Rolled back uploaded image: " + uploadedFilename);
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete uploaded image: " + ex.getMessage());
                }
            }
            
            response.put("success", false);
            response.put("message", "Error updating product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Delete product
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get product to delete its image
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
            
            // Delete product image if exists
            if (product.getImage() != null && !product.getImage().isEmpty()) {
                String filename = extractFilenameFromUrl(product.getImage());
                String categoryName = product.getCategory().getName();
                
                if (filename != null && categoryName != null) {
                    try {
                        fileStorageService.deleteFileInCategoryFolder(filename, categoryName);
                        System.out.println("🗑️ Deleted product image: " + filename);
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to delete image: " + e.getMessage());
                    }
                }
            }
            
            // Delete product
            productService.deleteProduct(id);
            
            response.put("success", true);
            response.put("message", "Product deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get product count
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getProductCount(
            @RequestParam(required = false) Integer categoryId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            long count;
            
            if (categoryId != null) {
                count = productService.getProductCountByCategory(categoryId);
            } else {
                count = productService.getProductCount();
            }
            
            response.put("success", true);
            response.put("count", count);
            if (categoryId != null) {
                response.put("categoryId", categoryId);
            }
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error getting product count: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Add stock endpoint
    @PostMapping("/{id}/add-stock")
    public ResponseEntity<Map<String, Object>> addStock(
            @PathVariable Integer id,
            @RequestParam("quantity") Integer quantity) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Product product = productService.addStock(id, quantity);
            
            response.put("success", true);
            response.put("message", "Stock added successfully. New quantity: " + product.getStock().getQty());
            response.put("data", product);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error adding stock: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Remove stock endpoint
    @PostMapping("/{id}/remove-stock")
    public ResponseEntity<Map<String, Object>> removeStock(
            @PathVariable Integer id,
            @RequestParam("quantity") Integer quantity) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Product product = productService.removeStock(id, quantity);
            
            response.put("success", true);
            response.put("message", "Stock removed successfully. New quantity: " + product.getStock().getQty());
            response.put("data", product);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error removing stock: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Set stock endpoint
    @PutMapping("/{id}/set-stock")
    public ResponseEntity<Map<String, Object>> setStock(
            @PathVariable Integer id,
            @RequestParam("quantity") Integer quantity) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Product product = productService.setStock(id, quantity);
            
            response.put("success", true);
            response.put("message", "Stock set successfully. New quantity: " + product.getStock().getQty());
            response.put("data", product);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error setting stock: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get products with low stock
    @GetMapping("/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockProducts(
            @RequestParam(value = "threshold", defaultValue = "10") Integer threshold) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Product> products = productService.getLowStockProducts(threshold);
            
            response.put("success", true);
            response.put("data", products);
            response.put("count", products.size());
            response.put("threshold", threshold);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving low stock products: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Helper method to extract filename from URL
    private String extractFilenameFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        
        int lastSlashIndex = imageUrl.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < imageUrl.length() - 1) {
            return imageUrl.substring(lastSlashIndex + 1);
        }
        
        return null;
    }
    
    // Helper method to sanitize folder name
    private String sanitizeFolderName(String name) {
        if (name == null) {
            return null;
        }
        // Replace special characters with underscore, keep alphanumeric and spaces
        return name.replaceAll("[^a-zA-Z0-9\\s]", "_").trim();
    }
}