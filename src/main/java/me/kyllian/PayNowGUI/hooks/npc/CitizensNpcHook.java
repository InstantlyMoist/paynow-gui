package me.kyllian.PayNowGUI.hooks.npc;

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
        for (String line : hologramLines.reversed()) { // Citizens handles holograms in reverse order
            hologramTrait.addLine(line);
        }
    }
}
