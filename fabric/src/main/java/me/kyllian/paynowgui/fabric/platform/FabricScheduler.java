package me.kyllian.paynowgui.fabric.platform;

import me.kyllian.paynowgui.core.platform.PlatformScheduler;
import me.kyllian.paynowgui.fabric.FabricPayNowMod;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fabric implementation of PlatformScheduler.
 * <p>
 * Async tasks run on a shared thread pool.
 * Sync tasks are submitted to the MinecraftServer main thread via server.execute().
 * Delayed/repeating tasks use a ScheduledExecutorService.
 */
public class FabricScheduler implements PlatformScheduler {

    private final FabricPayNowMod mod;
    private final ExecutorService asyncPool;
    private final ScheduledExecutorService scheduledPool;
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public FabricScheduler(FabricPayNowMod mod) {
        this.mod = mod;
        this.asyncPool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "paynow-gui-async");
            t.setDaemon(true);
            return t;
        });
        this.scheduledPool = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "paynow-gui-scheduled");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void runAsync(Runnable task) {
        asyncPool.submit(task);
    }

    @Override
    public void runSync(Runnable task) {
        if (mod.getServer() != null) {
            mod.getServer().execute(task);
        } else {
            // Fallback: run directly if server not yet available
            task.run();
        }
    }

    @Override
    public void runSyncLater(Runnable task, long delayTicks) {
        // Convert ticks to milliseconds (1 tick = 50ms)
        long delayMs = delayTicks * 50;
        scheduledPool.schedule(() -> runSync(task), delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public int runSyncRepeatingAsync(Runnable task, long delayTicks, long periodTicks) {
        int taskId = taskIdCounter.incrementAndGet();
        long delayMs = delayTicks * 50;
        long periodMs = periodTicks * 50;

        ScheduledFuture<?> future = scheduledPool.scheduleAtFixedRate(task, delayMs, periodMs, TimeUnit.MILLISECONDS);
        scheduledTasks.put(taskId, future);
        return taskId;
    }

    @Override
    public void cancelTask(int taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }
}
