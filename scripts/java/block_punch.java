package scripts.java;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.scriptinginternal.ItemScript;
import static dev.lone.itemsadder.api.scriptinginternal.ItemsUtils.*;
import static dev.lone.itemsadder.api.scriptinginternal.WorldUtils.*;
import static dev.lone.itemsadder.api.scriptinginternal.WorldUtils.isCustom;
import static dev.lone.itemsadder.api.scriptinginternal.ScriptingUtils.*;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.session.SessionManager;

public class block_punch extends ItemScript {
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

    private void decrementDurability(ItemStack item, CustomStack customStack, Player player) {
        var meta = (Damageable) item.getItemMeta();
        if (meta.getDamage() + 1 >= customStack.getMaxDurability()) {
            playSound(player.getLocation(), "minecraft:entity.item.break");
            player.getWorld().spawnParticle(Particle.ITEM, player.getLocation().add(0, 1.0, 0), 15, 0.2, 0.2, 0.2, 0.1, item);
            item.setAmount(0);
        } else {
            meta.setDamage(meta.getDamage() + 1);
            item.setItemMeta(meta);
        }
    }

    @Override
    public void handleEvent(Plugin plugin, Event event, Player player, CustomStack customStack, ItemStack vanillaItem) {
        if (event instanceof PlayerInteractEvent interactEvent && interactEvent.hasBlock()) {
            var clickedBlock = interactEvent.getClickedBlock();
            var loc = clickedBlock.getLocation();

            if (clickedBlock.getPistonMoveReaction() != PistonMoveReaction.MOVE || !canBuild(player, clickedBlock)) return;

            switch (interactEvent.getBlockFace()) {
                case UP -> loc.add(0.0, -1.0, 0.0);
                case DOWN -> loc.add(0.0, 1.0, 0.0);
                case EAST -> loc.add(-1.0, 0.0, 0.0);
                case WEST -> loc.add(1.0, 0.0, 0.0);
                case SOUTH -> loc.add(0.0, 0.0, -1.0);
                default -> loc.add(0.0, 0.0, 1.0);
            }

            var newBlock = block(loc);
            if ((newBlock.getType() != Material.AIR && newBlock.getType() != Material.LIGHT) || !canBuild(player, newBlock)) return;

            if (isCustom(clickedBlock)) {
                var custom = customBlock(clickedBlock);
                _runDelayed(1, () -> {
                    custom.playBreakParticles();
                    custom.playBreakSound();
                    custom.remove();
                    custom.place(loc);
                });
            } else {
                loc.getWorld().spawnParticle(Particle.ITEM, clickedBlock.getLocation().add(0.5, 0.5, 0.5), 16, 0.2, 0.2, 0.2, 0.1, newStack(clickedBlock.getType()));
                loc.getWorld().playSound(clickedBlock.getLocation(), clickedBlock.getBlockData().getSoundGroup().getBreakSound(), 1.0f, 1.0f);
                placeBlock(newBlock, clickedBlock.getType());
                removeBlock(clickedBlock);
            }

            if (player.getGameMode() != GameMode.CREATIVE) decrementDurability(vanillaItem, customStack, player);
        }
    }
}
