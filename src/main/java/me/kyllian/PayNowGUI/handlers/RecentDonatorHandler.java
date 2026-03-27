package me.kyllian.PayNowGUI.handlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gg.paynow.sdk.PayNowClient;
import gg.paynow.sdk.storefront.api.ModulesApi;
import gg.paynow.sdk.storefront.model.ModuleDto;
import me.kyllian.PayNowGUI.PayNowGUIPlugin;
import me.kyllian.PayNowGUI.hooks.npc.INpcHook;
import me.kyllian.PayNowGUI.models.RecentOrder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

import static me.kyllian.PayNowGUI.utils.StringUtils.colorize;

public class RecentDonatorHandler {

    private final PayNowGUIPlugin plugin;
    private final INpcHook npcHook;
    private final Gson gson = new Gson();
    private int taskId = -1;

    public RecentDonatorHandler(PayNowGUIPlugin plugin, INpcHook npcHook) {
        this.plugin = plugin;
        this.npcHook = npcHook;

        start();
    }

    public void start() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("recent_donator_npc");
        if (config == null || !config.getBoolean("enabled", false)) return;

        long intervalTicks = config.getInt("update_every", 60) * 20L;
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::fetchAndUpdate, 0L, intervalTicks).getTaskId();
    }

    private void fetchAndUpdate() {
        try {
            String storeId = plugin.getConfig().getString("store_identifier");
            PayNowClient client = PayNowClient.forStorefront(storeId);

            ModulesApi modulesApi = client.getStorefrontApi(ModulesApi.class);
            List<ModuleDto> modules = modulesApi.getPreparedModules(storeId);

            for (ModuleDto module : modules) {
                if (!module.getId().toString().equals("recent_payments")) continue;

                List<RecentOrder> orders = gson.fromJson(
                        gson.toJsonTree(module.getData().getOrders()),
                        new TypeToken<List<RecentOrder>>() {}.getType()
                );
                if (orders == null || orders.isEmpty()) return;

                RecentOrder mostRecent = orders.getFirst();
                String customerName = mostRecent.getCustomer() != null
                        ? mostRecent.getCustomer().getName()
                        : "Unknown";

                ConfigurationSection config = plugin.getConfig().getConfigurationSection("recent_donator_npc");
                int npcId = config.getInt("npc_id", 99);
                String hologramTemplate = config.getString("hologram", "");
                String packageFormat = config.getString("package_format", "&8- &7%name% &d&l%amount%");

                StringBuilder packagesBuilder = new StringBuilder();
                List<RecentOrder.RecentOrderLine> lines = mostRecent.getLines();
                if (lines != null) {
                    for (int i = 0; i < lines.size(); i++) {
                        RecentOrder.RecentOrderLine line = lines.get(i);
                        String name = line.getProductName() != null ? line.getProductName() : "Unknown";
                        String amount = String.format("$%.2f", line.getPrice() / 100.0);
                        packagesBuilder.append(packageFormat
                                .replace("%name%", name)
                                .replace("%amount%", amount));
                        if (i < lines.size() - 1) packagesBuilder.append("\n");
                    }
                }

                // Build the total from the order
                String total = mostRecent.getTotalAmountStr() != null
                        ? mostRecent.getTotalAmountStr()
                        : String.format("$%.2f", mostRecent.getTotalAmount() / 100.0);

                // Replace placeholders in the hologram template
                String hologramText = hologramTemplate
                        .replace("%packages%", packagesBuilder.toString())
                        .replace("%name%", customerName)
                        .replace("%total%", total);

                // Split into individual lines and colorize
                List<String> hologramLines = new ArrayList<>();
                for (String hologramLine : hologramText.split("\n")) {
                    hologramLines.add(colorize(hologramLine));
                }

                // Update the NPC on the main thread (Citizens requires main thread access)
                Bukkit.getScheduler().runTask(plugin, () -> npcHook.updateNpc(npcId, customerName, hologramLines));
                return;
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }
}
