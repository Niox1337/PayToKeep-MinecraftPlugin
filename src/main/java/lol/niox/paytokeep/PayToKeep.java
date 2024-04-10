package lol.niox.paytokeep;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
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

public final class PayToKeep extends JavaPlugin {
    private static Economy econ = null;
    private static Permission perms = null;
    private static Chat chat = null;
    private static Map<String, List<Boolean>> data;
    private static int price;

    @Override
    public void onEnable() {
        System.out.println("PayToKeep has been enabled!");
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
                }
                if (econ.getBalance(player) < price) {
                    player.sendMessage("穷鬼，钱够了再来！");
                    return true;
                } else {
                    econ.withdrawPlayer(player, price);
                    data.get(playerUUID).set(0, true);
                    player.sendMessage("你现在可以随便死一次了！");
                    saveJsonData("./PayToKeepData/data.json", data, price);
                    return true;
                }

            }
        }

        // /setinvprice
        if (command.getName().equalsIgnoreCase("setinvprice")) {}

        // /switchkeep
        if (command.getName().equalsIgnoreCase("switchkeep")) {}

        // /salvage
        if (command.getName().equalsIgnoreCase("salvage")) {}
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
        ObjectMapper objectMapper = new ObjectMapper();
        Path path = Paths.get(filePath);
        Map<String, Object> loadedJson = ((Map<String, Object>) objectMapper.readValue(path.toFile(), Map.class));

        // Check if file is empty, if so initialize data with an empty map
        if (Files.size(path) == 0) {
            data = new HashMap<>();
            price = 1000;
        } else {
            // Read the file and convert its content to a Map<String, boolean[]>
            data = loadedJson.get("data") == null ? new HashMap<>() : (Map<String, List<Boolean>>) loadedJson.get("data");
            price = (int) loadedJson.get("price");
        }
    }

    private void saveJsonData(String filePath, Map<String, List<Boolean>> data, int price) {
        ObjectMapper objectMapper = new ObjectMapper();
        Path path = Paths.get(filePath);
        Map<String, Object> dataToWrite = new HashMap<>();
        dataToWrite.put("data", data);
        dataToWrite.put("price", price);

        // Write the data to the file
        try {
            objectMapper.writeValue(path.toFile(), dataToWrite);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
