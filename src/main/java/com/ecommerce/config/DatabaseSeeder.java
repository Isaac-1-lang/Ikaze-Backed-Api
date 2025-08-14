package com.ecommerce.config;

import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    // User related
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Product related
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductAttributeTypeRepository attributeTypeRepository;
    private final ProductAttributeValueRepository attributeValueRepository;
    private final DiscountRepository discountRepository;

    // Product entities
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantImageRepository productVariantImageRepository;
    private final ProductVideoRepository productVideoRepository;
    private final VariantAttributeValueRepository variantAttributeValueRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (shouldSeedData()) {
            log.info("Starting database seeding...");

            // Seed in order of dependencies
            seedUsers();
            seedCategories();
            seedBrands();
            seedDiscounts();
            seedProductAttributes();
            seedProducts();

            log.info("Database seeding completed successfully!");
        } else {
            log.info("Database already contains data. Skipping seeding.");
        }
    }

    private boolean shouldSeedData() {
        // Check if data already exists
        return userRepository.count() == 0 && categoryRepository.count() == 0;
    }

    @Transactional
    public void seedUsers() {
        log.info("Seeding users...");

        String adminEmail = System.getenv("ADMIN_EMAIL");
        String adminPassword = System.getenv("ADMIN_PASSWORD");
        String adminFirstName = System.getenv("ADMIN_FIRST_NAME");
        String adminLastName = System.getenv("ADMIN_LAST_NAME");
        String adminPhone = System.getenv("ADMIN_PHONE");

        // Use default values if environment variables are not set
        if (adminEmail == null)
            adminEmail = "admin@example.com";
        if (adminPassword == null)
            adminPassword = "admin123";
        if (adminFirstName == null)
            adminFirstName = "Admin";
        if (adminLastName == null)
            adminLastName = "User";
        if (adminPhone == null)
            adminPhone = "+1234567890";

        if (userRepository.findByUserEmail(adminEmail).isEmpty()) {
            User adminUser = new User();
            adminUser.setFirstName(adminFirstName);
            adminUser.setLastName(adminLastName);
            adminUser.setUserEmail(adminEmail);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setRole(User.UserRole.ADMIN);
            adminUser.setAuthProvider(User.AuthProvider.LOCAL);
            adminUser.setEmailVerified(true);
            adminUser.setEnabled(true);
            adminUser.setAccountNonLocked(true);
            adminUser.setCredentialsNonExpired(true);
            adminUser.setAccountNonExpired(true);
            adminUser.setPhoneNumber(adminPhone);

            userRepository.save(adminUser);
            log.info("Admin user created: {}", adminUser.getUserEmail());
        }
    }

    @Transactional
    public void seedCategories() {
        log.info("Seeding categories...");

        List<Category> categories = Arrays.asList(
                createCategory("Electronics", "Electronic devices and accessories", "electronics"),
                createCategory("Audio", "Audio equipment and accessories", "audio"),
                createCategory("Headphones", "Headphones and earbuds", "headphones"),
                createCategory("Smartphones", "Mobile phones and accessories", "smartphones"),
                createCategory("Computers", "Laptops, desktops and computer accessories", "computers"));

        categoryRepository.saveAll(categories);
        log.info("Created {} categories", categories.size());
    }

    @Transactional
    public void seedBrands() {
        log.info("Seeding brands...");

        List<Brand> brands = Arrays.asList(
                createBrand("Apple", "Premium technology company", "apple"),
                createBrand("Samsung", "South Korean technology giant", "samsung"),
                createBrand("Sony", "Japanese electronics company", "sony"),
                createBrand("Bose", "Audio equipment specialist", "bose"),
                createBrand("Microsoft", "Software and hardware company", "microsoft"));

        brandRepository.saveAll(brands);
        log.info("Created {} brands", brands.size());
    }

    @Transactional
    public void seedDiscounts() {
        log.info("Seeding discounts...");

        List<Discount> discounts = Arrays.asList(
                createDiscount("Summer Sale", "Summer discount campaign", new BigDecimal("15.00"), "SUMMER15"),
                createDiscount("Black Friday", "Black Friday special discount", new BigDecimal("25.00"),
                        "BLACKFRIDAY25"),
                createDiscount("New Customer", "Discount for new customers", new BigDecimal("10.00"), "WELCOME10"));

        discountRepository.saveAll(discounts);
        log.info("Created {} discounts", discounts.size());
    }

    @Transactional
    public void seedProductAttributes() {
        log.info("Seeding product attributes...");

        // Create attribute types
        ProductAttributeType colorType = createAttributeType("Color", false);
        ProductAttributeType sizeType = createAttributeType("Size", false);
        ProductAttributeType storageType = createAttributeType("Storage", false);

        attributeTypeRepository.saveAll(Arrays.asList(colorType, sizeType, storageType));

        // Create color values
        List<ProductAttributeValue> colorValues = Arrays.asList(
                createAttributeValue("Blue", colorType),
                createAttributeValue("Red", colorType),
                createAttributeValue("Black", colorType),
                createAttributeValue("White", colorType),
                createAttributeValue("Silver", colorType),
                createAttributeValue("Gold", colorType));

        // Create size values
        List<ProductAttributeValue> sizeValues = Arrays.asList(
                createAttributeValue("XSM", sizeType),
                createAttributeValue("SM", sizeType),
                createAttributeValue("MD", sizeType),
                createAttributeValue("LG", sizeType),
                createAttributeValue("XLG", sizeType));

        // Create storage values
        List<ProductAttributeValue> storageValues = Arrays.asList(
                createAttributeValue("64GB", storageType),
                createAttributeValue("128GB", storageType),
                createAttributeValue("256GB", storageType),
                createAttributeValue("512GB", storageType),
                createAttributeValue("1TB", storageType));

        attributeValueRepository.saveAll(colorValues);
        attributeValueRepository.saveAll(sizeValues);
        attributeValueRepository.saveAll(storageValues);

        log.info("Created {} attribute values", colorValues.size() + sizeValues.size() + storageValues.size());
    }

    @Transactional
    public void seedProducts() {
        log.info("Seeding products...");

        // Get required entities
        Category audioCategory = categoryRepository.findByName("Audio").orElseThrow();
        Brand appleBrand = brandRepository.findByBrandName("Apple").orElseThrow();

        // Get attribute values
        ProductAttributeValue blueColor = attributeValueRepository.findByValue("Blue").orElseThrow();
        ProductAttributeValue redColor = attributeValueRepository.findByValue("Red").orElseThrow();
        ProductAttributeValue blackColor = attributeValueRepository.findByValue("Black").orElseThrow();
        ProductAttributeValue lgSize = attributeValueRepository.findByValue("LG").orElseThrow();
        ProductAttributeValue smSize = attributeValueRepository.findByValue("SM").orElseThrow();
        ProductAttributeValue mdSize = attributeValueRepository.findByValue("MD").orElseThrow();

        // Create AirPods Pro product
        Product airpods = createAirPodsProduct(audioCategory, appleBrand);
        Product savedAirpods = productRepository.save(airpods);

        // Add product images
        addProductImages(savedAirpods);

        // Add product videos
        addProductVideos(savedAirpods);

        // Create variants
        createAirPodsVariants(savedAirpods, blueColor, redColor, blackColor, lgSize, smSize, mdSize);

        log.info("Created AirPods product with variants");

        // Create additional sample products
        createAdditionalProducts(audioCategory, appleBrand);
    }

    private Product createAirPodsProduct(Category category, Brand brand) {
        Product product = new Product();
        product.setProductName("AirPods Pro");
        product.setShortDescription("Premium wireless earbuds with active noise cancellation");
        product.setSku("AIRPODS-PRO-001");
        product.setBarcode("1234567890123");
        product.setPrice(new BigDecimal("17.00"));
        product.setCompareAtPrice(new BigDecimal("20.00"));
        product.setCostPrice(new BigDecimal("12.00"));
        product.setStockQuantity(30);
        product.setLowStockThreshold(5);
        product.setCategory(category);
        product.setBrand(brand);
        product.setModel("A2084");
        product.setSlug("airpods-pro");
        product.setActive(true);
        product.setFeatured(true);
        product.setBestseller(true);
        product.setNewArrival(true);
        product.setOnSale(false);

        // Create product detail
        ProductDetail detail = new ProductDetail();
        detail.setProduct(product);
        detail.setDescription(
                "Experience premium sound quality with these state-of-the-art wireless earbuds featuring advanced noise cancellation technology.");
        detail.setMetaTitle("AirPods Pro - Premium Wireless Earbuds");
        detail.setMetaDescription("Buy AirPods Pro with noise cancellation. Available in multiple colors and sizes.");
        detail.setMetaKeywords("airpods, wireless, earbuds, noise cancellation, apple");
        detail.setSearchKeywords("airpods pro wireless earbuds apple noise cancellation");
        detail.setDimensionsCm("5.4 x 4.5 x 2.1");
        detail.setWeightKg(new BigDecimal("0.056"));
        detail.setMaterial("Plastic, Silicone");
        detail.setCareInstructions("Clean with dry cloth. Do not expose to water.");
        detail.setWarrantyInfo("1 year limited warranty");
        detail.setShippingInfo("Free shipping on orders over $25");
        detail.setReturnPolicy("30-day return policy");

        product.setProductDetail(detail);

        return product;
    }

    private void addProductImages(Product product) {
        List<ProductImage> images = Arrays.asList(
                createProductImage(product, "https://images.unsplash.com/photo-1588423771073-b8903fbb85b5?w=800",
                        "AirPods Pro main view", true, 0),
                createProductImage(product, "https://images.unsplash.com/photo-1572569511254-d8f925fe2cbb?w=800",
                        "AirPods Pro side view", false, 1),
                createProductImage(product, "https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?w=800",
                        "AirPods Pro in case", false, 2));

        productImageRepository.saveAll(images);
    }

    private void addProductVideos(Product product) {
        List<ProductVideo> videos = Arrays.asList(
                createProductVideo(product, "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
                        "AirPods Pro Demo", "Product demonstration video", 0),
                createProductVideo(product, "https://sample-videos.com/zip/10/mp4/SampleVideo_640x360_1mb.mp4",
                        "AirPods Pro Review", "Customer review video", 1));

        productVideoRepository.saveAll(videos);
    }

    private void createAirPodsVariants(Product product, ProductAttributeValue blueColor, ProductAttributeValue redColor,
            ProductAttributeValue blackColor, ProductAttributeValue lgSize,
            ProductAttributeValue smSize, ProductAttributeValue mdSize) {

        // Blue Large - 14 stock
        ProductVariant blueLarge = createVariant(product, "AIRPODS-PRO-BLUE-LG", new BigDecimal("16.00"), 14);
        ProductVariant savedBlueLarge = productVariantRepository.save(blueLarge);
        addVariantAttributes(savedBlueLarge, Arrays.asList(blueColor, lgSize));
        addVariantImages(savedBlueLarge, "Blue Large");

        // Red Large - 15 stock
        ProductVariant redLarge = createVariant(product, "AIRPODS-PRO-RED-LG", new BigDecimal("16.00"), 15);
        ProductVariant savedRedLarge = productVariantRepository.save(redLarge);
        addVariantAttributes(savedRedLarge, Arrays.asList(redColor, lgSize));
        addVariantImages(savedRedLarge, "Red Large");

        // Black Medium - 1 stock
        ProductVariant blackMedium = createVariant(product, "AIRPODS-PRO-BLACK-MD", new BigDecimal("17.00"), 1);
        ProductVariant savedBlackMedium = productVariantRepository.save(blackMedium);
        addVariantAttributes(savedBlackMedium, Arrays.asList(blackColor, mdSize));
        addVariantImages(savedBlackMedium, "Black Medium");

        // Additional variants for testing
        ProductVariant blueSmall = createVariant(product, "AIRPODS-PRO-BLUE-SM", new BigDecimal("16.00"), 8);
        ProductVariant savedBlueSmall = productVariantRepository.save(blueSmall);
        addVariantAttributes(savedBlueSmall, Arrays.asList(blueColor, smSize));
        addVariantImages(savedBlueSmall, "Blue Small");

        ProductVariant redSmall = createVariant(product, "AIRPODS-PRO-RED-SM", new BigDecimal("16.00"), 7);
        ProductVariant savedRedSmall = productVariantRepository.save(redSmall);
        addVariantAttributes(savedRedSmall, Arrays.asList(redColor, smSize));
        addVariantImages(savedRedSmall, "Red Small");
    }

    private void addVariantAttributes(ProductVariant variant, List<ProductAttributeValue> attributeValues) {
        for (ProductAttributeValue attributeValue : attributeValues) {
            VariantAttributeValue variantAttributeValue = new VariantAttributeValue();
            variantAttributeValue.setId(new VariantAttributeValue.VariantAttributeValueId(
                    variant.getId(), attributeValue.getAttributeValueId()));
            variantAttributeValue.setProductVariant(variant);
            variantAttributeValue.setAttributeValue(attributeValue);
            variantAttributeValueRepository.save(variantAttributeValue);
        }
    }

    private void addVariantImages(ProductVariant variant, String colorSize) {
        String baseUrl = "https://images.unsplash.com/photo-";
        String[] imageIds = { "1588423771073-b8903fbb85b5", "1572569511254-d8f925fe2cbb",
                "1606220945770-b5b6c2c55bf1" };

        for (int i = 0; i < imageIds.length; i++) {
            ProductVariantImage image = new ProductVariantImage();
            image.setProductVariant(variant);
            image.setImageUrl(baseUrl + imageIds[i] + "?w=800&q=80");
            image.setAltText("AirPods Pro " + colorSize + " view " + (i + 1));
            image.setTitle("AirPods Pro " + colorSize);
            image.setPrimary(i == 0);
            image.setSortOrder(i);
            image.setFileSize(150000L + (i * 10000)); // Simulate file sizes
            image.setMimeType("image/jpeg");
            image.setWidth(800);
            image.setHeight(600);

            productVariantImageRepository.save(image);
        }
    }

    private void createAdditionalProducts(Category category, Brand brand) {
        // Create iPhone sample product
        Product iphone = new Product();
        iphone.setProductName("iPhone 15 Pro");
        iphone.setShortDescription("Latest iPhone with titanium design");
        iphone.setSku("IPHONE-15-PRO-001");
        iphone.setPrice(new BigDecimal("999.00"));
        iphone.setCompareAtPrice(new BigDecimal("1099.00"));
        iphone.setStockQuantity(50);
        iphone.setCategory(category);
        iphone.setBrand(brand);
        iphone.setSlug("iphone-15-pro");
        iphone.setActive(true);
        iphone.setFeatured(true);
        iphone.setNewArrival(true);

        productRepository.save(iphone);

        // Add some images
        List<ProductImage> iphoneImages = Arrays.asList(
                createProductImage(iphone, "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=800",
                        "iPhone 15 Pro main", true, 0),
                createProductImage(iphone, "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800",
                        "iPhone 15 Pro back", false, 1));
        productImageRepository.saveAll(iphoneImages);

        log.info("Created additional sample products");
    }

    // Helper methods
    private Category createCategory(String name, String description, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setSlug(slug);
        category.setActive(true);
        category.setFeatured(false);
        category.setSortOrder(0);
        category.setMetaTitle(name);
        category.setMetaDescription(description);
        return category;
    }

    private Brand createBrand(String name, String description, String slug) {
        Brand brand = new Brand();
        brand.setBrandName(name);
        brand.setDescription(description);
        brand.setSlug(slug);
        brand.setActive(true);
        brand.setFeatured(true);
        brand.setSortOrder(0);
        brand.setMetaTitle(name);
        brand.setMetaDescription(description);
        return brand;
    }

    private Discount createDiscount(String name, String description, BigDecimal percentage, String code) {
        Discount discount = new Discount();
        discount.setName(name);
        discount.setDescription(description);
        discount.setPercentage(percentage);
        discount.setDiscountCode(code);
        discount.setStartDate(LocalDateTime.now());
        discount.setEndDate(LocalDateTime.now().plusMonths(3));
        discount.setActive(true);
        discount.setUsageLimit(1000);
        discount.setUsedCount(0);
        discount.setDiscountType(Discount.DiscountType.PERCENTAGE);
        return discount;
    }

    private ProductAttributeType createAttributeType(String name, boolean required) {
        ProductAttributeType type = new ProductAttributeType();
        type.setName(name);
        type.setRequired(required);
        return type;
    }

    private ProductAttributeValue createAttributeValue(String value, ProductAttributeType type) {
        ProductAttributeValue attributeValue = new ProductAttributeValue();
        attributeValue.setValue(value);
        attributeValue.setAttributeType(type);
        return attributeValue;
    }

    private ProductImage createProductImage(Product product, String url, String altText, boolean isPrimary,
            int sortOrder) {
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(url);
        image.setAltText(altText);
        image.setTitle(altText);
        image.setPrimary(isPrimary);
        image.setSortOrder(sortOrder);
        image.setFileSize(200000L); // 200KB
        image.setMimeType("image/jpeg");
        image.setWidth(800);
        image.setHeight(600);
        return image;
    }

    private ProductVideo createProductVideo(Product product, String url, String title, String description,
            int sortOrder) {
        ProductVideo video = new ProductVideo();
        video.setProduct(product);
        video.setUrl(url);
        video.setTitle(title);
        video.setDescription(description);
        video.setSortOrder(sortOrder);
        return video;
    }

    private ProductVariant createVariant(Product product, String sku, BigDecimal price, int stock) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantSku(sku);
        variant.setPrice(price);
        variant.setStockQuantity(stock);
        variant.setLowStockThreshold(2);
        variant.setActive(true);
        variant.setSortOrder(0);
        return variant;
    }
}
