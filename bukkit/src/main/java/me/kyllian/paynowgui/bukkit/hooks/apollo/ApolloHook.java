package me.kyllian.paynowgui.bukkit.hooks.apollo;

import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.module.paynow.PayNowEmbeddedCheckoutSupport;
import com.lunarclient.apollo.module.paynow.PayNowModule;
import com.lunarclient.apollo.player.ApolloPlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ApolloHook implements IApolloHook {

    private final PayNowModule payNowModule;

    public ApolloHook() {
        this.payNowModule = Apollo.getModuleManager().getModule(PayNowModule.class);
    }

    @Override
    public boolean isLunar(Player player) {
        ApolloPlayerManager pm = Apollo.getPlayerManager();
        UUID id = player.getUniqueId();

        if (!pm.hasSupport(id)) return false;
        return pm.getPlayer(id)
                .map(p -> p.getPayNowEmbeddedCheckoutSupport() != PayNowEmbeddedCheckoutSupport.UNSUPPORTED)
                .orElse(false);
    }

    @Override
    public void checkout(Player player, String checkoutToken) {
        this.payNowModule.displayPayNowEmbeddedCheckout(Apollo.getPlayerManager().getPlayer(player.getUniqueId()).get(), checkoutToken);
    }
}
