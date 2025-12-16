package me.kyllian.PayNowGUI.models;

import gg.paynow.sdk.storefront.model.StorefrontProductDto;
import lombok.Getter;
import org.bukkit.Material;

@Getter
public class GUIProduct {

    private final String displayName;
    private final Material material;
    private final int customModelData;

    public GUIProduct(StorefrontProductDto product) {
        // Default constructor
        this.displayName = "&a" + product.getName();
        this.material = Material.STONE;
        this.customModelData = 0;
    }

    public GUIProduct(String displayName, Material material, int customModelData) {
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
    }
}
