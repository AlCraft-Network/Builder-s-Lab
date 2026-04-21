package scripts.java;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.scriptinginternal.ItemScript;

import static dev.lone.itemsadder.api.scriptinginternal.ItemsUtils.*;
import static dev.lone.itemsadder.api.scriptinginternal.PlayerUtils.*;

public class hanging_item_frame_interact extends ItemScript {
    private void cancelEvent(Event event) {
        try {
            if (event instanceof Cancellable e) e.setCancelled(true);
        } catch (Exception ignored) {}
    }

    @Override
    public void handleEvent(Plugin plugin, Event event, Player player, CustomStack customStack, ItemStack vanillaItem) {
        // On interact furniture
        if (event instanceof PlayerInteractEvent interactEvent) {
            var clickedBlock = interactEvent.getClickedBlock();
            var handItem = player.getInventory().getItemInMainHand();
            if (!clickedBlock.getType().name().contains("HANGING_SIGN") || handItem.getType() == Material.AIR) return;

            var furniture = CustomFurniture.byAlreadySpawned(clickedBlock);
            var itemDisplays = furniture.getEntity().getPassengers().stream().map(e -> (ItemDisplay) e).toList();

            var itemToPlace = handItem.clone();
            itemToPlace.setAmount(1);
            
            handItem.setAmount(handItem.getAmount()-1);
            var itemToReturn = itemDisplays.get(0).getItemStack();
            if (itemToReturn.getAmount() == 1) clickedBlock.getWorld().dropItemNaturally(clickedBlock.getLocation(), itemToReturn);
            itemDisplays.forEach(display -> display.setItemStack(itemToPlace));
        // On interact furniture
        } else {
            cancelEvent(event);

            CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entityInFront(player));
            if (furniture == null) return;

            Location loc = furniture.getEntity().getLocation();
            var itemDisplays = furniture.getEntity().getPassengers().stream().map(e -> (ItemDisplay) e).toList();

            var itemToReturn = itemDisplays.get(0).getItemStack();
            if (!itemDisplays.isEmpty() && itemToReturn.getAmount() == 1) loc.getWorld().dropItemNaturally(loc, itemToReturn);
            loc.getWorld().dropItemNaturally(loc, customStack(furniture.getNamespacedID().replaceFirst("_interact$", "")).getItemStack());
            itemDisplays.forEach(display -> display.remove());
            furniture.getEntity().remove();
        }
    }
}
