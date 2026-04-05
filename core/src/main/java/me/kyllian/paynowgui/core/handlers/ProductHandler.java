package me.kyllian.paynowgui.core.handlers;

import com.google.gson.JsonParser;
import gg.paynow.sdk.PayNowClient;
import gg.paynow.sdk.storefront.api.CartApi;
import gg.paynow.sdk.storefront.api.CustomerApi;
import gg.paynow.sdk.storefront.api.ProductsApi;
import gg.paynow.sdk.storefront.client.ApiException;
import gg.paynow.sdk.storefront.model.*;
import lombok.Getter;
import me.kyllian.paynowgui.core.models.GUIProduct;
import me.kyllian.paynowgui.core.platform.PayNowPlatform;
import me.kyllian.paynowgui.core.platform.PlatformPlayer;
import me.kyllian.paynowgui.core.utils.Statistics;
import me.kyllian.paynowgui.core.utils.YMLFile;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static me.kyllian.paynowgui.core.utils.StringUtils.colorize;

@Getter
public class ProductHandler {

    private final PayNowPlatform platform;
    private final YMLFile productsFile;

    private boolean debug;
    private String storeId;
    private PayNowClient client;

    private final HashMap<Object, GUIProduct> guiProductMap = new HashMap<>();
    private final Map<UUID, String> customerTokens = new HashMap<>();

    public ProductHandler(PayNowPlatform platform) {
        this.platform = platform;
        this.productsFile = new YMLFile(platform.getDataFolder(), "products.yml");

        loadProducts();
    }

    public void loadProducts() {
        this.debug = platform.getConfigBoolean("debug", false);
        this.storeId = platform.getConfigString("store_identifier");
        this.client = PayNowClient.forStorefront(this.storeId);

        try {
            ProductsApi productsApi = client.getStorefrontApi(ProductsApi.class);
            List<StorefrontProductDto> products = new ArrayList<>(productsApi.getStorefrontProducts(storeId, null, null, null, "en-US"));

            guiProductMap.clear();

            products.forEach(p -> {
                if (productsFile.has(p.getId().toString())) {
                    String displayName = productsFile.getString(p.getId().toString() + ".display_name", "&a" + p.getName());
                    String materialName = productsFile.getString(p.getId().toString() + ".material", "STONE");
                    int customModelData = productsFile.getInt(p.getId().toString() + ".custom_model_data", 0);
                    GUIProduct guiProduct = new GUIProduct(displayName, materialName, customModelData);
                    guiProductMap.put(p.getId(), guiProduct);
                } else {
                    GUIProduct product = new GUIProduct(p);
                    guiProductMap.put(p.getId(), product);

                    productsFile.set(p.getId().toString() + ".display_name", product.getDisplayName());
                    productsFile.set(p.getId().toString() + ".material", product.getMaterial());
                }
            });

            Statistics.products = products.size();
            Statistics.tags = products.stream()
                    .flatMap(p -> p.getTags().stream())
                    .collect(Collectors.toSet())
                    .size();

            productsFile.save();
            platform.getLogger().info("[paynow-gui] Cached " + products.size() + " products");
        } catch (ApiException exception) {
            if (debug) exception.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    private void runAsync(Runnable task) {
        platform.getScheduler().runAsync(task);
    }

    private <T> void withAuth(PlatformPlayer player, ThrowingFunction<PayNowClient, T> task, Consumer<T> onSuccess) {
        if (!hasToken(player)) {
            error(player);
            return;
        }
        runAsync(() -> {
            try {
                PayNowClient authClient = PayNowClient.forStorefrontWithAuth(storeId, "customer " + customerTokens.get(player.getUniqueId()));
                T result = task.apply(authClient);
                consumeOnMain(onSuccess, result);
            } catch (Exception e) {
                error(player);
                if (debug) e.printStackTrace();
            }
        });
    }

    public void getProducts(PlatformPlayer player, Consumer<List<StorefrontProductDto>> successCallback) {
        withAuth(player,
                c -> c.getStorefrontApi(ProductsApi.class).getStorefrontProducts(storeId, null, null, player.getHostName(), "en-US"),
                successCallback);
    }

    public void authenticate(PlatformPlayer player, Consumer<String> successCallback) {
        platform.getScheduler().runAsync(() -> {
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
                error(player);
                if (debug) e.printStackTrace();
            }
        });
    }

    public void getCart(PlatformPlayer player, Consumer<CartDto> successCallback) {
        withAuth(player,
                c -> c.getStorefrontApi(CartApi.class).getCart(null, player.getHostName(), "en-US"),
                successCallback);
    }

    public void setProductQuantityInCart(PlatformPlayer player, Object gameServerId, Object productId, int quantity, int delta, Consumer<Void> successCallback) {
        if (delta < 0) Statistics.productsRemoved.getAndAdd(Math.abs(delta));
        else Statistics.productsAdded.getAndAdd(delta);

        withAuth(player, c -> {
            CartApi cartApi = c.getStorefrontApi(CartApi.class);
            try {
                cartApi.addLine(productId.toString(), quantity, null, null, gameServerId != null ? gameServerId.toString() : null, null, null, null, null, null, player.getHostName(), null);
            } catch (ApiException e) {
                try {
                    String errorMessage = e.getResponseBody() != null
                            ? new JsonParser().parse(e.getResponseBody()).getAsJsonObject().get("message").getAsString()
                            : "An error occurred";
                    // Capitalize first letter
                    errorMessage = errorMessage.substring(0, 1).toUpperCase() + errorMessage.substring(1);
                    player.sendMessage(colorize("&c" + errorMessage));
                } catch (Exception ex) {
                    if (debug) ex.printStackTrace();
                }
            }

            return null;
        }, successCallback);
    }

    public void clearCart(PlatformPlayer player, Consumer<Void> successCallback) {
        withAuth(player, c -> {
            c.getStorefrontApi(CartApi.class).clearCart(player.getHostName(), null);
            return null;
        }, successCallback);
    }

    public void createCheckout(PlatformPlayer player, Consumer<CreateCheckoutSessionResponseDto> successCallback) {
        withAuth(player, c -> {
            CreateCartCheckoutSessionDto request = new CreateCartCheckoutSessionDto()
                    .returnUrl(platform.getConfigString("return_url"))
                    .cancelUrl(platform.getConfigString("cancel_url"));
            return c.getStorefrontApi(CartApi.class).createCartCheckout(player.getHostName(), null, request);
        }, successCallback);
    }

    public void reload() {
        productsFile.reload();
    }

    private boolean hasToken(PlatformPlayer player) {
        return player != null && customerTokens.containsKey(player.getUniqueId());
    }

    private <T> void consumeOnMain(Consumer<T> consumer, T param) {
        if (consumer == null) return;
        platform.getScheduler().runSync(() -> consumer.accept(param));
    }

    private void error(PlatformPlayer player) {
        if (player == null || !player.isOnline()) return;
        platform.getScheduler().runSync(() -> {
            player.closeGUI();
            player.sendMessage(colorize(platform.getConfigString("messages.error")));
        });
    }
}
