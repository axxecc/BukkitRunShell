package wtf.bukkitrunshell.sessions;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import wtf.bukkitrunshell.BukkitRunShell;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ShellSession {
    private final Player player;
    private final ExecutorService executor;
    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean active;

    public ShellSession(Player player) {
        this.player = player;
        this.executor = Executors.newFixedThreadPool(2);
        this.active = false;
    }

    public boolean start() {
        try {
            // 创建交互式bash进程
            ProcessBuilder bash = new ProcessBuilder("bash");
            bash.redirectErrorStream(true);
            process = bash.start();

            // 获取输入输出流
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            active = true;

            // 启动输出监听线程
            executor.submit(this::readOutput);

            // 发送欢迎消息
            player.sendMessage(ChatColor.GREEN + "=== Shell会话已启动 ===");
            player.sendMessage(ChatColor.YELLOW + "现在你可以在聊天中输入命令");
            player.sendMessage(ChatColor.YELLOW + "输入 'exit' 或使用 '/shell stop' 退出");
            player.sendMessage(ChatColor.GOLD + "当前目录: " + System.getProperty("user.dir"));

            // 发送初始提示符
            sendPrompt();

            return true;

        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "启动Shell失败: " + e.getMessage());
            return false;
        }
    }

    private void sendPrompt() {
        try {
            // 发送命令提示符请求
            writer.write("echo -n \"$USER@$HOSTNAME:$PWD$ \"\n");
            writer.flush();
        } catch (IOException e) {
            // 忽略错误
        }
    }

    public void executeCommand(String command) {
        if (!active || process == null) {
            player.sendMessage(ChatColor.RED + "会话未激活!");
            return;
        }

        // 检查退出命令
        if (command.equalsIgnoreCase("exit")) {
            SessionManager.getInstance().stopSession(player);
            return;
        }

        try {
            // 发送命令到bash
            writer.write(command + "\n");
            writer.flush();

            // 添加空行分隔
            player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");

        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "发送命令失败: " + e.getMessage());
        }
    }

    private void readOutput() {
        try {
            StringBuilder currentOutput = new StringBuilder();
            boolean inPrompt = false;

            while (active && process != null && process.isAlive()) {
                if (reader.ready()) {
                    char c = (char) reader.read();

                    if (c == '\n') {
                        if (!currentOutput.isEmpty()) {
                            final String line = currentOutput.toString();
                            BukkitRunShell.getInstance().runSync(() -> {
                                player.sendMessage(ChatColor.WHITE + line);
                            });
                            currentOutput.setLength(0);
                        }
                    } else if (c == '$' || c == '#' || c == '>') {
                        inPrompt = true;
                    } else {
                        if (!inPrompt || Character.isWhitespace(c)) {
                            currentOutput.append(c);
                        }
                        inPrompt = false;
                    }
                } else {
                    // 短暂休眠避免CPU占用过高
                    Thread.sleep(10);
                }
            }
        } catch (IOException | InterruptedException e) {
            if (active) {
                BukkitRunShell.getInstance().runSync(() -> {
                    player.sendMessage(ChatColor.RED + "读取输出出错: " + e.getMessage());
                });
            }
        }
    }

    public void stop(boolean pluginDisabling) {
        active = false;

        try {
            // 发送退出命令
            if (writer != null) {
                writer.write("exit\n");
                writer.flush();
            }

            // 等待进程结束
            if (process != null) {
                process.waitFor(2, TimeUnit.SECONDS);
                process.destroy();
            }

            // 关闭流
            if (reader != null) reader.close();
            if (writer != null) writer.close();

        } catch (IOException | InterruptedException e) {
            // 忽略错误
        } finally {
            executor.shutdownNow();
            if (!pluginDisabling) {
                BukkitRunShell.getInstance().runSync(() -> {
                    player.sendMessage(ChatColor.GREEN + "=== Shell会话已结束 ===");
                });
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public Player getPlayer() {
        return player;
    }
}