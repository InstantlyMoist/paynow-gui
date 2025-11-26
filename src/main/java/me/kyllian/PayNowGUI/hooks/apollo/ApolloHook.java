package me.kyllian.PayNowGUI.hooks.apollo;

import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.module.tebex.TebexEmbeddedCheckoutSupport;
import com.lunarclient.apollo.player.ApolloPlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ApolloHook implements IApolloHook {

    @Override
    public boolean isLunar(Player player) {
        ApolloPlayerManager pm = Apollo.getPlayerManager();
        UUID id = player.getUniqueId();

        if (!pm.hasSupport(id)) return false;
        return pm.getPlayer(id)
                .map(p -> p.getTebexEmbeddedCheckoutSupport() != TebexEmbeddedCheckoutSupport.UNSUPPORTED)
                .orElse(false);
    }

    @Override
    public void checkout(Player player, String baskedIdent) {
        Bukkit.broadcastMessage("lunar checkout");
    }
}
