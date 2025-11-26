package me.kyllian.PayNowGUI.models;

import gg.paynow.sdk.storefront.model.CartDto;
import gg.paynow.sdk.storefront.model.ProductTagDto;
import gg.paynow.sdk.storefront.model.StorefrontProductDto;
import lombok.Data;
import lombok.Setter;

import java.util.LinkedList;

@Data
public class GUIPayload {

    private final LinkedList<StorefrontProductDto> allProducts;
    private final LinkedList<ProductTagDto> allTags;

    @Setter
    private CartDto cart;

    public GUIPayload() {
        this.allProducts = new LinkedList<>();
        this.allTags = new LinkedList<>();

        this.cart = null;
    }
}
