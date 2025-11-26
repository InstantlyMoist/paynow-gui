package me.kyllian.PayNowGUI.hooks.apollo;

import org.bukkit.entity.Player;

public class NoopApolloHook implements IApolloHook {

    @Override
    public boolean isLunar(Player player) {
        return false;
    }

    @Override
    public void checkout(Player player, String baskedIdent) {}
}
