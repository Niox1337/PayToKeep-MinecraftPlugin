package lol.niox.paytokeep;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;
import java.util.Map;

public class PlayerDeathListener implements Listener {
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

    }
}
