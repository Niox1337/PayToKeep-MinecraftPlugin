package lol.niox.paytokeep;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static lol.niox.paytokeep.PayToKeep.saveJsonData;

public class PlayerDeathListener implements Listener {
    public static HashMap<UUID, DeathInfo> deathRecords = new HashMap<>();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Map<String, List<Boolean>> data = PayToKeep.getData();
        String playerID = event.getEntity().getUniqueId().toString();
        if (data.containsKey(playerID)) {
            if (data.get(playerID).get(0) && data.get(playerID).get(1)) {
                event.setKeepInventory(true);
                event.setKeepLevel(true);
                event.getDrops().clear();
                return;
            }
        }
        event.getEntity().sendMessage(String.format("库存丢失，你有%s秒时间来购买库存恢复", PayToKeep.getSalvageExpirationTime() / 1000));
        deathRecords.put(event.getEntity().getUniqueId(), new DeathInfo(System.currentTimeMillis(), event.getDrops(), event.getEntity().getExp()));
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
            if (currentTime - deathInfo.lastDeath > PayToKeep.getSalvageExpirationTime()){
                deathRecords.remove(player.getUniqueId());
                player.sendMessage("你的库存已过期");
                return;
            }
            deathInfo.drops.forEach(player.getInventory()::addItem);
            deathInfo.drops.clear();
            player.setExp(deathInfo.exp);
            deathRecords.remove(playerUUID);
            player.sendMessage("库存已回收");
        } else {
            player.sendMessage("没有可回收的库存");
        }
    }
}
