package lol.niox.paytokeep;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DeathInfo {
    public long lastDeath;
    public List<ItemStack> drops;
    public float exp;
    public Location location;

    public DeathInfo(long lastDeath, List<ItemStack> drops, float exp, Location location) {
        this.lastDeath = lastDeath;
        this.drops = drops;
        this.exp = exp;
        this.location = location;
    }
}
