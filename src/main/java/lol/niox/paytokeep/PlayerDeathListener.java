package lol.niox.paytokeep;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
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
//            List<Entity> entities = new ArrayList<>(Objects.requireNonNull(deathInfo.location.getWorld())
//                    .getNearbyEntities(deathInfo.location, 5, 50, 5));
//            List<Material> playerDroppedItems = new ArrayList<>();
//            List<Material> deathInfoDropTypes = Arrays.stream(deathInfo.drops)
//                    .filter(Objects::nonNull)
//                    .map(ItemStack::getType)
//                    .collect(Collectors.toList());
//            for (Entity entity : entities) {
//                if (entity instanceof Item) {
//                    Material itemType = ((Item) entity).getItemStack().getType();
//                    if (deathInfoDropTypes.contains(itemType)) {
//                        playerDroppedItems.add(itemType);
//                    }
//                }
//            }
//            if (new HashSet<>(playerDroppedItems).containsAll(deathInfoDropTypes)) {
//                for (Entity entity : entities) {
//                    if (entity instanceof Item) {
//                        Material itemType = ((Item) entity).getItemStack().getType();
//                        if (deathInfoDropTypes.contains(itemType)) {
//                            entity.remove();
//                        }
//                    }
//                }

            playerInventory.setContents(deathInfo.drops);
            deathInfo.clearDrops();
            player.setExp(deathInfo.exp);
            player.setLevel(deathInfo.level);
            player.sendMessage(ChatColor.AQUA + "库存已恢复");
            deathRecords.remove(playerUUID);
//            } else {
//                player.sendMessage(ChatColor.RED + "库存不完整");
//            }
//            deathRecords.remove(playerUUID);
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
        Entity entity = event.getEntity();
        for (DeathInfo deathInfo : deathRecords.values()) {
            if (deathInfo.droppedItems.contains(item)) {
                event.setCancelled(true);
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    player.sendMessage(ChatColor.RED + "你不能拾取这个物品");
                }
            }
        }
    }
}
