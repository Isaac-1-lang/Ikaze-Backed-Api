# Multi-Vendor Architecture Documentation

## Overview
The application has been refactored to support a multi-vendor marketplace where a single customer order can contain items from multiple shops.

## Entity Relationships

### Before (Single Vendor)
```
Order (1) ──→ (N) OrderItem
```

### After (Multi-Vendor)
```
Order (1) ──→ (N) ShopOrder (1) ──→ (N) OrderItem
                     ↓
                   Shop
```

## Key Changes

### 1. Order Entity (`Order.java`)
**Removed:**
- `pickup_token` (moved to ShopOrder)
- `pickup_token_used` (moved to ShopOrder)

**Added:**
- `shopOrders` (List<ShopOrder>) - One-to-many relationship

**Purpose:**
- Represents the overall customer order
- Contains global order information (customer info, address, transaction)
- Aggregates data from multiple shop orders

### 2. ShopOrder Entity (`ShopOrder.java`)
**Key Fields:**
- `order` - Link to parent Order
- `shop` - Link to the Shop fulfilling this part of the order
- `items` - List of OrderItems for this shop
- `pickup_token` - Shop-specific pickup token
- `pickup_token_used` - Whether token has been used
- `status` - Shop-specific order status
- `shippingCost` - Shop-specific shipping cost
- `discountAmount` - Shop-specific discount
- `totalAmount` - Shop-specific total

**Purpose:**
- Represents a portion of an order fulfilled by a single shop
- Each shop has its own pickup token, status, and shipping details
- Enables independent fulfillment tracking per shop

### 3. OrderItem Entity (`OrderItem.java`)
**Changed:**
- Now links to `ShopOrder` instead of `Order`
- `@JoinColumn(name = "shop_order_id", nullable = false)`

**Purpose:**
- Represents individual products in a shop's portion of the order
- Grouped by shop through ShopOrder relationship

## Database Schema Changes

### order_items Table
**Removed:**
- `order_id` column (replaced by `shop_order_id`)

**Current Structure:**
```sql
order_items (
  order_item_id BIGSERIAL PRIMARY KEY,
  shop_order_id BIGINT NOT NULL REFERENCES shop_orders(shop_order_id),
  product_id UUID,
  variant_id UUID,
  quantity INTEGER NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
```

### orders Table
**Removed:**
- `pickup_token` column (moved to shop_orders)
- `pickup_token_used` column (moved to shop_orders)

**Current Structure:**
```sql
orders (
  order_id BIGSERIAL PRIMARY KEY,
  order_code VARCHAR(255) UNIQUE NOT NULL,
  order_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  user_id UUID,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
```

### shop_orders Table
**Structure:**
```sql
shop_orders (
  shop_order_id BIGSERIAL PRIMARY KEY,
  shop_order_code VARCHAR(255) UNIQUE NOT NULL,
  order_id BIGINT NOT NULL REFERENCES orders(order_id),
  shop_id UUID NOT NULL REFERENCES shops(shop_id),
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  pickup_token VARCHAR(255) UNIQUE NOT NULL,
  pickup_token_used BOOLEAN NOT NULL DEFAULT FALSE,
  shipping_cost DECIMAL(10,2) DEFAULT 0,
  discount_amount DECIMAL(10,2) DEFAULT 0,
  total_amount DECIMAL(10,2) DEFAULT 0,
  delivered_at TIMESTAMP,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
```

## Checkout Flow

### 1. Create Order
```java
Order order = new Order();
order.setUser(user);
order.setStatus("PENDING");
// Set order info, address, customer info, transaction
Order saved = orderRepository.save(order);
```

### 2. Group Items by Shop
```java
Map<Shop, List<CartItemDTO>> itemsByShop = new HashMap<>();
for (CartItemDTO item : req.getItems()) {
    Shop shop = getShopForCartItem(item);
    itemsByShop.computeIfAbsent(shop, k -> new ArrayList<>()).add(item);
}
```

### 3. Create ShopOrder for Each Shop
```java
for (Map.Entry<Shop, List<CartItemDTO>> entry : itemsByShop.entrySet()) {
    Shop shop = entry.getKey();
    List<CartItemDTO> shopItems = entry.getValue();
    
    ShopOrder shopOrder = new ShopOrder();
    shopOrder.setOrder(saved);
    shopOrder.setShop(shop);
    shopOrder.setStatus(ShopOrder.ShopOrderStatus.PROCESSING);
    
    // Create OrderItems for this shop
    for (CartItemDTO ci : shopItems) {
        OrderItem oi = new OrderItem();
        // Set product/variant, quantity, price
        oi.setShopOrder(shopOrder);
        shopOrder.getItems().add(oi);
    }
    
    // Calculate shop-specific totals
    shopOrder.setShippingCost(calculateShipping(shopItems));
    shopOrder.setDiscountAmount(calculateDiscount(shopItems));
    shopOrder.setTotalAmount(subtotal + shipping - discount);
    
    saved.addShopOrder(shopOrder);
    shopOrderRepository.save(shopOrder);
}
```

## Benefits

### 1. Independent Fulfillment
- Each shop can fulfill their portion independently
- Different pickup tokens per shop
- Separate status tracking per shop

### 2. Accurate Cost Calculation
- Shop-specific shipping costs
- Shop-specific discounts
- Per-shop reward points

### 3. Better Tracking
- Customer sees which shop is fulfilling which items
- Shops only see their own order portions
- Clear accountability per shop

### 4. Scalability
- Easy to add new shops
- No cross-shop dependencies
- Parallel order processing

## API Response Structure

### Order Response with Shop Orders
```json
{
  "orderId": 123,
  "orderCode": "ORD-1234567890-ABC123",
  "status": "PROCESSING",
  "totalAmount": 1500.00,
  "shopOrders": [
    {
      "shopOrderId": 1,
      "shopOrderCode": "SO-1234567890-XYZ",
      "shopName": "Electronics Store",
      "status": "PROCESSING",
      "pickupToken": "abc-123-def",
      "pickupTokenUsed": false,
      "shippingCost": 10.00,
      "totalAmount": 800.00,
      "items": [
        {
          "productName": "Laptop",
          "quantity": 1,
          "price": 750.00
        }
      ]
    },
    {
      "shopOrderId": 2,
      "shopOrderCode": "SO-1234567891-ABC",
      "shopName": "Fashion Store",
      "status": "SHIPPED",
      "pickupToken": "xyz-456-ghi",
      "pickupTokenUsed": false,
      "shippingCost": 5.00,
      "totalAmount": 700.00,
      "items": [
        {
          "productName": "T-Shirt",
          "quantity": 2,
          "price": 50.00
        }
      ]
    }
  ]
}
```

## Migration Required

**IMPORTANT:** Before deploying these changes, you must run the database migration to:
1. Remove `order_id` from `order_items` table
2. Remove `pickup_token` and `pickup_token_used` from `orders` table

See `RUN_MIGRATION.md` for detailed instructions.

## Testing Checklist

- [ ] Create order with items from single shop
- [ ] Create order with items from multiple shops
- [ ] Verify each shop has unique pickup token
- [ ] Verify shop-specific shipping costs
- [ ] Verify shop-specific order status
- [ ] Test guest checkout
- [ ] Test authenticated checkout
- [ ] Verify order tracking shows per-shop status
- [ ] Test order cancellation (should cancel all shop orders)
- [ ] Test partial returns (shop-specific)

## Future Enhancements

1. **Partial Delivery:** Allow customer to receive items from one shop while waiting for others
2. **Shop-Specific Delivery Windows:** Each shop sets their own delivery timeframes
3. **Independent Returns:** Process returns per shop without affecting other shops
4. **Shop Performance Metrics:** Track fulfillment speed, customer satisfaction per shop
5. **Dynamic Shipping:** Real-time shipping quotes per shop based on their location

