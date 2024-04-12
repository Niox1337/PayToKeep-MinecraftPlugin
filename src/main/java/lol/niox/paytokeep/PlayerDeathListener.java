package lol.niox.paytokeep;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.stream.Collectors;

import static lol.niox.paytokeep.PayToKeep.saveJsonData;

public class PlayerDeathListener implements Listener {
    public static HashMap<UUID, DeathInfo> deathRecords = new HashMap<>();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Map<String, List<Boolean>> data = PayToKeep.getData();
        String playerID = event.getEntity().getUniqueId().toString();
        Player player = event.getEntity();
        if (data.containsKey(playerID)) {
            if (data.get(playerID).get(0) && data.get(playerID).get(1)) {
                event.setKeepInventory(true);
                event.setKeepLevel(true);
                event.getDrops().clear();
                return;
            }
        }
        event.getEntity().sendMessage(String.format("库存丢失，你有%s秒时间来购买库存恢复", PayToKeep.getSalvageExpirationTime() / 1000));
        deathRecords.put(event.getEntity().getUniqueId(),
                new DeathInfo(System.currentTimeMillis(),
                        event.getDrops(),
                        player.getExp(),
                        player.getLocation(),
                        player.getInventory().getArmorContents(),
                        player.getInventory().getItemInOffHand()
                ));
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Map<String, List<Boolean>> data = PayToKeep.getData();
        String playerID = event.getPlayer().getUniqueId().toString();
        if (data.containsKey(playerID)) {
            if (data.get(playerID).get(0) && data.get(playerID).get(1)) {
                data.get(playerID).set(0, false);
                event.getPlayer().sendMessage("库存已恢复");
                saveJsonData("./PayToKeepData/data.json");
            }
        }
    }

    public static void Salvage(Player player, long currentTime) {
        UUID playerUUID = player.getUniqueId();
        if (deathRecords.containsKey(playerUUID)) {
            DeathInfo deathInfo = deathRecords.get(playerUUID);
            PlayerInventory playerInventory = player.getInventory();
            if (currentTime - deathInfo.lastDeath > PayToKeep.getSalvageExpirationTime()){
                deathRecords.remove(player.getUniqueId());
                player.sendMessage("你的库存已过期");
                return;
            }
            List<Entity> entities = Objects.requireNonNull(deathInfo.location.getWorld())
                    .getNearbyEntities(deathInfo.location, 2, 300, 2)
                    .stream()
                    .filter(entity -> entity instanceof Item)
                    .collect(Collectors.toList());
            entities.forEach(Entity::remove);
            deathInfo.drops.forEach(playerInventory::addItem);
            player.setExp(deathInfo.exp);
            playerInventory.setArmorContents(deathInfo.equipment);
            playerInventory.setItemInOffHand(deathInfo.offHand);
            deathRecords.remove(playerUUID);
            player.sendMessage("库存已回收");
        } else {
            player.sendMessage("没有可回收的库存");
        }
    }
}
