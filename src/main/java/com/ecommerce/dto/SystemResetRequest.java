package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemResetRequest {
    
    @NotNull(message = "At least one deletion option must be specified")
    private Boolean deleteProducts;
    
    @NotNull(message = "At least one deletion option must be specified")
    private Boolean deleteDiscounts;
    
    @NotNull(message = "At least one deletion option must be specified")
    private Boolean deleteOrders;
    
    @NotNull(message = "At least one deletion option must be specified")
    private Boolean deleteRewardSystems;
    
    @NotNull(message = "At least one deletion option must be specified")
    private Boolean deleteShippingCosts;
    
    @NotNull(message = "At least one deletion option must be specified")
    private Boolean deleteMoneyFlows;
    
    @NotNull(message = "At least one deletion option must be specified")
    private Boolean deleteCategories;
    
    @NotNull(message = "At least one deletion option must be specified")
    private Boolean deleteBrands;
    
    @NotNull(message = "At least one deletion option must be specified")
    private Boolean deleteWarehouses;
    
    /**
     * Validates that at least one deletion option is selected
     */
    public boolean hasAtLeastOneSelection() {
        return (deleteProducts != null && deleteProducts) ||
               (deleteDiscounts != null && deleteDiscounts) ||
               (deleteOrders != null && deleteOrders) ||
               (deleteRewardSystems != null && deleteRewardSystems) ||
               (deleteShippingCosts != null && deleteShippingCosts) ||
               (deleteMoneyFlows != null && deleteMoneyFlows) ||
               (deleteCategories != null && deleteCategories) ||
               (deleteBrands != null && deleteBrands) ||
               (deleteWarehouses != null && deleteWarehouses);
    }
}
