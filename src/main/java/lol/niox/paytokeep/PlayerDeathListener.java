package lol.niox.paytokeep;

import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

import static lol.niox.paytokeep.PayToKeep.FILE_PATH;
import static lol.niox.paytokeep.PayToKeep.saveJsonData;

public class PlayerDeathListener implements Listener {
    public static HashMap<UUID, DeathInfo> deathRecords = new HashMap<>();

    public static void Salvage(Player player, long currentTime) {
        UUID playerUUID = player.getUniqueId();
        if (deathRecords.containsKey(playerUUID)) {
            DeathInfo deathInfo = deathRecords.get(playerUUID);
            PlayerInventory playerInventory = player.getInventory();
            if (currentTime - deathInfo.lastDeath > PayToKeep.getSalvageExpirationTime()) {
                deathRecords.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "你的库存已过期");
                return;
            }
            if (deathInfo.droppedItems.isEmpty()){
                player.sendMessage(ChatColor.RED + "可恢复库存为空！");
                deathRecords.remove(player.getUniqueId());
                return;
            }
            if (!deathInfo.lostItems.isEmpty()) {
                player.sendMessage(ChatColor.RED + "库存不完整，以下物品已丢失:");
                for (Item item : deathInfo.lostItems) {
                    player.sendMessage(Objects.requireNonNull(item.getItemStack().getItemMeta()).getDisplayName() +
                            " x" + item.getItemStack().getAmount());
                }
                player.sendMessage(ChatColor.RED + "输入/salvagepart来恢复剩余物品");
                deathInfo.setAttemptedSalvage(true);
                deathInfo.setLastDeath(System.currentTimeMillis());
                return;
            }
            playerInventory.setContents(deathInfo.drops);
            deathInfo.clearDrops();
            player.setExp(deathInfo.exp);
            player.setLevel(deathInfo.level);
            player.sendMessage(ChatColor.AQUA + "库存已恢复");
            deathRecords.remove(playerUUID);
        } else {
            player.sendMessage(ChatColor.RED + "没有可回收的库存");
        }
    }

    public static void salvagePart(Player player, long currentTime) {
        UUID playerUUID = player.getUniqueId();
        if (deathRecords.containsKey(playerUUID)) {
            DeathInfo deathInfo = deathRecords.get(playerUUID);
            if (!deathInfo.attemptedSalvage) {
                player.sendMessage(ChatColor.RED + "不能直接恢复部分库存，请先输入/salvage来尝试恢复全部库存");
                return;
            }
            PlayerInventory playerInventory = player.getInventory();
            if (currentTime - deathInfo.lastDeath > PayToKeep.getSalvageExpirationTime()) {
                deathRecords.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "你的库存已过期");
                return;
            }
            for (Item item : deathInfo.droppedItems){
                if (playerInventory.firstEmpty() != -1) {
                    playerInventory.addItem(item.getItemStack());
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), item.getItemStack());
                    player.sendMessage(ChatColor.RED + "库存空间不足" + Objects.requireNonNull(item.getItemStack().getItemMeta()).getDisplayName() + "已掉落");
                }
            }
            deathInfo.clearDrops();
            player.setExp(deathInfo.exp);
            player.setLevel(deathInfo.level);
            player.sendMessage(ChatColor.AQUA + "部分库存已恢复");
            deathRecords.remove(playerUUID);
        } else {
            player.sendMessage(ChatColor.RED + "没有可回收的库存");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Map<String, List<Boolean>> data = PayToKeep.getData();
        String playerID = event.getEntity().getUniqueId().toString();
        Player player = event.getEntity();
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();
        if (data.containsKey(playerID)) {
            if (data.get(playerID).get(0) && data.get(playerID).get(1)) {
                event.setKeepInventory(true);
                event.setKeepLevel(true);
                return;
            }
        }
        event.getEntity().sendMessage(String.format("库存丢失，你有%s秒时间来购买库存恢复", PayToKeep.getSalvageExpirationTime() / 1000));
        PlayerInventory playerInventory = player.getInventory();

        DeathInfo deathInfo = new DeathInfo(System.currentTimeMillis(),
                playerInventory.getContents(),
                player.getExp(),
                player.getLevel(),
                player.getLocation(),
                drops
        );
        deathRecords.put(event.getEntity().getUniqueId(), deathInfo);
        deathInfo.dropItems();


    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Map<String, List<Boolean>> data = PayToKeep.getData();
        String playerID = event.getPlayer().getUniqueId().toString();
        if (data.containsKey(playerID)) {
            if (data.get(playerID).get(0) && data.get(playerID).get(1)) {
                data.get(playerID).set(0, false);
                event.getPlayer().sendMessage(ChatColor.AQUA + "库存已恢复");
                event.getPlayer().sendMessage(ChatColor.DARK_RED + "购买已重置");
                saveJsonData(FILE_PATH);
            }
        }
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        Item item = event.getItem();
        for (Map.Entry<UUID, DeathInfo> entry : deathRecords.entrySet()) {
            DeathInfo deathInfo = entry.getValue();
            if (deathInfo.droppedItemContains(item)) {
                deathInfo.removeDroppedItem(item);
                deathInfo.addLostItem(item);
                return;
            }
        }
    }
}
