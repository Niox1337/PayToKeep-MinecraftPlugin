package lol.niox.paytokeep;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lol.niox.paytokeep.PlayerDeathListener.Salvage;
import static lol.niox.paytokeep.PlayerDeathListener.salvagePart;

public final class PayToKeep extends JavaPlugin {
    public static final String FILE_PATH = "./plugins/PayToKeepData/data.json";
    public static Map<String, List<Boolean>> data;  // <playerUUID, [hasBought, setToKeep]>
    private static Economy econ = null;
    private static Permission perms = null;
    private static Chat chat = null;
    private static double price;
    private static double salvagePrice;
    private static int salvageExpirationTime;

    public static void saveJsonData(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        Path path = Paths.get(filePath);
        Map<String, Object> dataToWrite = new HashMap<>();
        dataToWrite.put("data", data);
        dataToWrite.put("price", price);
        dataToWrite.put("salvagePrice", salvagePrice);
        dataToWrite.put("salvageExpirationTime", salvageExpirationTime);

        // Write the data to the file
        try {
            objectMapper.writeValue(path.toFile(), dataToWrite);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, List<Boolean>> getData() {
        return data;
    }

    public static double getSalvageExpirationTime() {
        return salvageExpirationTime;
    }

    @Override
    public void onEnable() {
        System.out.println("autumnal—leaves是大笨蛋！");
        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();
        setupChat();

        Path filePath = Paths.get(FILE_PATH);

        if (!Files.exists(filePath)) {
            try {
                Files.createDirectories(filePath.getParent());
                Files.createFile(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            loadJsonData(filePath.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);

        System.out.println("PayToKeep has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /buykp
        if (command.getName().equalsIgnoreCase("kpbuy")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String playerUUID = player.getUniqueId().toString();
                if (data.containsKey(playerUUID)) {
                    if (data.get(playerUUID).get(0)) {
                        player.sendMessage("你不许再买了！");
                        return true;
                    }
                } else {
                    List<Boolean> booleanList = Arrays.asList(false, true);
                    data.put(playerUUID, booleanList);
                    saveJsonData(FILE_PATH);
                }
                if (econ.getBalance(player) < price) {
                    player.sendMessage("穷鬼，钱够了再来！");
                    return true;
                } else {
                    econ.withdrawPlayer(player, price);
                    data.get(playerUUID).set(0, true);
                    player.sendMessage("你现在可以随便死一次了！");
                    saveJsonData(FILE_PATH);
                    return true;
                }

            }
        }

        // /setkpprice
        if (command.getName().equalsIgnoreCase("kpsetprice")) {
            if (args.length > 0) {
                try {
                    price = Double.parseDouble(args[0]);
                    saveJsonData(FILE_PATH);
                    sender.sendMessage("价格已设置为" + ChatColor.GREEN + price);
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("请输入一个数字！");
                }
            } else {
                sender.sendMessage("请输入一个价格！");
            }
        }

        // /switchkp
        if (command.getName().equalsIgnoreCase("kpswitch")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String playerUUID = player.getUniqueId().toString();
                if (data.containsKey(playerUUID)) {
                    data.get(playerUUID).set(1, !data.get(playerUUID).get(1));
                    player.sendMessage("你的保留状态已切换为" + ChatColor.GOLD + (data.get(playerUUID).get(1) ? " 开启" : " 关闭"));
                    saveJsonData(FILE_PATH);
                } else {
                    List<Boolean> booleanList = Arrays.asList(false, false);
                    data.put(playerUUID, booleanList);
                    player.sendMessage("你的保留状态已切换为" + ChatColor.GOLD + " 关闭");
                    saveJsonData(FILE_PATH);
                }
                return true;
            }
        }

        // /kpstatus
        if (command.getName().equalsIgnoreCase("kpstatus")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String playerUUID = player.getUniqueId().toString();
                if (data.containsKey(playerUUID)) {
                    player.sendMessage("你的库存状态为：" + ChatColor.GOLD + (data.get(playerUUID).get(0) ? "已购买" : "未购买") + ChatColor.RESET + "，" +
                            "保留状态为：" + ChatColor.GOLD + (data.get(playerUUID).get(1) ? "开启" : "关闭"));
                } else {
                    List<Boolean> booleanList = Arrays.asList(false, false);
                    data.put(playerUUID, booleanList);
                    saveJsonData(FILE_PATH);
                    player.sendMessage("你的库存状态为：" + ChatColor.GOLD + "未购买" + ChatColor.RESET + "，" +
                            "保留状态为：" + ChatColor.GOLD + "关闭");
                }
                return true;
            }
        }

        // /salvage
        if (command.getName().equalsIgnoreCase("salvage")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (econ.getBalance(player) < salvagePrice) {
                    player.sendMessage("穷鬼，钱够了再来！");
                    return true;
                } else {
                    econ.withdrawPlayer(player, salvagePrice);
                }
                Salvage(player, System.currentTimeMillis());
                return true;
            }
        }

        // /setsalvageprice
        if (command.getName().equalsIgnoreCase("salvagesetprice")) {
            if (args.length > 0) {
                try {
                    salvagePrice = Double.parseDouble(args[0]);
                    saveJsonData(FILE_PATH);
                    sender.sendMessage("价格已设置为" + ChatColor.GREEN + salvagePrice);
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("请输入一个数字！");
                }
            } else {
                sender.sendMessage("请输入一个价格！");
            }
        }

        // /setexpire
        if (command.getName().equalsIgnoreCase("salvagesetexpire")) {
            if (args.length > 0) {
                try {
                    salvageExpirationTime = Integer.parseInt(args[0]);
                    saveJsonData(FILE_PATH);
                    sender.sendMessage("时间已设置为" + ChatColor.GOLD + salvageExpirationTime + ChatColor.RESET + "毫秒");
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("请输入一个数字！");
                }
            } else {
                sender.sendMessage("请输入一个时间！");
            }
        }

        // /salvagepart
        if (command.getName().equalsIgnoreCase("salvagepart")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (econ.getBalance(player) < salvagePrice) {
                    player.sendMessage("穷鬼，钱够了再来！");
                    return true;
                } else {
                    econ.withdrawPlayer(player, salvagePrice);
                }
                salvagePart(player, System.currentTimeMillis());
                return true;
            }
        }
        return false;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        chat = rsp.getProvider();
        return chat != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    private void loadJsonData(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        // Check if file is empty, if so initialize data with an empty map
        if (Files.size(path) == 0) {
            data = new HashMap<>();
            price = 1000.0;
            salvagePrice = 1500.0;
            salvageExpirationTime = 30000;
        } else {
            // Read the file and convert its content to a Map<String, boolean[]>
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> loadedJson = ((Map<String, Object>) objectMapper.readValue(path.toFile(), Map.class));
            data = loadedJson.get("data") == null ? new HashMap<>() : (Map<String, List<Boolean>>) loadedJson.get("data");
            price = (double) loadedJson.get("price");
            salvagePrice = (double) loadedJson.get("salvagePrice");
            salvageExpirationTime = (int) loadedJson.get("salvageExpirationTime");
        }
    }

}
