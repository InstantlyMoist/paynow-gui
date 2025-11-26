package me.kyllian.PayNowGUI.gui;

import gg.paynow.sdk.storefront.model.CartLineDto;
import gg.paynow.sdk.storefront.model.ProductTagDto;
import gg.paynow.sdk.storefront.model.StorefrontProductDto;
import me.kyllian.PayNowGUI.PayNowGUIPlugin;
import me.kyllian.PayNowGUI.models.GUIPayload;
import me.kyllian.PayNowGUI.models.GUIProduct;
import me.kyllian.PayNowGUI.utils.BasicInventory;
import me.kyllian.PayNowGUI.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedList;
import java.util.List;

import static org.bukkit.Bukkit.getScheduler;

public class ProductsGUI extends BasicInventory<PayNowGUIPlugin> {

    private final Player player;
    private final GUIPayload payload;

    private final ProductTagDto tag;
    private final List<StorefrontProductDto> products = new LinkedList<>();

    private boolean loading = false;

    public ProductsGUI(PayNowGUIPlugin plugin, Player player, GUIPayload payload, ProductTagDto tag) {
        super(plugin, plugin.getConfig(), "products_gui");
        this.player = player;
        this.payload = payload;
        this.tag = tag;

        this.products.addAll(payload.getAllProducts().stream()
                .filter(p -> p.getTags().stream()
                        .anyMatch(t -> t.getId().equals(tag.getId()))).toList());
    }

    @Override
    public String titlePlaceholders(String title) {
        return title.replace("%tag%", tag.getName());
    }

    @Override
    public int getSlots() {
        return ((products.size() - 1) / 9 + 1) * 9 + 9;
    }

    @Override
    public List<Integer> getBackSlots() {
        return List.of(inventory.getSize() - 5);
    }

    @Override
    public void buildInventory() {
        for (int i = 0; i < products.size(); i++) {
            StorefrontProductDto product = products.get(i);
            GUIProduct guiProduct = plugin.getProductHandler().getGuiProductMap().getOrDefault(product.getId(), new GUIProduct(product));

            CartLineDto productInCart = payload.getCart().getLines().stream()
                    .filter(line -> line.getProductId().equals(product.getId()))
                    .findFirst()
                    .orElse(null);

            boolean fullFilled = productInCart != null && product.getStock() != null && product.getStock().getCustomerAvailable() != -1 && productInCart.getQuantity() >= product.getStock().getCustomerAvailable();
            int inCart = productInCart != null ? productInCart.getQuantity() : 0;

            Material material = fullFilled ? Material.valueOf(getSection().getString("item.material_max_quantity")) : guiProduct.getMaterial();
            if (!product.getSingleGameServerOnly()) material = Material.GOLD_BLOCK;

            ItemStack productItem = new ItemBuilder(material, Math.max(1, Math.min(inCart, material.getMaxStackSize())))
                    .setName(guiProduct.getDisplayName())
                    .setLore(getSection().getString("item.lore")
                            .replace("%price%", String.format("%.2f", product.getPrice() / 100.0))
                            .replace("%amount%", String.valueOf(inCart))
                    )
                    .setEnchanted(inCart != 0)
                    .toItemStack();

            addItem(i, productItem, (e) -> {
                if (loading) return;

                Object gameServerId = null;
                if (product.getSingleGameServerOnly()) {
                    Bukkit.broadcastMessage("single game server only!");
                    if (inCart == 0) {
                        loading = true;
                        player.openInventory(new SelectServerGUI(plugin, player, payload, product).getInventory());
                        return;
                    }
                    // It's in cart, get the product in cart, and the game ID
                    CartLineDto line = payload.getCart().getLines().stream()
                            .filter(l -> l.getProductId().equals(product.getId()))
                            .findFirst()
                            .orElse(null);
                    gameServerId = line != null ? line.getSelectedGameserverId() : null;
                }

                if (e.getClick() == ClickType.LEFT && !fullFilled) setQuantity(product, inCart + 1, gameServerId);
                else if (e.getClick() == ClickType.SHIFT_RIGHT && inCart != 0) setQuantity(product, 0, gameServerId);
                else if (e.getClick() == ClickType.RIGHT && inCart != 0) setQuantity(product, inCart - 1, gameServerId);
            });
        }
    }

    private void setQuantity(StorefrontProductDto product, int quantity, Object gameServerId) {
        loading = true;
        plugin.getProductHandler().setProductQuantityInCart(player, gameServerId, product.getId(), quantity, (nothing) -> {
            plugin.getProductHandler().getCart(player, (fetchedCart) -> {
                this.payload.setCart(fetchedCart);
                player.openInventory(new ProductsGUI(plugin, player, payload, tag).getInventory());
            }, null);
        }, null);
    }

    @Override
    public void onBack(Player player) {
        player.closeInventory();
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (loading) return; // Redraw of the GUI
        getScheduler().runTaskLater(plugin, () -> player.openInventory(new TagsGUI(plugin, player, payload).getInventory()), 1L);
    }
}
