package lol.niox.paytokeep;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeathInfo {
    public long lastDeath;
    public ItemStack[] drops;
    public float exp;
    public int level;
    public Location location;
    public List<ItemStack> realDrops;
    public List<Item> droppedItems = new ArrayList<>();

    public DeathInfo(long lastDeath, ItemStack[] drops, float exp, int level, Location location, List<ItemStack> realDrops) {
        this.lastDeath = lastDeath;
        this.drops = drops;
        this.exp = exp;
        this.level = level;
        this.location = location;
        this.realDrops = realDrops;
    }

    public void clearDrops() {
        for (Item item : droppedItems) {
            item.remove();
        }
    }

    public void dropItems() {
        for (ItemStack item : realDrops) {
            droppedItems.add(Objects.requireNonNull(location.getWorld()).dropItemNaturally(location, item));
        }
    }
}
