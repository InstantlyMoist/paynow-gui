package me.kyllian.paynowgui.bukkit.platform;

import me.kyllian.paynowgui.core.platform.PlatformScheduler;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.Bukkit.getScheduler;

public class BukkitScheduler implements PlatformScheduler {

    private final JavaPlugin plugin;

    public BukkitScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable task) {
        getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runSync(Runnable task) {
        getScheduler().runTask(plugin, task);
    }

    @Override
    public void runSyncLater(Runnable task, long delayTicks) {
        getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public int runSyncRepeatingAsync(Runnable task, long delayTicks, long periodTicks) {
        return getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks).getTaskId();
    }

    @Override
    public void cancelTask(int taskId) {
        getScheduler().cancelTask(taskId);
    }
}
