package me.kyllian.paynowgui.core.platform;

/**
 * Platform-agnostic scheduler for running tasks.
 */
public interface PlatformScheduler {

    void runAsync(Runnable task);

    void runSync(Runnable task);

    void runSyncLater(Runnable task, long delayTicks);

    int runSyncRepeatingAsync(Runnable task, long delayTicks, long periodTicks);

    void cancelTask(int taskId);
}
