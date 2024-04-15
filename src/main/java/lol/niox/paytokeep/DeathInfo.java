package lol.niox.paytokeep;

import org.bukkit.Location;
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
    public List<Item> lostItems = new ArrayList<>();
    public boolean attemptedSalvage = false;

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

    public void addLostItem(Item item) {
        lostItems.add(item);
    }

    public void removeDroppedItem(Item item) {
        droppedItems.remove(item);
    }

    public boolean droppedItemContains(Item item) {
        return droppedItems.contains(item);
    }

    public void setAttemptedSalvage(boolean attemptedSalvage) {
        this.attemptedSalvage = attemptedSalvage;
    }

    public void setLastDeath(long lastDeath) {
        this.lastDeath = lastDeath;
    }
}
