package scripts.java;

import java.util.Map;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
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
import static dev.lone.itemsadder.api.scriptinginternal.WorldUtils.*;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.session.SessionManager;

public class wall_rack extends ItemScript {
    private boolean canBuild(Player player, Block block) {
        Location loc = block.getLocation();
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        SessionManager sessionManager = WorldGuard.getInstance().getPlatform().getSessionManager();
        RegionQuery query = container.createQuery();
        if (!sessionManager.hasBypass(localPlayer,  BukkitAdapter.adapt(loc.getWorld()))) {
            return query.testBuild(BukkitAdapter.adapt(loc), localPlayer);
        }
        return true;
    }

    private record DisplaySettings(Vector3f offset, Vector3f scale, float rotX, float rotY, float rotZ) {}
    private DisplaySettings settings(float offx, float offy, float offz, float scale, float rotX, float rotY, float rotZ) {
        return new DisplaySettings(new Vector3f(offx, offy, offz), new Vector3f(scale, scale, scale), rotX, rotY, rotZ);
    }

    private final DisplaySettings DEFAULT_SETTINGS = settings(0.0f, 0.2f, -0.05f, 0.8f, 45.0f, 0.0f, 45.0f);
    private final Map<String, DisplaySettings> ITEM_SETTINGS = Map.ofEntries(
        Map.entry("minecraft:stone_spear", settings(0.0f, 0.2f, -0.05f, 1.5f, 45.0f, 0.0f, -225.0f)),
        Map.entry("minecraft:copper_spear", settings(0.0f, 0.2f, -0.05f, 1.5f, 45.0f, 0.0f, -225.0f)),
        Map.entry("minecraft:iron_spear", settings(0.0f, 0.2f, -0.05f, 1.5f, 45.0f, 0.0f, -225.0f)),
        Map.entry("minecraft:golden_spear", settings(0.0f, 0.2f, -0.05f, 1.5f, 45.0f, 0.0f, -225.0f)),
        Map.entry("minecraft:diamond_spear", settings(0.0f, 0.2f, -0.05f, 1.5f, 45.0f, 0.0f, -225.0f)),
        Map.entry("minecraft:netherite_spear", settings(0.0f, 0.2f, -0.05f, 1.5f, 45.0f, 0.0f, -225.0f)),
        Map.entry("minecraft:trident", settings(-1.25f, 0.2f, -0.73f, 1.0f, 45.0f, 0.0f, 90.0f)),
        Map.entry("minecraft:shield", settings(-0.5f, 0.3f, -0.75f, 1.0f, 45.0f, 0.0f, 90.0f)),
        Map.entry("minecraft:spyglass", settings(0.0f, 0.15f, -0.05f, 1.0f, -45.0f, 0.0f, -90.0f))
    );

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

    private String getItemKey(ItemStack item) {
        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack != null) return customStack.getNamespacedID();

        Material material = item.getType();
        return "minecraft:" + material.name().toLowerCase();
    }

    private Transformation getTransformationFor(ItemStack item) {
        DisplaySettings settings = ITEM_SETTINGS.getOrDefault(getItemKey(item), DEFAULT_SETTINGS);
        Quaternionf leftRotation = new Quaternionf()
            .rotateX((float) Math.toRadians(settings.rotX()))
            .rotateY((float) Math.toRadians(settings.rotY()))
            .rotateZ((float) Math.toRadians(settings.rotZ()));

        return new Transformation(new Vector3f(settings.offset()), leftRotation, new Vector3f(settings.scale()), new Quaternionf());
    }

    @Override
    public void handleEvent(Plugin plugin, Event event, Player player, CustomStack customStack, ItemStack vanillaItem) {
        // On interact furniture
        if (event instanceof PlayerInteractEvent) {
            cancelEvent(event);

            CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entityInFront(player));
            if (furniture == null || !canBuild(player, block(furniture.getEntity().getLocation()))) return;


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
                    display.setTransformation(getTransformationFor(itemToPlace));
                    rack.addPassenger(display);
                } else {
                    var lastDisplay = (ItemDisplay) itemDisplays.get(0);
                    getBackItem(player, handItem, lastDisplay);
                    lastDisplay.setItemStack(itemToPlace);
                    lastDisplay.setTransformation(getTransformationFor(itemToPlace));
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
