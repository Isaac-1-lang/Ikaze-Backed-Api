package com.ecommerce.repository;

import com.ecommerce.entity.AppealMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppealMediaRepository extends JpaRepository<AppealMedia, Long> {

    /**
     * Find all media files for a specific appeal
     */
    List<AppealMedia> findByAppealId(Long appealId);

    /**
     * Find media files by appeal ID ordered by upload date
     */
    List<AppealMedia> findByAppealIdOrderByUploadedAtAsc(Long appealId);

    /**
     * Find media files by appeal ID ordered by upload date (descending)
     */
    List<AppealMedia> findByAppealIdOrderByUploadedAtDesc(Long appealId);

    /**
     * Find media files by file type
     */
    List<AppealMedia> findByFileType(AppealMedia.FileType fileType);

    /**
     * Find media files by appeal ID and file type
     */
    List<AppealMedia> findByAppealIdAndFileType(Long appealId, AppealMedia.FileType fileType);

    /**
     * Find media files uploaded within a date range
     */
    List<AppealMedia> findByUploadedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find media files by appeal ID uploaded within a date range
     */
    List<AppealMedia> findByAppealIdAndUploadedAtBetween(Long appealId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count media files for a specific appeal
     */
    long countByAppealId(Long appealId);

    /**
     * Count media files by file type for a specific appeal
     */
    long countByAppealIdAndFileType(Long appealId, AppealMedia.FileType fileType);

    /**
     * Find image files for a specific appeal
     */
    @Query("SELECT am FROM AppealMedia am WHERE am.appealId = :appealId AND am.fileType = 'IMAGE' ORDER BY am.uploadedAt ASC")
    List<AppealMedia> findImagesByAppealId(@Param("appealId") Long appealId);

    /**
     * Find video files for a specific appeal
     */
    @Query("SELECT am FROM AppealMedia am WHERE am.appealId = :appealId AND am.fileType = 'VIDEO' ORDER BY am.uploadedAt ASC")
    List<AppealMedia> findVideosByAppealId(@Param("appealId") Long appealId);

    /**
     * Find media files by file URL (for duplicate checking)
     */
    List<AppealMedia> findByFileUrl(String fileUrl);

    /**
     * Check if media file exists for appeal
     */
    boolean existsByAppealIdAndFileUrl(Long appealId, String fileUrl);

    /**
     * Find media files uploaded before a specific date (for cleanup)
     */
    List<AppealMedia> findByUploadedAtBefore(LocalDateTime date);

    /**
     * Delete media files by appeal ID
     */
    void deleteByAppealId(Long appealId);

    /**
     * Find latest media file for an appeal
     */
    @Query("SELECT am FROM AppealMedia am WHERE am.appealId = :appealId ORDER BY am.uploadedAt DESC LIMIT 1")
    AppealMedia findLatestByAppealId(@Param("appealId") Long appealId);

    /**
     * Find first media file for an appeal
     */
    @Query("SELECT am FROM AppealMedia am WHERE am.appealId = :appealId ORDER BY am.uploadedAt ASC LIMIT 1")
    AppealMedia findFirstByAppealId(@Param("appealId") Long appealId);

    /**
     * Find media files by return request ID (through appeal relationship)
     */
    @Query("SELECT am FROM AppealMedia am JOIN am.returnAppeal ra WHERE ra.returnRequestId = :returnRequestId")
    List<AppealMedia> findByReturnRequestId(@Param("returnRequestId") Long returnRequestId);

    /**
     * Find media files by customer ID (through appeal and return request relationships)
     */
    @Query("SELECT am FROM AppealMedia am JOIN am.returnAppeal ra JOIN ra.returnRequest rr WHERE rr.customerId = :customerId")
    List<AppealMedia> findByCustomerId(@Param("customerId") String customerId);

    /**
     * Count media files by customer ID
     */
    @Query("SELECT COUNT(am) FROM AppealMedia am JOIN am.returnAppeal ra JOIN ra.returnRequest rr WHERE rr.customerId = :customerId")
    long countByCustomerId(@Param("customerId") String customerId);

    /**
     * Find media files by order ID (through appeal and return request relationships)
     */
    @Query("SELECT am FROM AppealMedia am JOIN am.returnAppeal ra JOIN ra.returnRequest rr WHERE rr.orderId = :orderId")
    List<AppealMedia> findByOrderId(@Param("orderId") Long orderId);

    /**
     * Find recent media files (last 30 days)
     */
    @Query("SELECT am FROM AppealMedia am WHERE am.uploadedAt >= :thirtyDaysAgo ORDER BY am.uploadedAt DESC")
    List<AppealMedia> findRecentMedia(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
}
