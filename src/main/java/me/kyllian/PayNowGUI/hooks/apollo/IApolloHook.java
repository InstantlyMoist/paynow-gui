package me.kyllian.PayNowGUI.hooks.apollo;

import org.bukkit.entity.Player;

public interface IApolloHook {

    boolean isLunar(Player player);
    void checkout(Player player, String ident);

}
