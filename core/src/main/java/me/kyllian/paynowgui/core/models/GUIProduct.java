package me.kyllian.paynowgui.core.models;

import gg.paynow.sdk.storefront.model.StorefrontProductDto;
import lombok.Getter;

/**
 * Platform-agnostic product display configuration.
 * Material is stored as a String so each platform can interpret it.
 */
@Getter
public class GUIProduct {

    private final String displayName;
    private final String material;
    private final int customModelData;

    public GUIProduct(StorefrontProductDto product) {
        this.displayName = "&a" + product.getName();
        this.material = "STONE";
        this.customModelData = 0;
    }

    public GUIProduct(String displayName, String material, int customModelData) {
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
    }
}
