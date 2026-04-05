package me.kyllian.paynowgui.core.hooks;

import java.util.List;

public class NoopNpcHook implements INpcHook {

    @Override
    public void updateNpc(int npcId, String skinName, List<String> hologramLines) {}
}
