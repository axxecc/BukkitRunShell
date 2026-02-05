package wtf.bukkitrunshell.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import wtf.bukkitrunshell.sessions.SessionManager;
import wtf.bukkitrunshell.sessions.ShellSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ShellCommand implements CommandExecutor, TabExecutor {
    private final SessionManager sessionManager;

    public ShellCommand() {
        this.sessionManager = SessionManager.getInstance();
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        if (!player.hasPermission("bukkitrunshell.use")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        String subCommand = args.length > 0 ? args[0].toLowerCase() : "start";

        switch (subCommand) {
            case "start":
                handleStart(player);
                break;

            case "exit":
                handleStop(player);
                break;

            case "status":
                handleStatus(player);
                break;

            case "help":
            default:
                handleHelp(player);
                break;
        }
        return true;
    }

    private void handleStart(Player player) {
        if (sessionManager.startSession(player)) {
            player.sendMessage(ChatColor.GREEN + "交互式Shell会话已启动!");
        } else {
            player.sendMessage(ChatColor.RED + "无法启动Shell会话!");
        }
    }

    private void handleStop(Player player) {
        if (sessionManager.stopSession(player)) {
            player.sendMessage(ChatColor.GREEN + "Shell会话已结束!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "没有活动的Shell会话!");
        }
    }

    private void handleStatus(Player player) {
        Collection<ShellSession> sessions = sessionManager.getAllSessions();
        if (!sessions.isEmpty()) {
            player.sendMessage(ChatColor.GOLD + "=== 活动Shell会话 ===");
            for (ShellSession session : sessions) {
                Player sessionPlayer = session.getPlayer();
                player.sendMessage(ChatColor.YELLOW + "- " + sessionPlayer.getName());
            }
        }
        if (sessionManager.hasActiveSession(player)) {
            player.sendMessage(ChatColor.GREEN + "✓ 你有一个活动的Shell会话");
        } else {
            player.sendMessage(ChatColor.YELLOW + "✗ 没有活动的Shell会话");
        }
    }

    private void handleHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== InteractiveShell帮助 ===");
        player.sendMessage(ChatColor.YELLOW + "/shell start - 启动交互式Shell会话");
        player.sendMessage(ChatColor.YELLOW + "/shell exit - 结束Shell会话");
        player.sendMessage(ChatColor.YELLOW + "/shell status - 查看会话状态");
        player.sendMessage(ChatColor.YELLOW + "/shell help - 显示此帮助");
        player.sendMessage(ChatColor.GRAY + "提示: 启动会话后，所有聊天消息都会作为Shell命令执行");
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> commands = Arrays.asList("start", "status", "help", "exit");

            for (String command : commands) {
                if (command.startsWith(partial)) {
                    completions.add(command);
                }
            }
        }
        return completions;
    }
}