package me.kyllian.paynowgui.core.hooks;

import java.util.List;

public interface INpcHook {

    void updateNpc(int npcId, String skinName, List<String> hologramLines);
}
