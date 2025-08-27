package com.ecommerce.service;

import com.ecommerce.dto.BrandDTO;
import com.ecommerce.dto.CreateBrandDTO;
import com.ecommerce.entity.Brand;
import com.ecommerce.repository.BrandRepository;
import com.ecommerce.service.impl.BrandServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandServiceImpl brandService;

    private CreateBrandDTO createBrandDTO;
    private Brand brand;
    private UUID brandId;

    @BeforeEach
    void setUp() {
        brandId = UUID.randomUUID();

        createBrandDTO = new CreateBrandDTO();
        createBrandDTO.setBrandName("Test Brand");
        createBrandDTO.setDescription("Test Description");
        createBrandDTO.setLogoUrl("https://example.com/logo.png");
        createBrandDTO.setWebsiteUrl("https://example.com");
        createBrandDTO.setActive(true);
        createBrandDTO.setFeatured(false);
        createBrandDTO.setSortOrder(1);
        createBrandDTO.setMetaTitle("Test Meta Title");
        createBrandDTO.setMetaDescription("Test Meta Description");
        createBrandDTO.setMetaKeywords("test, brand, example");

        brand = new Brand();
        brand.setBrandId(brandId);
        brand.setBrandName("Test Brand");
        brand.setDescription("Test Description");
        brand.setLogoUrl("https://example.com/logo.png");
        brand.setWebsiteUrl("https://example.com");
        brand.setActive(true);
        brand.setFeatured(false);
        brand.setSortOrder(1);
        brand.setMetaTitle("Test Meta Title");
        brand.setMetaDescription("Test Meta Description");
        brand.setMetaKeywords("test, brand, example");
    }

    @Test
    void testCreateBrand_Success() {
        // Arrange
        when(brandRepository.findByBrandName("Test Brand")).thenReturn(Optional.empty());
        when(brandRepository.save(any(Brand.class))).thenReturn(brand);

        // Act
        BrandDTO result = brandService.createBrand(createBrandDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Test Brand", result.getBrandName());
        assertEquals("Test Description", result.getDescription());
        assertEquals("https://example.com/logo.png", result.getLogoUrl());
        assertEquals("https://example.com", result.getWebsiteUrl());
        assertTrue(result.isActive());
        assertFalse(result.isFeatured());
        assertEquals(1, result.getSortOrder());
        assertEquals("Test Meta Title", result.getMetaTitle());
        assertEquals("Test Meta Description", result.getMetaDescription());
        assertEquals("test, brand, example", result.getMetaKeywords());

        verify(brandRepository).findByBrandName("Test Brand");
        verify(brandRepository).save(any(Brand.class));
    }

    @Test
    void testCreateBrand_DuplicateName() {
        // Arrange
        when(brandRepository.findByBrandName("Test Brand")).thenReturn(Optional.of(brand));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            brandService.createBrand(createBrandDTO);
        });

        verify(brandRepository).findByBrandName("Test Brand");
        verify(brandRepository, never()).save(any(Brand.class));
    }

    @Test
    void testGetBrandById_Success() {
        // Arrange
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));

        // Act
        BrandDTO result = brandService.getBrandById(brandId);

        // Assert
        assertNotNull(result);
        assertEquals(brandId, result.getBrandId());
        assertEquals("Test Brand", result.getBrandName());

        verify(brandRepository).findById(brandId);
    }

    @Test
    void testGetBrandById_NotFound() {
        // Arrange
        when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
            brandService.getBrandById(brandId);
        });

        verify(brandRepository).findById(brandId);
    }

    @Test
    void testExistsByBrandName_Exists() {
        // Arrange
        when(brandRepository.findByBrandName("Test Brand")).thenReturn(Optional.of(brand));

        // Act
        boolean result = brandService.existsByBrandName("Test Brand", null);

        // Assert
        assertTrue(result);
        verify(brandRepository).findByBrandName("Test Brand");
    }

    @Test
    void testExistsByBrandName_NotExists() {
        // Arrange
        when(brandRepository.findByBrandName("Test Brand")).thenReturn(Optional.empty());

        // Act
        boolean result = brandService.existsByBrandName("Test Brand", null);

        // Assert
        assertFalse(result);
        verify(brandRepository).findByBrandName("Test Brand");
    }
}
