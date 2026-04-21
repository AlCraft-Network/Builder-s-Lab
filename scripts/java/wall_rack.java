package scripts.java;

import java.util.Map;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.scriptinginternal.ItemScript;

import static dev.lone.itemsadder.api.scriptinginternal.PlayerUtils.*;

public class wall_rack extends ItemScript {
    private void getBackItem(Player player, ItemStack handItem, ItemDisplay displayEntity) {
        ItemStack item = displayEntity.getItemStack();

        if (handItem.getType().isAir()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            leftover.values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
        }
    }

    private void cancelEvent(Event event) {
        try {
            if (event instanceof Cancellable e) e.setCancelled(true);
        } catch (Exception ignored) {}
    }

    @Override
    public void handleEvent(Plugin plugin, Event event, Player player, CustomStack customStack, ItemStack vanillaItem) {
        // On interact furniture
        if (event instanceof PlayerInteractEvent) {
            cancelEvent(event);

            CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entityInFront(player));
            if (furniture == null) return;

            var rack = furniture.getEntity();
            var handItem = player.getInventory().getItemInMainHand();
            var itemDisplays = rack.getPassengers();

            if (!handItem.getType().isAir() && !handItem.getType().isBlock()) {
                var itemToPlace = handItem.clone();
                itemToPlace.setAmount(1);

                handItem.setAmount(handItem.getAmount() - 1);

                if (itemDisplays.isEmpty()) {
                    var display = rack.getWorld().spawn(rack.getLocation(), ItemDisplay.class);
                    display.setItemStack(itemToPlace);
                    display.setTransformation(new Transformation(new Vector3f(0.0f, 0.2f, -0.05f), new Quaternionf().rotateX((float) Math.toRadians(45.0f)).rotateZ((float) Math.toRadians(45.0f)), new Vector3f(0.8f, 0.8f, 0.8f), new Quaternionf()));
                    rack.addPassenger(display);
                } else {
                    var lastDisplay = (ItemDisplay) itemDisplays.get(0);
                    getBackItem(player, handItem, lastDisplay);
                    lastDisplay.setItemStack(itemToPlace);
                }
            } else if (!itemDisplays.isEmpty()) {
                var lastDisplay = (ItemDisplay) itemDisplays.get(0);
                getBackItem(player, handItem, lastDisplay);
                lastDisplay.remove();
            }
        } else {
            CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entityInFront(player));
            if (furniture == null) return;

            Location loc = furniture.getEntity().getLocation();
            var itemDisplays = furniture.getEntity().getPassengers().stream().map(e -> (ItemDisplay) e).toList();

            itemDisplays.forEach(display -> {
                loc.getWorld().dropItemNaturally(loc, display.getItemStack());
                display.remove();
            });
        }
    }
}
