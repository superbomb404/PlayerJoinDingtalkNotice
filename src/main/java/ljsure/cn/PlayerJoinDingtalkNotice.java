package ljsure.cn;

import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PlayerJoinDingtalkNotice extends JavaPlugin implements Listener {

    private SimpleDateFormat sdf;
    private String webhookUrl;

    @Override
    public void onEnable() {
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 保存默认配置
        saveDefaultConfig();
        reloadConfiguration();

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("钉钉通知插件已启用");
    }

    private void reloadConfiguration() {
        reloadConfig();
        FileConfiguration config = getConfig();

        webhookUrl = config.getString("webhook_url", "");
        if(webhookUrl.isEmpty()) {
            getLogger().severe("未配置钉钉Webhook地址！请在config.yml中设置webhook_url");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("成功加载配置文件");
        getLogger().info("当前Webhook地址: " + webhookUrl);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        String ipAddress = Objects.requireNonNull(event.getPlayer().getAddress()).getAddress().getHostAddress().split(":")[0];
        String time = sdf.format(new Date());

        getLogger().info(String.format("检测到玩家 %s 加入服务器 (IP: %s)", playerName, ipAddress));

        String coordinates = getPlayerCoordinates(event.getPlayer().getLocation().getWorld().getName(),
                event.getPlayer().getLocation());

        String message = String.format("玩家 %s 加入服务器\nIP: %s\n时间: %s\n位置: %s",
                playerName, ipAddress, time, coordinates);

        sendToDingTalk(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerName = event.getPlayer().getName();
        String ipAddress = Objects.requireNonNull(event.getPlayer().getAddress()).getAddress().getHostAddress().split(":")[0];
        String time = sdf.format(new Date());

        getLogger().info(String.format("检测到玩家 %s 离开服务器 (IP: %s)", playerName, ipAddress));

        String coordinates = getPlayerCoordinates(event.getPlayer().getLocation().getWorld().getName(),
                event.getPlayer().getLocation());

        String message = String.format("玩家 %s 离开服务器\nIP: %s\n时间: %s\n最后位置: %s",
                playerName, ipAddress, time, coordinates);

        sendToDingTalk(message);
    }

    private String getPlayerCoordinates(String worldName, org.bukkit.Location location) {
        try {
            String coordinates = String.format("%s世界 (%d, %d, %d)",
                    worldName,
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ());
            getLogger().info("成功获取玩家坐标: " + coordinates);
            return coordinates;
        } catch (Exception e) {
            getLogger().warning("获取坐标时出错: " + e.getMessage());
            return "未知";
        }
    }

    private void sendToDingTalk(String message) {
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
//            getLogger().info("正在发送消息到钉钉: " + message);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(webhookUrl);

                Map<String, Object> requestBody = new HashMap<>();
                Map<String, String> content = new HashMap<>();
                content.put("content", message);
                requestBody.put("msgtype", "text");
                requestBody.put("text", content);

                StringEntity entity = new StringEntity(new Gson().toJson(requestBody), "UTF-8");
                entity.setContentType("application/json");
                httpPost.setEntity(entity);

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        getLogger().info("钉钉消息发送成功");
                    } else {
                        getLogger().warning("钉钉消息发送失败，状态码: " + statusCode);
                    }
                }
            } catch (Exception e) {
                getLogger().warning("发送消息时出错: " + e.getMessage());
            }
        });
    }
}