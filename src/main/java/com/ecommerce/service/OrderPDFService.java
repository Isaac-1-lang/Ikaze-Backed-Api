package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderCustomerInfo;
import com.ecommerce.entity.OrderInfo;
import com.ecommerce.entity.OrderAddress;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPDFService {

    private final QRCodeService qrCodeService;
    
    // Enhanced color scheme - Amazon-inspired
    private static final BaseColor BRAND_PRIMARY = new BaseColor(35, 47, 62);      // Dark blue-gray
    private static final BaseColor BRAND_ACCENT = new BaseColor(255, 153, 0);       // Amazon orange
    private static final BaseColor TABLE_HEADER = new BaseColor(245, 245, 245);     // Light gray
    private static final BaseColor TABLE_BORDER = new BaseColor(221, 221, 221);     // Border gray
    private static final BaseColor TEXT_DARK = new BaseColor(51, 51, 51);           // Dark text
    private static final BaseColor TEXT_MUTED = new BaseColor(102, 102, 102);       // Muted text
    private static final BaseColor SUCCESS_GREEN = new BaseColor(0, 123, 85);       // Success green
    
    /**
     * Generates a professional PDF order invoice/summary with enhanced design
     */
    public byte[] generateOrderInvoicePDF(Order order) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            // Enhanced fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 32, BRAND_PRIMARY);
            Font largeHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, TEXT_DARK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TEXT_DARK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, TEXT_DARK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_DARK);
            Font mutedFont = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_MUTED);
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BRAND_PRIMARY);
            
            // Company header with logo placeholder and branding
            addCompanyHeader(document, titleFont, largeHeaderFont, mutedFont);
            
            // Order overview section
            addOrderOverview(document, order, headerFont, boldFont, normalFont);
            
            // Customer information
            if (order.getOrderCustomerInfo() != null) {
                addCustomerInformation(document, order.getOrderCustomerInfo(), headerFont, boldFont, normalFont);
            }
            
            // Pickup address information
            if (order.getOrderAddress() != null) {
                addPickupAddress(document, order.getOrderAddress(), headerFont, boldFont, normalFont);
            }
            
            // Order items with enhanced table
            addOrderItems(document, order, headerFont, boldFont, normalFont);
            
            // Order summary/totals
            if (order.getOrderInfo() != null) {
                addOrderSummary(document, order, headerFont, boldFont, normalFont, totalFont);
            }
            
            // QR Code and pickup information
            addPickupInformation(document, order, headerFont, boldFont, normalFont);
            
            // Footer
            addFooter(document, normalFont, mutedFont);
            
            document.close();
            
            log.info("Generated enhanced PDF invoice for order: {}", order.getOrderCode());
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to generate PDF for order: {}", order.getOrderCode(), e);
            throw new RuntimeException("Failed to generate PDF invoice", e);
        }
    }
    
    private void addCompanyHeader(Document document, Font titleFont, Font largeHeaderFont, Font mutedFont) throws DocumentException {
        // Company name with accent color
        Paragraph title = new Paragraph();
        Chunk shopChunk = new Chunk("SHOP", titleFont);
        Chunk sphereChunk = new Chunk("SPHERE", new Font(titleFont.getBaseFont(), titleFont.getSize(), Font.BOLD, BRAND_ACCENT));
        title.add(shopChunk);
        title.add(sphereChunk);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        document.add(title);
        
        // Subtitle
        Paragraph subtitle = new Paragraph("Your Order Confirmation", largeHeaderFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(30);
        document.add(subtitle);
        
        // Add a subtle separator line
        addSeparatorLine(document);
    }
    
    private void addOrderOverview(Document document, Order order, Font headerFont, Font boldFont, Font normalFont) throws DocumentException {
        Paragraph sectionHeader = new Paragraph("Order Details", headerFont);
        sectionHeader.setSpacingBefore(20);
        sectionHeader.setSpacingAfter(10);
        document.add(sectionHeader);
        
        PdfPTable orderTable = new PdfPTable(4);
        orderTable.setWidthPercentage(100);
        orderTable.setWidths(new float[]{1, 1, 1, 1});
        orderTable.setSpacingAfter(20);
        
        // Add order info in a clean 4-column layout
        orderTable.addCell(createInfoCell("Order Number", order.getOrderCode(), boldFont, normalFont));
        orderTable.addCell(createInfoCell("Order Date", 
            order.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), boldFont, normalFont));
        orderTable.addCell(createInfoCell("Status", 
            formatOrderStatus(order.getOrderStatus().toString()), boldFont, normalFont));
        orderTable.addCell(createInfoCell("Items", 
            String.valueOf(order.getOrderItems().size()), boldFont, normalFont));
        
        document.add(orderTable);
    }
    
    private void addCustomerInformation(Document document, OrderCustomerInfo customerInfo, 
                                      Font headerFont, Font boldFont, Font normalFont) throws DocumentException {
        Paragraph customerHeader = new Paragraph("Customer Information", headerFont);
        customerHeader.setSpacingBefore(20);
        customerHeader.setSpacingAfter(10);
        document.add(customerHeader);
        
        PdfPTable customerTable = new PdfPTable(1);
        customerTable.setWidthPercentage(100);
        customerTable.setSpacingAfter(20);
        
        // Create a bordered cell for customer info
        PdfPCell customerCell = new PdfPCell();
        customerCell.setBorderColor(TABLE_BORDER);
        customerCell.setBorderWidth(1);
        customerCell.setPadding(15);
        customerCell.setBackgroundColor(BaseColor.WHITE);
        
        Paragraph customerContent = new Paragraph();
        customerContent.add(new Chunk(customerInfo.getFirstName() + " " + customerInfo.getLastName() + "\n", boldFont));
        customerContent.add(new Chunk(customerInfo.getEmail() + "\n", normalFont));
        if (customerInfo.getPhoneNumber() != null) {
            customerContent.add(new Chunk(customerInfo.getPhoneNumber(), normalFont));
        }
        
        customerCell.addElement(customerContent);
        customerTable.addCell(customerCell);
        document.add(customerTable);
    }
    
    private void addPickupAddress(Document document, OrderAddress orderAddress, 
                                Font headerFont, Font boldFont, Font normalFont) throws DocumentException {
        Paragraph addressHeader = new Paragraph("Pickup Address", headerFont);
        addressHeader.setSpacingBefore(20);
        addressHeader.setSpacingAfter(10);
        document.add(addressHeader);
        
        PdfPTable addressTable = new PdfPTable(1);
        addressTable.setWidthPercentage(100);
        addressTable.setSpacingAfter(20);
        
        // Create a bordered cell for address info
        PdfPCell addressCell = new PdfPCell();
        addressCell.setBorderColor(TABLE_BORDER);
        addressCell.setBorderWidth(1);
        addressCell.setPadding(15);
        addressCell.setBackgroundColor(new BaseColor(248, 250, 252)); // Light blue background
        
        Paragraph addressContent = new Paragraph();
        
        // Street address
        if (orderAddress.getStreet() != null && !orderAddress.getStreet().isEmpty()) {
            addressContent.add(new Chunk(orderAddress.getStreet() + "\n", normalFont));
        }
        
        // Road name (if different from street)
        if (orderAddress.getRoadName() != null && !orderAddress.getRoadName().isEmpty() 
            && !orderAddress.getRoadName().equals(orderAddress.getStreet())) {
            addressContent.add(new Chunk(orderAddress.getRoadName() + "\n", normalFont));
        }
        
        // Regions
        if (orderAddress.getRegions() != null && !orderAddress.getRegions().isEmpty()) {
            String[] regions = orderAddress.getRegionsArray();
            if (regions.length > 0) {
                addressContent.add(new Chunk(String.join(", ", regions) + "\n", normalFont));
            }
        }
        
        // Country
        if (orderAddress.getCountry() != null && !orderAddress.getCountry().isEmpty()) {
            addressContent.add(new Chunk(orderAddress.getCountry() + "\n", boldFont));
        }
        
        // Coordinates (if available)
        if (orderAddress.getLatitude() != null && orderAddress.getLongitude() != null) {
            addressContent.add(new Chunk(
                String.format("Coordinates: %.6f, %.6f", orderAddress.getLatitude(), orderAddress.getLongitude()), 
                new Font(normalFont.getBaseFont(), 9, Font.ITALIC, TEXT_MUTED)
            ));
        }
        
        addressCell.addElement(addressContent);
        addressTable.addCell(addressCell);
        document.add(addressTable);
    }
    
    private void addOrderItems(Document document, Order order, Font headerFont, Font boldFont, Font normalFont) throws DocumentException {
        Paragraph itemsHeader = new Paragraph("Order Items", headerFont);
        itemsHeader.setSpacingBefore(20);
        itemsHeader.setSpacingAfter(10);
        document.add(itemsHeader);
        
        PdfPTable itemsTable = new PdfPTable(6);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{3.5f, 1, 1.2f, 1.2f, 1f, 1.1f});
        itemsTable.setSpacingAfter(20);
        
        // Enhanced table headers
        itemsTable.addCell(createStyledHeaderCell("Product", boldFont));
        itemsTable.addCell(createStyledHeaderCell("Qty", boldFont));
        itemsTable.addCell(createStyledHeaderCell("Original Price", boldFont));
        itemsTable.addCell(createStyledHeaderCell("Paid Price", boldFont));
        itemsTable.addCell(createStyledHeaderCell("Discount", boldFont));
        itemsTable.addCell(createStyledHeaderCell("Total", boldFont));
        
        // Add items with alternating row colors
        boolean isAlternate = false;
        for (OrderItem item : order.getOrderItems()) {
            String productName = getProductDisplayName(item);
            BaseColor rowColor = isAlternate ? new BaseColor(249, 249, 249) : BaseColor.WHITE;
            
            // Calculate discount information
            DiscountInfo discountInfo = calculateDiscountInfo(item);
            
            itemsTable.addCell(createStyledDataCell(productName, normalFont, rowColor));
            itemsTable.addCell(createStyledDataCell(String.valueOf(item.getQuantity()), normalFont, rowColor));
            itemsTable.addCell(createStyledDataCell("$" + formatPrice(discountInfo.originalPrice), normalFont, rowColor));
            itemsTable.addCell(createStyledDataCell("$" + formatPrice(item.getPrice()), normalFont, rowColor));
            
            // Discount column
            if (discountInfo.hasDiscount) {
                Font discountFont = new Font(boldFont.getBaseFont(), boldFont.getSize(), Font.BOLD, SUCCESS_GREEN);
                itemsTable.addCell(createStyledDataCell(discountInfo.discountText, discountFont, rowColor));
            } else {
                itemsTable.addCell(createStyledDataCell("-", normalFont, rowColor));
            }
            
            itemsTable.addCell(createStyledDataCell("$" + formatPrice(item.getSubtotal()), boldFont, rowColor));
            
            isAlternate = !isAlternate;
        }
        
        document.add(itemsTable);
    }
    
    private void addOrderSummary(Document document, Order order, Font headerFont, Font boldFont, 
                               Font normalFont, Font totalFont) throws DocumentException {
        OrderInfo orderInfo = order.getOrderInfo();
        
        // Calculate subtotal
        BigDecimal subtotal = order.getOrderItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(50);
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.setWidths(new float[]{2, 1});
        summaryTable.setSpacingAfter(30);
        
        // Subtotal
        summaryTable.addCell(createSummaryCell("Subtotal:", normalFont, false));
        summaryTable.addCell(createSummaryCell("$" + formatPrice(subtotal), normalFont, true));
        
        // Shipping
        if (orderInfo.getShippingCost() != null && orderInfo.getShippingCost().compareTo(BigDecimal.ZERO) > 0) {
            summaryTable.addCell(createSummaryCell("Shipping & Handling:", normalFont, false));
            summaryTable.addCell(createSummaryCell("$" + formatPrice(orderInfo.getShippingCost()), normalFont, true));
        }
        
        // Tax
        if (orderInfo.getTaxAmount() != null && orderInfo.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            summaryTable.addCell(createSummaryCell("Tax:", normalFont, false));
            summaryTable.addCell(createSummaryCell("$" + formatPrice(orderInfo.getTaxAmount()), normalFont, true));
        }
        
        // Discount
        if (orderInfo.getDiscountAmount() != null && orderInfo.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            summaryTable.addCell(createSummaryCell("Discount:", normalFont, false));
            summaryTable.addCell(createSummaryCell("-$" + formatPrice(orderInfo.getDiscountAmount()), 
                new Font(normalFont.getBaseFont(), normalFont.getSize(), Font.NORMAL, SUCCESS_GREEN), true));
        }
        
        // Points information for points and hybrid payments
        if (order.getOrderTransaction() != null) {
            Integer pointsUsed = order.getOrderTransaction().getPointsUsed();
            BigDecimal pointsValue = order.getOrderTransaction().getPointsValue();
            
            if (pointsUsed != null && pointsUsed > 0) {
                summaryTable.addCell(createSummaryCell("Points Used (" + pointsUsed + " pts):", normalFont, false));
                summaryTable.addCell(createSummaryCell("-$" + formatPrice(pointsValue), 
                    new Font(normalFont.getBaseFont(), normalFont.getSize(), Font.NORMAL, SUCCESS_GREEN), true));
            }
        }
        
        // Add separator line
        PdfPCell separatorLeft = new PdfPCell();
        separatorLeft.setBorder(Rectangle.TOP);
        separatorLeft.setBorderColor(TABLE_BORDER);
        separatorLeft.setBorderWidth(1);
        separatorLeft.setPadding(5);
        separatorLeft.setPhrase(new Phrase(""));
        
        PdfPCell separatorRight = new PdfPCell();
        separatorRight.setBorder(Rectangle.TOP);
        separatorRight.setBorderColor(TABLE_BORDER);
        separatorRight.setBorderWidth(1);
        separatorRight.setPadding(5);
        separatorRight.setPhrase(new Phrase(""));
        
        summaryTable.addCell(separatorLeft);
        summaryTable.addCell(separatorRight);
        
        // Total
        summaryTable.addCell(createSummaryCell("Order Total:", totalFont, false));
        summaryTable.addCell(createSummaryCell("$" + formatPrice(orderInfo.getTotalAmount()), totalFont, true));
        
        document.add(summaryTable);
    }
    
    private void addPickupInformation(Document document, Order order, Font headerFont, Font boldFont, Font normalFont) throws DocumentException {
        String pickupToken = order.getPickupToken() != null ? order.getPickupToken() : 
                            "PICKUP-" + order.getOrderCode() + "-" + System.currentTimeMillis();
        
        Paragraph qrHeader = new Paragraph("Pickup Information", headerFont);
        qrHeader.setSpacingBefore(30);
        qrHeader.setSpacingAfter(15);
        document.add(qrHeader);
        
        // Create a bordered section for pickup info
        PdfPTable pickupTable = new PdfPTable(1);
        pickupTable.setWidthPercentage(60);
        pickupTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        pickupTable.setSpacingAfter(20);
        
        PdfPCell pickupCell = new PdfPCell();
        pickupCell.setBorderColor(TABLE_BORDER);
        pickupCell.setBorderWidth(1);
        pickupCell.setPadding(20);
        pickupCell.setBackgroundColor(new BaseColor(248, 249, 250));
        pickupCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        // Add QR code
        try {
            byte[] qrCodeBytes = qrCodeService.generateOrderTrackingQR(order.getOrderCode(), pickupToken);
            Image qrImage = Image.getInstance(qrCodeBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            qrImage.scaleToFit(120, 120);
            pickupCell.addElement(qrImage);
            
            Paragraph spacer = new Paragraph(" ");
            spacer.setSpacingAfter(10);
            pickupCell.addElement(spacer);
        } catch (Exception e) {
            log.warn("Failed to add QR code to PDF: {}", e.getMessage());
        }
        
        // Token information
        Paragraph tokenLabel = new Paragraph("Pickup Token:", normalFont);
        tokenLabel.setAlignment(Element.ALIGN_CENTER);
        pickupCell.addElement(tokenLabel);
        
        Paragraph tokenValue = new Paragraph(pickupToken, boldFont);
        tokenValue.setAlignment(Element.ALIGN_CENTER);
        tokenValue.setSpacingAfter(10);
        pickupCell.addElement(tokenValue);
        
        Paragraph instructions = new Paragraph("Present this code or scan the QR code when collecting your order.", normalFont);
        instructions.setAlignment(Element.ALIGN_CENTER);
        pickupCell.addElement(instructions);
        
        pickupTable.addCell(pickupCell);
        document.add(pickupTable);
    }
    
    private void addFooter(Document document, Font normalFont, Font mutedFont) throws DocumentException {
        Paragraph thankYou = new Paragraph("Thank you for choosing ShopSphere!", normalFont);
        thankYou.setAlignment(Element.ALIGN_CENTER);
        thankYou.setSpacingBefore(30);
        thankYou.setSpacingAfter(10);
        document.add(thankYou);
        
        Paragraph contact = new Paragraph("Questions? Contact us at support@shopsphere.com or +250 788 123 456", mutedFont);
        contact.setAlignment(Element.ALIGN_CENTER);
        document.add(contact);
    }
    
    // Helper methods
    private void addSeparatorLine(Document document) throws DocumentException {
        PdfPTable separatorTable = new PdfPTable(1);
        separatorTable.setWidthPercentage(100);
        separatorTable.setSpacingAfter(20);
        
        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setBorder(Rectangle.BOTTOM);
        separatorCell.setBorderColor(TABLE_BORDER);
        separatorCell.setBorderWidth(1);
        separatorCell.setFixedHeight(1);
        separatorCell.setPhrase(new Phrase(""));
        
        separatorTable.addCell(separatorCell);
        document.add(separatorTable);
    }
    
    private PdfPCell createInfoCell(String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(TABLE_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(10);
        cell.setBackgroundColor(BaseColor.WHITE);
        
        Paragraph content = new Paragraph();
        content.add(new Chunk(label + "\n", labelFont));
        content.add(new Chunk(value, valueFont));
        cell.addElement(content);
        
        return cell;
    }
    
    private PdfPCell createStyledHeaderCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setBackgroundColor(TABLE_HEADER);
        cell.setBorderColor(TABLE_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }
    
    private PdfPCell createStyledDataCell(String content, Font font, BaseColor backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setBackgroundColor(backgroundColor);
        cell.setBorderColor(TABLE_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }
    
    private PdfPCell createSummaryCell(String content, Font font, boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignRight ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        return cell;
    }
    
    private String getProductDisplayName(OrderItem item) {
        String productName = "";
        if (item.isVariantBased() && item.getProductVariant() != null) {
            productName = item.getProductVariant().getProduct().getProductName();
            if (item.getProductVariant().getVariantName() != null) {
                productName += " (" + item.getProductVariant().getVariantName() + ")";
            }
        } else if (item.getProduct() != null) {
            productName = item.getProduct().getProductName();
        }
        return productName;
    }
    
    private String formatOrderStatus(String status) {
        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
    }
    
    private String formatPrice(BigDecimal price) {
        return String.format("%.2f", price);
    }
    
    private DiscountInfo calculateDiscountInfo(OrderItem item) {
        DiscountInfo info = new DiscountInfo();
        
        if (item.isVariantBased() && item.getProductVariant() != null) {
            ProductVariant variant = item.getProductVariant();
            info.originalPrice = variant.getPrice();
            info.paidPrice = item.getPrice();
            
            // Check if there was a discount applied
            if (variant.hasActiveDiscount()) {
                BigDecimal discountAmount = info.originalPrice.subtract(info.paidPrice);
                if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                    info.hasDiscount = true;
                    BigDecimal discountPercentage = discountAmount
                        .divide(info.originalPrice, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    info.discountText = String.format("%.0f%% OFF", discountPercentage);
                }
            }
        } else if (item.getProduct() != null) {
            Product product = item.getProduct();
            info.originalPrice = product.getPrice();
            info.paidPrice = item.getPrice();
            
            // Check if there was a discount applied
            if (product.hasActiveDiscount()) {
                BigDecimal discountAmount = info.originalPrice.subtract(info.paidPrice);
                if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                    info.hasDiscount = true;
                    BigDecimal discountPercentage = discountAmount
                        .divide(info.originalPrice, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    info.discountText = String.format("%.0f%% OFF", discountPercentage);
                }
            }
        } else {
            // Fallback if no product/variant reference
            info.originalPrice = item.getPrice();
            info.paidPrice = item.getPrice();
        }
        
        return info;
    }
    
    // Helper class for discount information
    private static class DiscountInfo {
        BigDecimal originalPrice = BigDecimal.ZERO;
        BigDecimal paidPrice = BigDecimal.ZERO;
        boolean hasDiscount = false;
        String discountText = "";
    }
}