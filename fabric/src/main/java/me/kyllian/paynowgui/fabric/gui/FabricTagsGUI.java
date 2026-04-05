package me.kyllian.paynowgui.fabric.gui;

import gg.paynow.sdk.storefront.model.ProductTagDto;
import gg.paynow.sdk.storefront.model.StorefrontProductDto;
import me.kyllian.paynowgui.core.models.GUIPayload;
import me.kyllian.paynowgui.core.utils.Statistics;
import me.kyllian.paynowgui.core.utils.StringUtils;
import me.kyllian.paynowgui.fabric.FabricPayNowMod;
import me.kyllian.paynowgui.fabric.platform.FabricPlayer;
import me.kyllian.paynowgui.fabric.utils.FabricColorTranslator;
import me.kyllian.paynowgui.fabric.utils.FabricItemBuilder;
import me.kyllian.paynowgui.fabric.utils.MaterialMapper;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;

public class FabricTagsGUI extends FabricBasicInventory {

    private final ServerPlayerEntity player;
    private final FabricPlayer platformPlayer;
    private final GUIPayload payload;

    private boolean loading = false;

    public FabricTagsGUI(FabricPayNowMod mod, ServerPlayerEntity player, GUIPayload payload) {
        super(mod, "tags_gui");
        this.player = player;
        this.platformPlayer = new FabricPlayer(player);
        this.payload = payload;

        // Authenticate and load products after the GUI is opened
        mod.getProductHandler().authenticate(platformPlayer, (token) -> {
            if (isClosed()) return;

            List<String> hiddenTags = mod.getPlatform().getConfig().getStringList("tag_item.hide");
            if (this.payload.getAllProducts().isEmpty()) {
                mod.getProductHandler().getProducts(platformPlayer, (products) -> {
                    long serverIdentifier = mod.getPlatform().getConfigInt("server_identifier", 0);

                    List<StorefrontProductDto> filtered = products;
                    if (serverIdentifier != 0) {
                        filtered = products.stream()
                                .filter(p -> p.getGameservers().stream()
                                        .anyMatch(s -> s.getId().equalsIgnoreCase(String.valueOf(serverIdentifier))))
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
                if (isClosed()) return;
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
                tagItem = new FabricItemBuilder(config.getString(sectionPath + "." + basePath + "material"))
                        .setName(config.getString(sectionPath + "." + basePath + "name", "").replace("%tag%", tag.getName()))
                        .setLore(config.getString(sectionPath + "." + basePath + "lore"))
                        .setCustomModelData(config.getInt(sectionPath + "." + basePath + "custom_model_data", 0))
                        .toItemStack();
            } else {
                tagItem = new FabricItemBuilder(cfgString("tag_item.material"))
                        .setName(cfgString("tag_item.name", "").replace("%tag%", tag.getName()))
                        .setCustomModelData(cfgInt("tag_item.custom_model_data", 0))
                        .toItemStack();
            }

            addItem(slot, tagItem, (clickPlayer, actionType) -> {
                if (payload.getAllProducts().isEmpty() || payload.getCart() == null) {
                    clickPlayer.sendMessage(FabricColorTranslator.toText(
                            StringUtils.colorize(mod.getPlatform().getConfigString("messages.wait"))));
                    return;
                }
                new FabricProductsGUI(mod, clickPlayer, payload, tag).open(clickPlayer);
            });
        }
    }

    public void drawCart() {
        int slot = cfgInt("checkout_item.slot");

        if (payload.getCart() == null) {
            ItemStack loadingItem = new FabricItemBuilder(cfgString("checkout_item.loading.material"))
                    .setName(cfgString("checkout_item.loading.name"))
                    .setLore(cfgString("checkout_item.loading.lore"))
                    .setCustomModelData(cfgInt("checkout_item.loading.custom_model_data", 0))
                    .toItemStack();
            addItem(slot, loadingItem);
        } else if (payload.getCart().getLines().isEmpty()) {
            ItemStack emptyCartItem = new FabricItemBuilder(cfgString("checkout_item.empty.material"))
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
            ItemStack filledCartItem = new FabricItemBuilder(cfgString(basePath + "material"))
                    .setName(cfgString(basePath + "name"))
                    .setCustomModelData(cfgInt(basePath + "custom_model_data", 0))
                    .setLore(cfgString(basePath + "lore", "")
                            .replace("%cartprice%", String.format("%.2f", payload.getCart().getTotal() / 100.0))
                            .replace("%items%", items))
                    .toItemStack();

            addItem(slot, filledCartItem, (clickPlayer, actionType) -> {
                if (loading) {
                    clickPlayer.sendMessage(FabricColorTranslator.toText(
                            StringUtils.colorize(mod.getPlatform().getConfigString("messages.wait"))));
                    return;
                }
                loading = true;

                mod.getProductHandler().createCheckout(platformPlayer, (checkout) -> {
                    clickPlayer.closeHandledScreen();
                    Statistics.cartsOpened.getAndIncrement();

                    String msg = mod.getPlatform().getConfigString("messages.checkout_website");
                    String[] parts = msg.split("%link%", -1);

                    MutableText messageComp = FabricColorTranslator.toText(parts.length > 0 ? parts[0] : "").copy();

                    String linkText = mod.getPlatform().getConfigString("messages.checkout_link");
                    MutableText linkComp = FabricColorTranslator.toText(linkText).copy();
                    linkComp.styled(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, checkout.getUrl()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    FabricColorTranslator.toText(mod.getPlatform().getConfigString("messages.checkout_hover", checkout.getUrl()))
                            ))
                    );

                    messageComp.append(linkComp);

                    if (parts.length > 1) {
                        messageComp.append(FabricColorTranslator.toText(parts[1]));
                    }

                    clickPlayer.sendMessage(messageComp);
                });
            });

            // Clear cart item
            ItemStack clearCartItem = new FabricItemBuilder(cfgString("clear_cart_item.material"))
                    .setName(cfgString("clear_cart_item.name"))
                    .setLore(cfgString("clear_cart_item.lore"))
                    .setCustomModelData(cfgInt("clear_cart_item.custom_model_data", 0))
                    .toItemStack();

            addItem(parseSlots(cfgString("clear_cart_item.slots", "0")), clearCartItem, (clickPlayer, actionType) -> {
                if (loading) {
                    clickPlayer.sendMessage(FabricColorTranslator.toText(
                            StringUtils.colorize(mod.getPlatform().getConfigString("messages.wait"))));
                    return;
                }
                loading = true;
                mod.getProductHandler().clearCart(platformPlayer, (nothing) -> {
                    if (isClosed()) return;
                    Statistics.cartsCleared.getAndIncrement();
                    payload.getCart().getLines().clear();
                    new FabricTagsGUI(mod, clickPlayer, payload).open(clickPlayer);
                });
            });
        }
    }
}
