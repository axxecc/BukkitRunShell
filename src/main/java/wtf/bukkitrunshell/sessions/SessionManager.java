package wtf.bukkitrunshell.sessions;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {
    private static SessionManager instance;
    private final Map<UUID, ShellSession> activeSessions;

    private SessionManager() {
        activeSessions = new HashMap<>();
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public boolean startSession(Player player) {
        // 检查是否已有会话
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "你已有一个活动的Shell会话!");
            return false;
        }

        // 创建新会话
        ShellSession session = new ShellSession(player);
        if (session.start()) {
            activeSessions.put(player.getUniqueId(), session);
            return true;
        }
        return false;
    }

    public boolean stopSession(Player player) {
        ShellSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.stop(false);
            return true;
        }
        return false;
    }

    public ShellSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public void executeCommand(Player player, String command) {
        ShellSession session = getSession(player);
        if (session != null) {
            session.executeCommand(command);
        } else {
            player.sendMessage(ChatColor.RED + "没有活动的Shell会话!");
        }
    }

    public void stopAllSessions(boolean pluginDisabling) {
        for (ShellSession session : activeSessions.values()) {
            session.stop(pluginDisabling);
        }
        activeSessions.clear();
    }

    public Collection<ShellSession> getAllSessions() {
        return activeSessions.values();
    }
}