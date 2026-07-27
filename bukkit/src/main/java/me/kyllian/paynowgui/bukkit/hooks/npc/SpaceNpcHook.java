package me.kyllian.paynowgui.bukkit.hooks.npc;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import me.kyllian.paynowgui.core.hooks.INpcHook;
import me.tofaa.entitylib.npc.NPC;
import me.tofaa.entitylib.npc.NPCRegistry;
import me.tofaa.entitylib.npc.SpaceNPC;
import me.tofaa.entitylib.npc.skin.NPCSkin;
import me.tofaa.entitylib.wrapper.hologram.Hologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * NPC hook for SpaceNPC, the NPC plugin shipped as part of EntityLib.
 * <p>
 * SpaceNPC renders its name tag as a hologram built from the NPC's display name, split into lines
 * on the MiniMessage {@code <br>} tag, so the legacy coloured lines we receive are turned into one
 * component per line here.
 */
public class SpaceNpcHook implements INpcHook {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /** Height above the NPC's feet at which the bottom hologram line sits. */
    private static final double DEFAULT_HOLOGRAM_HEIGHT = 1.0;

    private final JavaPlugin plugin;

    /** Last skin we successfully applied, so we don't hit the skin API on every update. */
    private volatile String appliedSkinName;

    public SpaceNpcHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void updateNpc(String npcId, String skinName, List<String> hologramLines) {
        NPC npc = NPCRegistry.get(npcId);
        if (npc == null) {
            Bukkit.getLogger().severe("npc_id invalid for SpaceNPC");
            return;
        }

        List<Component> lines = toLines(hologramLines);

        // the display name is what SpaceNPC rebuilds the hologram from on respawn and on player
        // join, so it has to stay in sync even though we push the lines ourselves below
        npc.getOptions()
                .showNameTag(true)
                .displayName(toDisplayName(lines));

        if (npc.isSpawned()) {
            updateHologram(npc, lines);
        }

        applySkin(npc, skinName, lines);
    }

    private List<Component> toLines(List<String> hologramLines) {
        List<Component> lines = new ArrayList<>(hologramLines.size());
        for (String line : hologramLines) {
            lines.add(LEGACY.deserialize(line));
        }
        return lines;
    }

    /**
     * Pushes the lines straight onto the hologram rather than going through {@code NPC#updateHologram()},
     * which is private in some SpaceNPC builds and would blow up with an IllegalAccessError.
     */
    private void updateHologram(NPC npc, List<Component> lines) {
        Hologram hologram = npc.getHologram().orElse(null);
        if (hologram == null) return;

        hologram.setLines(raise(npc, hologram, lines));

        // lines added after the hologram was created start out with no viewers of their own, so the
        // hologram's audience has to be re-synced with the NPC's whenever the line count grows
        for (UUID viewer : npc.getViewers()) {
            hologram.addViewer(viewer);
        }
    }

    /**
     * SpaceNPC re-anchors every hologram to a fixed metre above its NPC twice a second
     * (NPCMovement#processViewerSync), so moving the anchor doesn't stick — it snaps back within
     * a second. Its lines also stack downwards from that anchor, which is what puts a multi-line
     * hologram inside the NPC's head.
     * <p>
     * Flip the offset so the lines stack upwards instead and pad the bottom of the stack with
     * blank lines until the text clears the head. Both survive the re-anchoring, since the tick
     * only rewrites the anchor and the layout is derived from it.
     */
    private List<Component> raise(NPC npc, Hologram hologram, List<Component> lines) {
        if (!(hologram instanceof Hologram.Legacy legacy)) return lines;

        float spacing = Math.abs(legacy.getLineOffset());
        if (spacing <= 0) return lines;
        legacy.setLineOffset(spacing);

        double height = plugin.getConfig().getDouble("recent_donator_npc.hologram_height", DEFAULT_HOLOGRAM_HEIGHT);
        double anchor = hologram.getLocation().getY() - npc.getPosition().getY();
        int padding = (int) Math.max(0, Math.round((height - anchor) / spacing));

        // line 0 is the bottom one now, so the padding goes first and the text is reversed
        List<Component> raised = new ArrayList<>(padding + lines.size());
        for (int i = 0; i < padding; i++) {
            raised.add(Component.empty());
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            raised.add(lines.get(i));
        }
        return raised;
    }

    /**
     * Each line becomes its own child of an unstyled root, separated by newlines. MiniMessage only
     * serializes a newline back to the {@code <br>} tag that SpaceNPC splits on when it sits at the
     * top level with no style open — a newline nested inside e.g. an unclosed {@code <bold>} is
     * written out as a raw line break instead, which leaves the whole hologram on one line.
     */
    private Component toDisplayName(List<Component> lines) {
        TextComponent.Builder displayName = Component.text();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) displayName.append(Component.newline());
            displayName.append(lines.get(i));
        }
        return displayName.build();
    }

    /**
     * Fetching a skin hits Mojang synchronously, so it happens off the main thread. The NPC has to
     * be respawned for a new skin to reach the players that already see it.
     */
    private void applySkin(NPC npc, String skinName, List<Component> lines) {
        if (skinName == null || skinName.isEmpty()) return;
        if (skinName.equals(appliedSkinName)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TextureProperty texture;
            try {
                List<TextureProperty> textures = SpaceNPC.getInstance().getSkinFetcher().getSkin(skinName);
                if (textures.isEmpty()) return;
                texture = textures.getFirst();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[paynow-gui] Failed to fetch skin for " + skinName, e);
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                npc.setSkin(new NPCSkin(texture.getValue(), texture.getSignature()));
                appliedSkinName = skinName;

                if (npc.isSpawned()) {
                    npc.despawn();
                    npc.spawn(npc.getPosition());
                    // the respawn rebuilds the hologram from the display name at SpaceNPC's own anchor
                    updateHologram(npc, lines);
                }
            });
        });
    }
}
