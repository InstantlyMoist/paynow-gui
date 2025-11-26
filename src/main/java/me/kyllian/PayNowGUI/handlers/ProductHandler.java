package me.kyllian.PayNowGUI.handlers;

import gg.paynow.sdk.PayNowClient;
import gg.paynow.sdk.storefront.api.CartApi;
import gg.paynow.sdk.storefront.api.CheckoutApi;
import gg.paynow.sdk.storefront.api.CustomerApi;
import gg.paynow.sdk.storefront.api.ProductsApi;
import gg.paynow.sdk.storefront.client.ApiException;
import gg.paynow.sdk.storefront.model.*;
import lombok.Getter;
import me.kyllian.PayNowGUI.PayNowGUIPlugin;
import me.kyllian.PayNowGUI.models.GUIProduct;
import me.kyllian.PayNowGUI.utils.YMLFile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

import static org.bukkit.Bukkit.getScheduler;

@Getter
public class ProductHandler extends YMLFile<PayNowGUIPlugin> {

    private final String storeId;
    private final PayNowClient client;

    private final List<ProductTagDto> tags = new LinkedList<>();
    private final List<StorefrontProductDto> products = new LinkedList<>();
    private final HashMap<Object, GUIProduct> guiProductMap = new HashMap<>(); // Map slot to GUIProduct (Display data)

    private final Map<UUID, String> customerTokens = new HashMap<>();

    public ProductHandler(PayNowGUIPlugin plugin) {
        super(plugin, "products.yml");
        this.storeId = plugin.getConfig().getString("store_identifier");
        this.client = PayNowClient.forStorefront(this.storeId);

        loadProducts();
    }

    public void loadProducts() {
        try {
            ProductsApi productsApi = client.getStorefrontApi(ProductsApi.class);
            this.products.addAll(productsApi.getStorefrontProducts(storeId, null, null, null, "en-US"));

            guiProductMap.clear();

            this.products.forEach(p -> {
                // Upsert GUIProduct data
                if (getFileConfiguration().get(p.getId().toString()) != null) {
                    String displayName = getFileConfiguration().getString(p.getId().toString() + ".display_name", "&a" + p.getName());
                    String materialName = getFileConfiguration().getString(p.getId().toString() + ".material", "STONE");
                    GUIProduct guiProduct = new GUIProduct(displayName, Material.valueOf(materialName));
                    guiProductMap.put(p.getId(), guiProduct);
                } else {
                    GUIProduct product = new GUIProduct(p);
                    guiProductMap.put(p.getId(), product);

                    getFileConfiguration().set(p.getId().toString() + ".display_name", product.getDisplayName());
                    getFileConfiguration().set(p.getId().toString() + ".material", product.getMaterial().name());
                }

                p.getTags().forEach(t -> {
                    if (this.tags.stream().noneMatch(existingTag -> existingTag.getId().equals(t.getId()))) {
                        this.tags.add(t);
                    }
                });

            });

            save();
            Bukkit.getLogger().info("[paynow-gui] Cached " + products.size() + " products and " + tags.size() + " tags.");
        } catch (ApiException exception) {
            exception.printStackTrace();
        }
    }

    public void getProducts(Player player, Consumer<List<StorefrontProductDto>> successCallback, Consumer<Exception> errorCallback) {
        if (!customerTokens.containsKey(player.getUniqueId())) {
            consumeOnMain(errorCallback, new IllegalStateException("Player is not authenticated"));
            return;
        }
        getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            PayNowClient authenticatedClient = PayNowClient.forStorefrontWithAuth(storeId, "customer " + customerTokens.get(player.getUniqueId()));
            ProductsApi productsApi = authenticatedClient.getStorefrontApi(ProductsApi.class);
            try {
                List<StorefrontProductDto> products = productsApi.getStorefrontProducts(storeId, null, null, player.getAddress().getHostName(), "en-US");
                consumeOnMain(successCallback, products);
            } catch (Exception e) {
                consumeOnMain(errorCallback, e);
            }
        });
    }

    public void authenticate(Player player, Consumer<String> successCallback, Consumer<Exception> errorCallback) {
        if (customerTokens.containsKey(player.getUniqueId())) {
            consumeOnMain(successCallback, customerTokens.get(player.getUniqueId()));
            return;
        }

        getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            CustomerApi customerApi = client.getStorefrontApi(CustomerApi.class);
            try {
                AuthenticateStorefrontCustomerResponseDto response = customerApi.authenticateStorefrontCustomer(storeId, null, "en-US",
                        new AuthenticateStorefrontCustomerRequestDto()
                                .platform(CustomerProfilePlatform.MINECRAFT)
                                .id(player.getName())
                );
                customerTokens.put(player.getUniqueId(), response.getCustomerToken());
                consumeOnMain(successCallback, response.getCustomerToken());
            } catch (Exception e) {
                e.printStackTrace();
                consumeOnMain(errorCallback, e);
            }
        });
    }

    public void getCart(Player player, Consumer<CartDto> successCallback, Consumer<Exception> errorCallback) {
        getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            PayNowClient authenticatedClient = PayNowClient.forStorefrontWithAuth(storeId, "customer " + customerTokens.get(player.getUniqueId()));
            CartApi cartApi = authenticatedClient.getStorefrontApi(CartApi.class);
            try {
                CartDto cart = cartApi.getCart(null, player.getAddress().getHostName(), "en-US");
                consumeOnMain(successCallback, cart);
            } catch (Exception e) {
                consumeOnMain(errorCallback, e);
            }
        });
    }

    public void setProductQuantityInCart(Player player, Object gameServerId, Object productId, int quantity, Consumer<Void> successCallback, Consumer<Exception> errorCallback) {
        if (!customerTokens.containsKey(player.getUniqueId())) {
            consumeOnMain(errorCallback, new IllegalStateException("Player is not authenticated"));
            return;
        }

        getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            PayNowClient authenticatedClient = PayNowClient.forStorefrontWithAuth(storeId, "customer " + customerTokens.get(player.getUniqueId()));
            CartApi cartApi = authenticatedClient.getStorefrontApi(CartApi.class);
            try {
                HashMap<String, Object> prodIdMap = new HashMap<>();
                prodIdMap.put("product_id", productId);

                HashMap<String, Object> gameServerIdMap = new HashMap<>();
                gameServerIdMap.put("gameserver_id", gameServerId);

                cartApi.addLine(prodIdMap, quantity, null, null, gameServerIdMap, null, null, null, null, null, player.getAddress().getHostName(), null);
                consumeOnMain(successCallback, null);
            } catch (Exception e) {
                e.printStackTrace();
                consumeOnMain(errorCallback, e);
            }
        });
    }

    public void clearCart(Player player, Consumer<Void> successCallback, Consumer<Exception> errorCallback) {
        if (!customerTokens.containsKey(player.getUniqueId())) {
            consumeOnMain(errorCallback, new IllegalStateException("Player is not authenticated"));
            return;
        }

        getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            PayNowClient authenticatedClient = PayNowClient.forStorefrontWithAuth(storeId, "customer " + customerTokens.get(player.getUniqueId()));
            CartApi cartApi = authenticatedClient.getStorefrontApi(CartApi.class);
            try {
                cartApi.clearCart(player.getAddress().getHostName(), null);
                consumeOnMain(successCallback, null);
            } catch (Exception e) {
                e.printStackTrace();
                consumeOnMain(errorCallback, e);
            }
        });
    }

    public void createCheckout(Player player, Consumer<CreateCheckoutSessionResponseDto> successCallback, Consumer<Exception> errorCallback) {
        if (!customerTokens.containsKey(player.getUniqueId())) {
            consumeOnMain(errorCallback, new IllegalStateException("Player is not authenticated"));
            return;
        }

        getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            PayNowClient authenticatedClient = PayNowClient.forStorefrontWithAuth(storeId, "customer " + customerTokens.get(player.getUniqueId()));
            CartApi cartApi = authenticatedClient.getStorefrontApi(CartApi.class);

            try {
                CreateCartCheckoutSessionDto request = new CreateCartCheckoutSessionDto()
                        .returnUrl(getPlugin().getConfig().getString("return_url"))
                        .cancelUrl(getPlugin().getConfig().getString("cancel_url"));

                CreateCheckoutSessionResponseDto response = cartApi.createCartCheckout(player.getAddress().getHostName(), null, request);
                consumeOnMain(successCallback, response);
            } catch (Exception e) {
                e.printStackTrace();
                consumeOnMain(errorCallback, e);
            }
        });
    }

    private <T> void consumeOnMain(Consumer<T> consumer, T param) {
        if (consumer == null) return;
        getScheduler().runTask(getPlugin(), () -> consumer.accept(param));
    }
}
