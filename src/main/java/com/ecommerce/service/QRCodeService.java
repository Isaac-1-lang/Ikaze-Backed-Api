package com.ecommerce.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class QRCodeService {

    /**
     * Generates a QR code as byte array for the given text
     * 
     * @param text The text to encode in the QR code
     * @param width Width of the QR code image
     * @param height Height of the QR code image
     * @return Byte array of the QR code image in PNG format
     * @throws WriterException If QR code generation fails
     * @throws IOException If image conversion fails
     */
    public byte[] generateQRCode(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        log.info("Generated QR code for text: {}", text.substring(0, Math.min(text.length(), 50)) + "...");
        return outputStream.toByteArray();
    }

    /**
     * Generates a QR code with default size (200x200) for pickup token
     * 
     * @param pickupToken The pickup token to encode
     * @return Byte array of the QR code image
     */
    public byte[] generatePickupTokenQR(String pickupToken) {
        try {
            return generateQRCode(pickupToken, 200, 200);
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code for pickup token: {}", pickupToken, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Generates a QR code for order tracking with order details
     * 
     * @param orderNumber The order number
     * @param pickupToken The pickup token
     * @return Byte array of the QR code image
     */
    public byte[] generateOrderTrackingQR(String orderNumber, String pickupToken) {
        String qrContent = String.format("Order: %s | Pickup Token: %s", orderNumber, pickupToken);
        try {
            return generateQRCode(qrContent, 250, 250);
        } catch (WriterException | IOException e) {
            log.error("Failed to generate order tracking QR code for order: {}", orderNumber, e);
            throw new RuntimeException("Failed to generate order tracking QR code", e);
        }
    }
}
