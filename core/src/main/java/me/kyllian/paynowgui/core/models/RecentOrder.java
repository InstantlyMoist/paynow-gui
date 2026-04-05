package me.kyllian.paynowgui.core.models;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class RecentOrder {

    private RecentOrderCustomer customer;
    private List<RecentOrderLine> lines;

    @SerializedName("total_amount")
    private double totalAmount;

    @SerializedName("total_amount_str")
    private String totalAmountStr;

    @Data
    public static class RecentOrderCustomer {
        private String name;

        @SerializedName("minecraft_uuid")
        private String minecraftUuid;
    }

    @Data
    public static class RecentOrderLine {
        @SerializedName("product_name")
        private String productName;

        private double price;

        @SerializedName("total_amount")
        private double totalAmount;

        private double quantity;
    }
}
