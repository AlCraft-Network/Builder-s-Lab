package scripts.java;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
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

import static dev.lone.itemsadder.api.scriptinginternal.ScriptingUtils.*;

public class hanging_item_frame extends ItemScript {
    private float getYawFromRotation(BlockFace face) {
        return switch (face) {
            case SOUTH -> 180.0f;
            case SOUTH_SOUTH_WEST -> -157.5f;
            case SOUTH_SOUTH_EAST -> 157.5f;
            case SOUTH_WEST -> -135.0f;
            case WEST_SOUTH_WEST -> -112.5f;
            case WEST -> -90.0f;
            case WEST_NORTH_WEST -> -67.5f;
            case NORTH_WEST -> -45.0f;
            case NORTH_NORTH_WEST -> -22.5f;
            case NORTH_NORTH_EAST -> 22.5f;
            case NORTH_EAST -> 45.0f;
            case EAST_NORTH_EAST -> 67.5f;
            case EAST -> 90.0f;
            case EAST_SOUTH_EAST -> 112.5f;
            case SOUTH_EAST -> 135.0f;
            default -> 0.0f;
        };
    }

    private void cancelEvent(Event event) {
        try {
            if (event instanceof Cancellable e) e.setCancelled(true);
        } catch (Exception ignored) {}
    }

    @Override
    public void handleEvent(Plugin plugin, Event event, Player player, CustomStack customStack, ItemStack vanillaItem) {
        if (event instanceof PlayerInteractEvent interactEvent) {
            var clickedBlock = interactEvent.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getType().name().contains("HANGING_SIGN")) {
                cancelEvent(event);
                if (CustomFurniture.byAlreadySpawned(clickedBlock.getLocation().getBlock()) != null) return;

                var itemToPlace = vanillaItem.clone();
                itemToPlace.setAmount(2);

                var data = clickedBlock.getBlockData();
                var loc = clickedBlock.getLocation();
                var rotation = data instanceof Directional ? ((Directional) data).getFacing() : ((Rotatable) data).getRotation();
                loc.setYaw(getYawFromRotation(rotation));

                vanillaItem.setAmount(vanillaItem.getAmount() - 1);

                _runDelayed(2, () -> {
                    var furniture = CustomFurniture.spawnPreciseNonSolid(customStack.getNamespacedID() + "_interact", loc.add(0.5, 0, 0.5));
                    var hangingFrame = furniture.getEntity();
                    for (int i = 0; i <= 2; i++) {
                        var display = hangingFrame.getWorld().spawn(hangingFrame.getLocation(), ItemDisplay.class);
                        display.setItemStack(itemToPlace);
                        display.setTransformation(new Transformation(new Vector3f(0.0f, 0.3f, i == 1 ? -0.08f : 0.08f), new Quaternionf().rotateY((float) Math.toRadians(i == 1 ? 0.0f : -180.0f)), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf()));
                        hangingFrame.addPassenger(display);
                    }
                });
            }
        }
    }
}
