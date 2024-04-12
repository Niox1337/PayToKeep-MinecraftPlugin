package lol.niox.paytokeep;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DeathInfo {
    public long lastDeath;
    public ItemStack[] drops;
    public float exp;
    public int level;
    public Location location;
    public ItemStack[] equipment;
    public ItemStack offHand;

    public DeathInfo(long lastDeath, ItemStack[] drops, float exp, int level, Location location, ItemStack[] equipment, ItemStack offHand) {
        this.lastDeath = lastDeath;
        this.drops = drops;
        this.exp = exp;
        this.level = level;
        this.location = location;
        this.equipment = equipment;
        this.offHand = offHand;
    }
}
