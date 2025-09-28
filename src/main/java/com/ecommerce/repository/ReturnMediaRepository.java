package com.ecommerce.repository;

import com.ecommerce.entity.ReturnMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReturnMediaRepository extends JpaRepository<ReturnMedia, Long> {

    /**
     * Find all media files for a specific return request
     */
    List<ReturnMedia> findByReturnRequestId(Long returnRequestId);

    /**
     * Find media files by return request ID ordered by upload date
     */
    List<ReturnMedia> findByReturnRequestIdOrderByUploadedAtAsc(Long returnRequestId);

    /**
     * Find media files by return request ID ordered by upload date (descending)
     */
    List<ReturnMedia> findByReturnRequestIdOrderByUploadedAtDesc(Long returnRequestId);

    /**
     * Find media files by file type
     */
    List<ReturnMedia> findByFileType(ReturnMedia.FileType fileType);

    /**
     * Find media files by return request ID and file type
     */
    List<ReturnMedia> findByReturnRequestIdAndFileType(Long returnRequestId, ReturnMedia.FileType fileType);

    /**
     * Find media files uploaded within a date range
     */
    List<ReturnMedia> findByUploadedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find media files by return request ID uploaded within a date range
     */
    List<ReturnMedia> findByReturnRequestIdAndUploadedAtBetween(Long returnRequestId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count media files for a specific return request
     */
    long countByReturnRequestId(Long returnRequestId);

    /**
     * Count media files by file type for a specific return request
     */
    long countByReturnRequestIdAndFileType(Long returnRequestId, ReturnMedia.FileType fileType);

    /**
     * Find image files for a specific return request
     */
    @Query("SELECT rm FROM ReturnMedia rm WHERE rm.returnRequestId = :returnRequestId AND rm.fileType = 'IMAGE' ORDER BY rm.uploadedAt ASC")
    List<ReturnMedia> findImagesByReturnRequestId(@Param("returnRequestId") Long returnRequestId);

    /**
     * Find video files for a specific return request
     */
    @Query("SELECT rm FROM ReturnMedia rm WHERE rm.returnRequestId = :returnRequestId AND rm.fileType = 'VIDEO' ORDER BY rm.uploadedAt ASC")
    List<ReturnMedia> findVideosByReturnRequestId(@Param("returnRequestId") Long returnRequestId);

    /**
     * Find media files by file URL (for duplicate checking)
     */
    List<ReturnMedia> findByFileUrl(String fileUrl);

    /**
     * Check if media file exists for return request
     */
    boolean existsByReturnRequestIdAndFileUrl(Long returnRequestId, String fileUrl);

    /**
     * Find media files uploaded before a specific date (for cleanup)
     */
    List<ReturnMedia> findByUploadedAtBefore(LocalDateTime date);

    /**
     * Delete media files by return request ID
     */
    void deleteByReturnRequestId(Long returnRequestId);

    /**
     * Find latest media file for a return request
     */
    @Query("SELECT rm FROM ReturnMedia rm WHERE rm.returnRequestId = :returnRequestId ORDER BY rm.uploadedAt DESC LIMIT 1")
    ReturnMedia findLatestByReturnRequestId(@Param("returnRequestId") Long returnRequestId);

    /**
     * Find first media file for a return request
     */
    @Query("SELECT rm FROM ReturnMedia rm WHERE rm.returnRequestId = :returnRequestId ORDER BY rm.uploadedAt ASC LIMIT 1")
    ReturnMedia findFirstByReturnRequestId(@Param("returnRequestId") Long returnRequestId);
}
