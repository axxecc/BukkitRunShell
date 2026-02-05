package wtf.bukkitrunshell;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.bukkitrunshell.commands.ShellCommand;
import wtf.bukkitrunshell.sessions.SessionManager;

public class BukkitRunShell extends JavaPlugin implements Listener {
    private static BukkitRunShell instance;
    private SessionManager sessionManager;
    private static boolean isFolia;

    public static BukkitRunShell getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        sessionManager = SessionManager.getInstance();

        // 检查操作系统
        if (!System.getProperty("os.name").toLowerCase().contains("linux")) {
            getLogger().severe("只支持Linux系统! 插件以禁用");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // 检查是否为Folia
        checkFolia();

        // 注册命令
        ShellCommand shellCommand = new ShellCommand();

        getCommand("shell").setExecutor(shellCommand);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().warning("======================================");
        getLogger().warning("InteractiveShell插件已启用!");
        getLogger().warning("这是一个危险插件，请谨慎使用!");
        getLogger().warning("======================================");
    }

    @Override
    public void onDisable() {
        sessionManager.stopAllSessions(true);
        getLogger().info("所有Shell会话已终止");
    }

    private void checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
    }

    // 运行任务
    public void runSync(Runnable task) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().run(this, scheduledTask1 -> task.run());
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // 检查玩家是否有活动的Shell会话
        if (sessionManager.hasActiveSession(event.getPlayer())) {
            // 取消聊天消息
            event.setCancelled(true);

            // 在同步上下文中执行Shell命令
            runSync(() -> {
                String message = event.getMessage();
                sessionManager.executeCommand(event.getPlayer(), message);
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 玩家离开时自动结束会话
        sessionManager.stopSession(event.getPlayer());
    }
}