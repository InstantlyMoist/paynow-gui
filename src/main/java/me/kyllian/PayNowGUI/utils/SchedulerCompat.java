package me.kyllian.PayNowGUI.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class SchedulerCompat {

    private static final Method GET_ASYNC_SCHEDULER = findMethod(Bukkit.getServer().getClass(), "getAsyncScheduler");
    private static final Method GET_GLOBAL_REGION_SCHEDULER = findMethod(Bukkit.getServer().getClass(), "getGlobalRegionScheduler");
    private static final Method GET_ENTITY_SCHEDULER = findMethod(Player.class, "getScheduler");

    private SchedulerCompat() {
    }

    public static boolean isFoliaSupportedRuntime() {
        return GET_ASYNC_SCHEDULER != null && GET_GLOBAL_REGION_SCHEDULER != null && GET_ENTITY_SCHEDULER != null;
    }

    public static void runAsync(JavaPlugin plugin, Runnable task) {
        Object asyncScheduler = getAsyncScheduler();
        if (asyncScheduler == null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            return;
        }

        invoke(findMethod(asyncScheduler.getClass(), "runNow", Plugin.class, Consumer.class), asyncScheduler, plugin, (Consumer<Object>) scheduledTask -> task.run());
    }

    public static CancellableTask runAsyncTimer(JavaPlugin plugin, Runnable task, long initialDelayTicks, long periodTicks) {
        Object asyncScheduler = getAsyncScheduler();
        if (asyncScheduler == null) {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, initialDelayTicks, periodTicks);
            return bukkitTask::cancel;
        }

        Object scheduledTask = invoke(
                findMethod(asyncScheduler.getClass(), "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class),
                asyncScheduler,
                plugin,
                (Consumer<Object>) taskHandle -> task.run(),
                ticksToMillis(initialDelayTicks),
                ticksToMillis(periodTicks),
                TimeUnit.MILLISECONDS
        );
        return () -> invoke(findMethod(scheduledTask.getClass(), "cancel"), scheduledTask);
    }

    public static void runGlobal(JavaPlugin plugin, Runnable task) {
        Object globalScheduler = getGlobalRegionScheduler();
        if (globalScheduler == null) {
            Bukkit.getScheduler().runTask(plugin, task);
            return;
        }

        invoke(findMethod(globalScheduler.getClass(), "execute", Plugin.class, Runnable.class), globalScheduler, plugin, task);
    }

    public static void runForPlayer(JavaPlugin plugin, Player player, Runnable task) {
        runForPlayer(plugin, player, 0L, task);
    }

    public static void runLaterForPlayer(JavaPlugin plugin, Player player, long delayTicks, Runnable task) {
        runForPlayer(plugin, player, delayTicks, task);
    }

    private static void runForPlayer(JavaPlugin plugin, Player player, long delayTicks, Runnable task) {
        if (player == null || !player.isOnline()) return;

        Object entityScheduler = getEntityScheduler(player);
        if (entityScheduler == null) {
            if (delayTicks <= 0L) {
                Bukkit.getScheduler().runTask(plugin, task);
                return;
            }
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return;
        }

        Boolean scheduled = invoke(
                findMethod(entityScheduler.getClass(), "execute", Plugin.class, Runnable.class, Runnable.class, long.class),
                entityScheduler,
                plugin,
                task,
                null,
                delayTicks
        );
        if (Boolean.FALSE.equals(scheduled)) return;
    }

    private static Object getAsyncScheduler() {
        if (GET_ASYNC_SCHEDULER == null) return null;
        return invoke(GET_ASYNC_SCHEDULER, Bukkit.getServer());
    }

    private static Object getGlobalRegionScheduler() {
        if (GET_GLOBAL_REGION_SCHEDULER == null) return null;
        return invoke(GET_GLOBAL_REGION_SCHEDULER, Bukkit.getServer());
    }

    private static Object getEntityScheduler(Player player) {
        if (GET_ENTITY_SCHEDULER == null) return null;
        return invoke(GET_ENTITY_SCHEDULER, player);
    }

    private static long ticksToMillis(long ticks) {
        return ticks * 50L;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Method method, Object instance, Object... args) {
        if (method == null) {
            throw new IllegalStateException("missing scheduler method");
        }

        try {
            return (T) method.invoke(instance, args);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("failed to invoke scheduler method", exception);
        }
    }

    @FunctionalInterface
    public interface CancellableTask {
        void cancel();
    }
}
