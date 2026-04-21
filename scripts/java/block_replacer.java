package scripts.java;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.scriptinginternal.ItemScript;
import static dev.lone.itemsadder.api.scriptinginternal.WorldUtils.*;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.session.SessionManager;

public class block_replacer extends ItemScript {
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

    private void cancelEvent(Event event) {
        try {
            if (event instanceof Cancellable e) e.setCancelled(true);
        } catch (Exception ignored) {}
    }

    @Override
    public void handleEvent(Plugin plugin, Event event, Player player, CustomStack customStack, ItemStack vanillaItem) {
        cancelEvent(event);

        if (event instanceof PlayerInteractEvent interactEvent && interactEvent.hasBlock()) {
            var clickedBlock = interactEvent.getClickedBlock();
            if (!clickedBlock.getType().isBlock() || clickedBlock.getType().getHardness() < 0 || clickedBlock.getType().getHardness() >= 50.0 || !canBuild(player, clickedBlock)) return;

            var secondaryItem = player.getInventory().getItemInOffHand();
            CustomBlock custom = CustomBlock.byItemStack(secondaryItem);
            if (secondaryItem.getType() == Material.AIR || !(secondaryItem.getType().isBlock() || custom != null)) return;

            if (isCustom(clickedBlock)) {
                var customBlock = customBlock(clickedBlock);
                clickedBlock.getLocation().getWorld().dropItemNaturally(clickedBlock.getLocation(), customBlock.getItemStack());
                customBlock.playBreakSound();
            } else {
                breakBlockNaturally(player, clickedBlock);
                playSound(clickedBlock.getLocation(), clickedBlock.getBlockData().getSoundGroup().getBreakSound().getKey().getKey());
            }

            if (custom != null) {
                custom.place(clickedBlock.getLocation());
            } else if (secondaryItem.getType().isBlock()) {
                placeBlock(clickedBlock, secondaryItem.getType());
            }

            if (player.getGameMode() != GameMode.CREATIVE) {
                secondaryItem.setAmount(secondaryItem.getAmount() - 1);
                decrementDurability(vanillaItem, customStack, player);
            }
        }
    }
}
