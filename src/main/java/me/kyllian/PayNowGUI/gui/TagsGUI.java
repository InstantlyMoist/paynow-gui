package me.kyllian.PayNowGUI.gui;

import gg.paynow.sdk.storefront.model.ProductTagDto;
import me.kyllian.PayNowGUI.PayNowGUIPlugin;
import me.kyllian.PayNowGUI.models.GUIPayload;
import me.kyllian.PayNowGUI.utils.BasicInventory;
import me.kyllian.PayNowGUI.utils.ItemBuilder;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static me.kyllian.PayNowGUI.utils.StringUtils.colorize;

public class TagsGUI extends BasicInventory<PayNowGUIPlugin> {

    private final Player player;
    private final GUIPayload payload;
    private final boolean isLunar;

    private boolean loading = false;

    public TagsGUI(PayNowGUIPlugin plugin, Player player, GUIPayload payload) {
        super(plugin, plugin.getConfig(), "tags_gui");

        this.player = player;
        this.payload = payload;
        this.isLunar = plugin.getApolloHook().isLunar(player);

        plugin.getProductHandler().authenticate(player, (token) -> {
            if (inventory == null) return;
            // Load products...
            if (this.payload.getAllProducts().isEmpty()) {
                plugin.getProductHandler().getProducts(player, (products) -> {
                    this.payload.getAllProducts().addAll(products);
                    products.forEach(p -> p.getTags().forEach(t -> {
                        if (this.payload.getAllTags().stream().noneMatch(existingTag -> existingTag.getId().equals(t.getId())))
                            this.payload.getAllTags().add(t);
                    }));
                    drawProducts();
                }, null);
            } else drawProducts();

            plugin.getProductHandler().getCart(player, (fetchedCart) -> {
                if (inventory == null) return;
                this.payload.setCart(fetchedCart);
                drawCart();
            }, null);
        }, null);
    }

    @Override
    public void buildInventory() {
        drawCart();
    }

    public void drawProducts() {
        List<Integer> slots = getSection().getIntegerList("tag_item.slots");
        for (int i = 0; i < payload.getAllTags().size(); i++) {
            if (i >= slots.size()) break;
            int slot = slots.get(i);
            ProductTagDto tag = payload.getAllTags().get(i);

            ItemStack tagItem = new ItemBuilder(Material.valueOf(getSection().getString("tag_item.material")))
                    .setName(getSection().getString("tag_item.name").replace("%tag%", tag.getName()))
                    .toItemStack();

            addItem(slot, tagItem, (e) -> {
                if (payload.getAllProducts().isEmpty() || payload.getCart() == null) {
                    player.sendMessage(colorize(plugin.getConfig().getString("messages.wait")));
                    return;
                }
                player.openInventory(new ProductsGUI(plugin, player, payload, tag).getInventory());
            });
        }
    }

    public void drawCart() {
        int slot = getSection().getInt("checkout_item.slot");

        if (payload.getCart() == null) {
            ItemStack loadingItem = new ItemBuilder(Material.valueOf(getSection().getString("checkout_item.loading.material")))
                    .setName(getSection().getString("checkout_item.loading.name"))
                    .setLore(getSection().getString("checkout_item.loading.lore"))
                    .toItemStack();

            addItem(slot, loadingItem);
        } else if (payload.getCart().getLines().isEmpty()) {
            ItemStack emptyCartItem = new ItemBuilder(Material.valueOf(getSection().getString("checkout_item.empty.material")))
                    .setName(getSection().getString("checkout_item.empty.name"))
                    .setLore(getSection().getString("checkout_item.empty.lore"))
                    .toItemStack();

            addItem(slot, emptyCartItem);
        } else {
            String template = getSection().getString("checkout_item.line_template");

            StringBuilder itemsBuilder = new StringBuilder();
            payload.getCart().getLines().forEach(line -> {
                itemsBuilder.append(template.replace("%item%", line.getName())
                        .replace("%amount%", String.valueOf(line.getQuantity()))
                        .replace("%price%", String.format("%.2f", line.getPrice() / 100.0)))
                        .append("\n");
            });

            String items = itemsBuilder.toString().trim();

            ItemStack filledCartItem = new ItemBuilder(Material.valueOf(getSection().getString("checkout_item.filled.material")))
                    .setName(getSection().getString("checkout_item.filled.name"))
                    .setLore(getSection().getString("checkout_item.filled.lore")
                            .replace("%cartprice%", String. format("%.2f", payload.getCart().getTotal() / 100.0))
                            .replace("%items%", items))
                    .toItemStack();

            addItem(slot, filledCartItem, e -> {
                if (loading) {
                    player.sendMessage(colorize(plugin.getConfig().getString("messages.wait")));
                    return;
                }
                loading = true;

                plugin.getProductHandler().createCheckout(player, (checkout) -> {
                    player.closeInventory();
                    String msg = plugin.getConfig().getString("messages.checkout_website");
                    String[] parts = msg.split("%link%", -1);

                    TextComponent messageComp = new TextComponent(colorize(parts.length > 0 ? parts[0] : ""));

                    TextComponent linkComp = new TextComponent(colorize(getPlugin().getConfig().getString("messages.checkout_link")));
                    linkComp.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, checkout.getUrl()));

                    String hoverText = plugin.getConfig().getString("messages.checkout_hover", checkout.getUrl());
                    linkComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(colorize(hoverText)).create()));

                    messageComp.addExtra(linkComp);

                    if (parts.length > 1) messageComp.addExtra(new TextComponent(colorize(parts[1])));

                    player.spigot().sendMessage(messageComp);
                }, null);
            });

            // Clear cart item
            ItemStack clearCartItem = new ItemBuilder(Material.valueOf(getSection().getString("clear_cart_item.material")))
                    .setName(getSection().getString("clear_cart_item.name"))
                    .setLore(getSection().getString("clear_cart_item.lore"))
                    .toItemStack();

            addItem(getSlots(getSection().getString("clear_cart_item.slots", "0")), clearCartItem, (e) -> {
                if (loading) {
                    player.sendMessage(colorize(plugin.getConfig().getString("messages.wait")));
                    return;
                }
                loading = true;
                plugin.getProductHandler().clearCart(player, (nothing) -> {
                    if (inventory == null) return;
                    payload.getCart().getLines().clear();
                    player.openInventory(new TagsGUI(plugin, player, payload).getInventory());
                }, null);
            });
        }
    }
}
