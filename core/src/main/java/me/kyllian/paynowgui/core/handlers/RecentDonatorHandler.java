package me.kyllian.paynowgui.core.handlers;

import gg.paynow.sdk.PayNowClient;
import gg.paynow.sdk.storefront.api.ModulesApi;
import gg.paynow.sdk.storefront.model.ModuleDto;
import gg.paynow.sdk.storefront.model.OrderDto;
import gg.paynow.sdk.storefront.model.OrderLineDto;
import me.kyllian.paynowgui.core.hooks.INpcHook;
import me.kyllian.paynowgui.core.platform.PayNowPlatform;

import java.util.ArrayList;
import java.util.List;

import static me.kyllian.paynowgui.core.utils.StringUtils.colorize;

public class RecentDonatorHandler {

    private final PayNowPlatform platform;
    private final INpcHook npcHook;
    private int taskId = -1;

    public RecentDonatorHandler(PayNowPlatform platform, INpcHook npcHook) {
        this.platform = platform;
        this.npcHook = npcHook;

        start();
    }

    public void start() {
        boolean enabled = platform.getConfigBoolean("recent_donator_npc.enabled", false);
        if (!enabled) return;

        long intervalTicks = platform.getConfigInt("recent_donator_npc.update_every", 60) * 20L;
        taskId = platform.getScheduler().runSyncRepeatingAsync(this::fetchAndUpdate, 0L, intervalTicks);
    }

    private void fetchAndUpdate() {
        try {
            String storeId = platform.getConfigString("store_identifier");
            PayNowClient client = PayNowClient.forStorefront(storeId);

            ModulesApi modulesApi = client.getStorefrontApi(ModulesApi.class);
            List<ModuleDto> modules = modulesApi.getPreparedModules(storeId);

            for (ModuleDto module : modules) {
                if (!module.getId().toString().equals("recent_payments")) continue;

                List<OrderDto> orders = module.getData().getOrders();
                if (orders == null || orders.isEmpty()) return;

                OrderDto mostRecent = orders.getFirst();
                String customerName = mostRecent.getCustomer() != null && mostRecent.getCustomer().getName() != null
                        ? mostRecent.getCustomer().getName()
                        : "Unknown";

                String npcId = platform.getConfigString("recent_donator_npc.npc_id", "99");
                String hologramTemplate = platform.getConfigString("recent_donator_npc.hologram", "");
                String packageFormat = platform.getConfigString("recent_donator_npc.package_format", "&8- &7%name% &d&l%amount%");

                StringBuilder packagesBuilder = new StringBuilder();
                List<OrderLineDto> lines = mostRecent.getLines();
                if (lines != null) {
                    for (int i = 0; i < lines.size(); i++) {
                        OrderLineDto line = lines.get(i);
                        String name = line.getProductName() != null ? line.getProductName() : "Unknown";
                        String amount = formatAmount(line.getPrice());
                        packagesBuilder.append(packageFormat
                                .replace("%name%", name)
                                .replace("%amount%", amount));
                        if (i < lines.size() - 1) packagesBuilder.append("\n");
                    }
                }

                String total = mostRecent.getTotalAmountStr() != null
                        ? mostRecent.getTotalAmountStr()
                        : formatAmount(mostRecent.getTotalAmount());

                String hologramText = hologramTemplate
                        .replace("%packages%", packagesBuilder.toString())
                        .replace("%name%", customerName)
                        .replace("%total%", total);

                List<String> hologramLines = new ArrayList<>();
                for (String hologramLine : hologramText.split("\n")) {
                    hologramLines.add(colorize(hologramLine));
                }

                // Update the NPC on the main thread
                platform.getScheduler().runSync(() -> npcHook.updateNpc(npcId, customerName, hologramLines));
                return;
            }
        } catch (Exception e) {
            if (platform.getConfigBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Amounts arrive in the smallest currency unit. Every field on the storefront models is
     * optional, so a missing amount falls back to zero rather than blowing up the update.
     */
    private String formatAmount(Integer amount) {
        return String.format("$%.2f", (amount != null ? amount : 0) / 100.0);
    }
}
