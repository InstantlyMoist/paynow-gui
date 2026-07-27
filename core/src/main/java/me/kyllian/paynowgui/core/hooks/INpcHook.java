package me.kyllian.paynowgui.core.hooks;

import java.util.List;

public interface INpcHook {

    /**
     * @param npcId identifier of the NPC, as configured. Citizens uses numeric ids,
     *              SpaceNPC uses free-form string ids.
     */
    void updateNpc(String npcId, String skinName, List<String> hologramLines);
}
