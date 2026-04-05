package me.kyllian.paynowgui.bukkit.hooks.npc;

import me.kyllian.paynowgui.core.hooks.INpcHook;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.HologramTrait;
import net.citizensnpcs.trait.SkinTrait;

import java.util.List;

public class CitizensNpcHook implements INpcHook {

    @Override
    public void updateNpc(int npcId, String skinName, List<String> hologramLines) {
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc == null) return;

        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
        skinTrait.setSkinName(skinName);

        HologramTrait hologramTrait = npc.getOrAddTrait(HologramTrait.class);
        hologramTrait.clear();
        hologramTrait.setLineHeight(0.3);
        for (String line : hologramLines.reversed()) {
            hologramTrait.addLine(line);
        }
    }
}
