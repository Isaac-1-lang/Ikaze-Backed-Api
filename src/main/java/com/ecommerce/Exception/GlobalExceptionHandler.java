package com.ecommerce.Exception;

import com.ecommerce.dto.FileUploadResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> handleCustomException(CustomException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<FileUploadResponseDTO> handleFileUploadException(FileUploadException ex) {
        FileUploadResponseDTO response = FileUploadResponseDTO.builder()
                .error(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<FileUploadResponseDTO> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        FileUploadResponseDTO response = FileUploadResponseDTO.builder()
                .error("File size exceeds the maximum allowed limit")
                .build();
        return new ResponseEntity<>(response, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<FileUploadResponseDTO> handleIllegalArgumentException(IllegalArgumentException ex) {
        // Check if it's a video duration or file size validation error
        String message = ex.getMessage();
        if (message.contains("duration") || message.contains("file size")) {
            FileUploadResponseDTO response = FileUploadResponseDTO.builder()
                    .error(message)
                    .build();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // For other IllegalArgumentException, return generic message
        FileUploadResponseDTO response = FileUploadResponseDTO.builder()
                .error("Invalid input: " + ex.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        System.err.println("Unexpected error: " + ex.getMessage());
        ex.printStackTrace();
        return new ResponseEntity<>("An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
