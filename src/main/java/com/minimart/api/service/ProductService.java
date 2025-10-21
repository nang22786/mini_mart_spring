package com.minimart.api.service;

import com.minimart.api.model.Product;
import com.minimart.api.model.Stock;
import com.minimart.api.model.Category;
import com.minimart.api.repository.ProductRepository;
import com.minimart.api.repository.StockRepository;
import com.minimart.api.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private StockRepository stockRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    // Get all products
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    // Get all products ordered by name
    public List<Product> getAllProductsOrderedByName() {
        return productRepository.findAllByOrderByNameAsc();
    }
    
    // Get all products ordered by price
    public List<Product> getAllProductsOrderedByPrice(String direction) {
        if ("desc".equalsIgnoreCase(direction)) {
            return productRepository.findAllByOrderByPriceDesc();
        }
        return productRepository.findAllByOrderByPriceAsc();
    }
    
    // Get all products ordered by creation date
    public List<Product> getAllProductsOrderedByDate() {
        return productRepository.findAllByOrderByCreatedAtDesc();
    }
    
    // Get product by ID
    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }
    
    // Get products by category
    public List<Product> getProductsByCategory(Integer categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }
    
    // Get products by category ordered by name
    public List<Product> getProductsByCategoryOrderedByName(Integer categoryId) {
        return productRepository.findByCategoryIdOrderByNameAsc(categoryId);
    }
    
    // Search products
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.searchProducts(keyword.trim());
    }
    
    // Get products with low stock
    public List<Product> getLowStockProducts(Integer threshold) {
        return productRepository.findByStockLessThanEqual(threshold);
    }
    
    // Create product with stock
    public Product createProduct(Product product, Integer stockQty) {
        // Validate product
        validateProduct(product);
        
        // Set default stock to 0 if not provided
        if (stockQty == null) {
            stockQty = 0;
        }
        
        // Validate stock quantity
        if (stockQty < 0) {
            throw new RuntimeException("Stock quantity cannot be negative");
        }
        
        // Check if category exists
        Category category = categoryRepository.findById(product.getCategory().getId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + product.getCategory().getId()));
        
        product.setCategory(category);
        
        // Create stock entry
        Stock stock = new Stock();
        stock.setQty(stockQty);
        stock.setProduct(product);
        product.setStock(stock);
        
        // Save product (stock will be saved automatically due to cascade)
        return productRepository.save(product);
    }
    
    // Update product with stock
    public Product updateProduct(Integer id, Product productDetails, Integer stockQty) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        // Validate product details
        validateProduct(productDetails);
        
        // Check if category exists
        Category category = categoryRepository.findById(productDetails.getCategory().getId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + productDetails.getCategory().getId()));
        
        // Update product fields
        product.setName(productDetails.getName());
        product.setDetail(productDetails.getDetail());
        product.setPrice(productDetails.getPrice());
        product.setCategory(category);
        product.setImage(productDetails.getImage());
        
        // Update stock if provided
        if (stockQty != null) {
            if (stockQty < 0) {
                throw new RuntimeException("Stock quantity cannot be negative");
            }
            
            if (product.getStock() != null) {
                // Update existing stock
                product.getStock().setQty(stockQty);
            } else {
                // Create new stock if it doesn't exist
                Stock stock = new Stock();
                stock.setQty(stockQty);
                stock.setProduct(product);
                product.setStock(stock);
            }
        }
        
        return productRepository.save(product);
    }
    
    // Delete product
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        // Delete image file if exists
        if (product.getImage() != null && !product.getImage().isEmpty()) {
            String filename = extractFilenameFromUrl(product.getImage());
            String categoryName = product.getCategory().getName();
            
            if (filename != null && !filename.isEmpty()) {
                try {
                    fileStorageService.deleteFileInCategoryFolder(filename, categoryName);
                    System.out.println("🗑️ Deleted product image: " + filename);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to delete product image: " + e.getMessage());
                }
            }
        }
        
        productRepository.delete(product);
        // Stock will be deleted automatically due to cascade = CascadeType.ALL
    }
    
    // Check if product exists
    public boolean productExists(Integer id) {
        return productRepository.existsById(id);
    }
    
    // Get product count
    public long getProductCount() {
        return productRepository.count();
    }
    
    // Get product count by category
    public long getProductCountByCategory(Integer categoryId) {
        return productRepository.countByCategoryId(categoryId);
    }
    
    // Add stock (increase stock quantity)
    public Product addStock(Integer productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity to add must be greater than 0");
        }
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        Stock stock = product.getStock();
        if (stock == null) {
            // Create new stock if doesn't exist
            stock = new Stock(quantity, product);
            product.setStock(stock);
        } else {
            // Add to existing stock
            stock.setQty(stock.getQty() + quantity);
        }
        
        return productRepository.save(product);
    }
    
    // Remove stock (decrease stock quantity)
    public Product removeStock(Integer productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity to remove must be greater than 0");
        }
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        Stock stock = product.getStock();
        if (stock == null) {
            throw new RuntimeException("Product has no stock record");
        }
        
        int newQty = stock.getQty() - quantity;
        if (newQty < 0) {
            throw new RuntimeException("Insufficient stock. Current: " + stock.getQty() + ", Requested: " + quantity);
        }
        
        stock.setQty(newQty);
        return productRepository.save(product);
    }
    
    // Set stock (replace stock quantity)
    public Product setStock(Integer productId, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new RuntimeException("Stock quantity cannot be negative");
        }
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        Stock stock = product.getStock();
        if (stock == null) {
            stock = new Stock(quantity, product);
            product.setStock(stock);
        } else {
            stock.setQty(quantity);
        }
        
        return productRepository.save(product);
    }
    
    // Validate product
    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new RuntimeException("Product name is required");
        }
        
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Product price must be greater than 0");
        }
        
        if (product.getCategory() == null || product.getCategory().getId() == null) {
            throw new RuntimeException("Product category is required");
        }
    }
    
    // Extract filename from URL
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
}