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
import java.util.*;

import static lol.niox.paytokeep.PlayerDeathListener.Salvage;

public final class PayToKeep extends JavaPlugin {
    private static Economy econ = null;
    private static Permission perms = null;
    private static Chat chat = null;
    public static Map<String, List<Boolean>> data;
    private static double price;
    private static double salvagePrice;
    private static long salvageExpirationTime;

    @Override
    public void onEnable() {
        System.out.println("autumnal—leaves是大笨蛋！");
        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();
        setupChat();

        Path filePath = Paths.get("./PayToKeepData/data.json");

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

        // /buyinv
        if (command.getName().equalsIgnoreCase("buyinv")) {
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
                    saveJsonData("./PayToKeepData/data.json");
                }
                if (econ.getBalance(player) < price) {
                    player.sendMessage("穷鬼，钱够了再来！");
                    return true;
                } else {
                    econ.withdrawPlayer(player, price);
                    data.get(playerUUID).set(0, true);
                    player.sendMessage("你现在可以随便死一次了！");
                    saveJsonData("./PayToKeepData/data.json");
                    return true;
                }

            }
        }

        // /setinvprice
        if (command.getName().equalsIgnoreCase("setinvprice")) {
            if (args.length == 0) {
                sender.sendMessage("请输入一个价格！");
            }
            try {
                price = Double.parseDouble(args[0]);
                saveJsonData("./PayToKeepData/data.json");
                sender.sendMessage("价格已设置为" + ChatColor.GREEN + price);
                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage("请输入一个数字！");
            }
        }

        // /switchkeep
        if (command.getName().equalsIgnoreCase("switchkeep")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String playerUUID = player.getUniqueId().toString();
                if (data.containsKey(playerUUID)) {
                    data.get(playerUUID).set(1, !data.get(playerUUID).get(1));
                    player.sendMessage("你的保留状态已切换为" + ChatColor.GOLD + (data.get(playerUUID).get(1) ? " 开启" : " 关闭"));
                    saveJsonData("./PayToKeepData/data.json");
                } else {
                    List<Boolean> booleanList = Arrays.asList(false, false);
                    data.put(playerUUID, booleanList);
                    player.sendMessage("你的保留状态已切换为" + ChatColor.GOLD + " 关闭");
                    saveJsonData("./PayToKeepData/data.json");
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
        if (command.getName().equalsIgnoreCase("setsalvageprice")) {
            if (args.length == 0) {
                sender.sendMessage("请输入一个价格！");
            }
            try {
                salvagePrice = Double.parseDouble(args[0]);
                saveJsonData("./PayToKeepData/data.json");
                sender.sendMessage("价格已设置为" + ChatColor.GREEN + salvagePrice);
                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage("请输入一个数字！");
            }
        }

        // /setsalvageexpirationtime
        if (command.getName().equalsIgnoreCase("setsalvageexpirationtime")) {
            if (args.length == 0) {
                sender.sendMessage("请输入一个时间！");
            }
            try {
                salvageExpirationTime = Long.parseLong(args[0]);
                saveJsonData("./PayToKeepData/data.json");
                sender.sendMessage("时间已设置为" + ChatColor.GOLD + salvageExpirationTime + "毫秒");
                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage("请输入一个数字！");
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
            salvageExpirationTime = (long) loadedJson.get("salvageExpirationTime");
        }
    }

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

}
