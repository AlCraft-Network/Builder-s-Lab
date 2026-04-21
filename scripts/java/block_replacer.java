package scripts.java;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
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

public class block_replacer extends ItemScript {
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
            if (!clickedBlock.getType().isBlock() || clickedBlock.getType().getHardness() < 0 || clickedBlock.getType().getHardness() >= 50.0) return;

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
