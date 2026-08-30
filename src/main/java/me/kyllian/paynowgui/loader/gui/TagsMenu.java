package me.kyllian.paynowgui.loader.gui;

import gg.paynow.sdk.storefront.model.ProductTagDto;
import gg.paynow.sdk.storefront.model.StorefrontProductDto;
import me.kyllian.paynowgui.core.models.GUIPayload;
import me.kyllian.paynowgui.core.utils.Statistics;
import me.kyllian.paynowgui.core.utils.StringUtils;
import me.kyllian.paynowgui.loader.PayNowLoaderMod;
import me.kyllian.paynowgui.loader.platform.LoaderPlayer;
import me.kyllian.paynowgui.loader.utils.ColorTranslator;
import me.kyllian.paynowgui.loader.utils.ItemBuilder;
import me.kyllian.paynowgui.loader.utils.MaterialMapper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TagsMenu extends BasicMenu {

    private final ServerPlayer player;
    private final LoaderPlayer platformPlayer;
    private final GUIPayload payload;

    private boolean loading = false;

    public TagsMenu(PayNowLoaderMod mod, ServerPlayer player, GUIPayload payload) {
        super(mod, "tags_gui");
        this.player = player;
        this.platformPlayer = new LoaderPlayer(player);
        this.payload = payload;

        // Authenticate and load products after the GUI is opened
        mod.getProductHandler().authenticate(platformPlayer, (token) -> {
            if (isClosed() || inventory == null) return;

            List<String> hiddenTags = cfgStringList("tag_item.hide");
            if (this.payload.getAllProducts().isEmpty()) {
                mod.getProductHandler().getProducts(platformPlayer, (products) -> {
                    long serverResourceLocation = mod.getPlatform().getConfigLong("server_identifier", 0L);

                    List<StorefrontProductDto> filtered = products;
                    if (serverResourceLocation != 0) {
                        filtered = products.stream()
                                .filter(p -> p.getGameservers().stream()
                                        .anyMatch(s -> s.getId().equalsIgnoreCase(String.valueOf(serverResourceLocation))))
                                .toList();
                    }

                    this.payload.getAllProducts().addAll(filtered);
                    filtered.forEach(p -> p.getTags().forEach(t -> {
                        if (this.payload.getAllTags().stream().noneMatch(existingTag -> existingTag.getId().equals(t.getId()))
                                && !hiddenTags.contains(t.getId()))
                            this.payload.getAllTags().add(t);
                    }));
                    drawProducts();
                });
            } else {
                drawProducts();
            }

            mod.getProductHandler().getCart(platformPlayer, (fetchedCart) -> {
                if (isClosed() || inventory == null) return;
                this.payload.setCart(fetchedCart);
                drawCart();
            });
        });
    }

    @Override
    public void buildInventory() {
        drawCart();
    }

    public void drawProducts() {
        List<Integer> slots = cfgIntList("tag_item.slots");
        for (int i = 0; i < payload.getAllTags().size(); i++) {
            if (i >= slots.size()) break;
            int slot = slots.get(i);
            ProductTagDto tag = payload.getAllTags().get(i);

            ItemStack tagItem;
            if (cfgHas("tag_item.overrides." + tag.getId())) {
                String basePath = "tag_item.overrides." + tag.getId() + ".";
                tagItem = new ItemBuilder(config.getString(sectionPath + "." + basePath + "material"))
                        .setName(config.getString(sectionPath + "." + basePath + "name", "").replace("%tag%", tag.getName()))
                        .setLore(config.getString(sectionPath + "." + basePath + "lore"))
                        .setCustomModelData(config.getInt(sectionPath + "." + basePath + "custom_model_data", 0))
                        .toItemStack();
            } else {
                tagItem = new ItemBuilder(cfgString("tag_item.material"))
                        .setName(cfgString("tag_item.name", "").replace("%tag%", tag.getName()))
                        .setCustomModelData(cfgInt("tag_item.custom_model_data", 0))
                        .toItemStack();
            }

            addItem(slot, tagItem, (clickPlayer, button, actionType) -> {
                if (payload.getAllProducts().isEmpty() || payload.getCart() == null) {
                    clickPlayer.sendSystemMessage(ColorTranslator.toText(
                            StringUtils.colorize(mod.getPlatform().getConfigString("messages.wait"))));
                    return;
                }
                new ProductsMenu(mod, clickPlayer, payload, tag).open(clickPlayer);
            });
        }
    }

    public void drawCart() {
        int slot = cfgInt("checkout_item.slot");

        if (payload.getCart() == null) {
            ItemStack loadingItem = new ItemBuilder(cfgString("checkout_item.loading.material"))
                    .setName(cfgString("checkout_item.loading.name"))
                    .setLore(cfgString("checkout_item.loading.lore"))
                    .setCustomModelData(cfgInt("checkout_item.loading.custom_model_data", 0))
                    .toItemStack();
            addItem(slot, loadingItem);
        } else if (payload.getCart().getLines().isEmpty()) {
            ItemStack emptyCartItem = new ItemBuilder(cfgString("checkout_item.empty.material"))
                    .setName(cfgString("checkout_item.empty.name"))
                    .setLore(cfgString("checkout_item.empty.lore"))
                    .setCustomModelData(cfgInt("checkout_item.empty.custom_model_data", 0))
                    .toItemStack();
            addItem(slot, emptyCartItem);
        } else {
            String template = cfgString("checkout_item.line_template");

            StringBuilder itemsBuilder = new StringBuilder();
            payload.getCart().getLines().forEach(line -> {
                itemsBuilder.append(template.replace("%item%", line.getName())
                                .replace("%amount%", String.valueOf(line.getQuantity()))
                                .replace("%price%", String.format("%.2f", line.getPrice() / 100.0)))
                        .append("\n");
            });
            String items = itemsBuilder.toString().trim();

            // No Lunar Client on Fabric, use the normal filled style
            String basePath = "checkout_item.filled.";
            ItemStack filledCartItem = new ItemBuilder(cfgString(basePath + "material"))
                    .setName(cfgString(basePath + "name"))
                    .setCustomModelData(cfgInt(basePath + "custom_model_data", 0))
                    .setLore(cfgString(basePath + "lore", "")
                            .replace("%cartprice%", String.format("%.2f", payload.getCart().getTotal() / 100.0))
                            .replace("%items%", items))
                    .toItemStack();

            addItem(slot, filledCartItem, (clickPlayer, button, actionType) -> {
                if (loading) {
                    clickPlayer.sendSystemMessage(ColorTranslator.toText(
                            StringUtils.colorize(mod.getPlatform().getConfigString("messages.wait"))));
                    return;
                }
                loading = true;

                mod.getProductHandler().createCheckout(platformPlayer, (checkout) -> {
                    clickPlayer.closeContainer();
                    Statistics.cartsOpened.getAndIncrement();

                    String msg = mod.getPlatform().getConfigString("messages.checkout_website");
                    String[] parts = msg.split("%link%", -1);

                    MutableComponent messageComp = ColorTranslator.toText(parts.length > 0 ? parts[0] : "").copy();

                    String linkText = mod.getPlatform().getConfigString("messages.checkout_link");
                    MutableComponent linkComp = ColorTranslator.toText(linkText).copy();
                    Component hoverComp = ColorTranslator.toText(
                            mod.getPlatform().getConfigString("messages.checkout_hover", checkout.getUrl()));
                    // ClickEvent/HoverEvent became sealed records in 1.21.5.
                    //? if >=1.21.5 {
                    /*linkComp.withStyle(style -> style
                            .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(checkout.getUrl())))
                            .withHoverEvent(new HoverEvent.ShowText(hoverComp))
                    );
                    *///?} else {
                    linkComp.withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, checkout.getUrl()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComp))
                    );
                    //?}

                    messageComp.append(linkComp);

                    if (parts.length > 1) {
                        messageComp.append(ColorTranslator.toText(parts[1]));
                    }

                    clickPlayer.sendSystemMessage(messageComp);
                });
            });

            // Clear cart item
            ItemStack clearCartItem = new ItemBuilder(cfgString("clear_cart_item.material"))
                    .setName(cfgString("clear_cart_item.name"))
                    .setLore(cfgString("clear_cart_item.lore"))
                    .setCustomModelData(cfgInt("clear_cart_item.custom_model_data", 0))
                    .toItemStack();

            addItem(parseSlots(cfgString("clear_cart_item.slots", "0")), clearCartItem, (clickPlayer, button, actionType) -> {
                if (loading) {
                    clickPlayer.sendSystemMessage(ColorTranslator.toText(
                            StringUtils.colorize(mod.getPlatform().getConfigString("messages.wait"))));
                    return;
                }
                loading = true;
                mod.getProductHandler().clearCart(platformPlayer, (nothing) -> {
                    if (isClosed()) return;
                    Statistics.cartsCleared.getAndIncrement();
                    payload.getCart().getLines().clear();
                    new TagsMenu(mod, clickPlayer, payload).open(clickPlayer);
                });
            });
        }
    }
}
