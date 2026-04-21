package scripts.java;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.scriptinginternal.ItemScript;

import static dev.lone.itemsadder.api.scriptinginternal.WorldUtils.*;

public class random_block_placer extends ItemScript {
    private static final Set<Material> inventoryBlocks = Set.of(Material.CHEST, Material.BARREL, Material.FURNACE, Material.DISPENSER, Material.DROPPER, Material.HOPPER, 
    Material.BLAST_FURNACE, Material.SMOKER, Material.BREWING_STAND, Material.SHULKER_BOX, Material.TRAPPED_CHEST, Material.ENDER_CHEST);

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
            List<ItemStack> blocks = Arrays.stream(player.getInventory().getContents(), 0, 9)
                .filter(Objects::nonNull).filter(item -> item.getType().isBlock() || CustomBlock.byItemStack(item) != null)
                .filter(item -> !inventoryBlocks.contains(item.getType())).toList();

            ItemStack randomBlock = blocks.get(new Random().nextInt(blocks.size()));
            var clickedBlock = interactEvent.getClickedBlock();
            var newBlock = clickedBlock.getRelative(interactEvent.getBlockFace());

            if (newBlock.getType() != Material.AIR && newBlock.getType() != Material.LIGHT) return;

            CustomBlock custom = CustomBlock.byItemStack(randomBlock);
            if (custom != null) {
                placeBlock(newBlock, custom);
                custom.playPlaceSound();
            } else {
                placeBlock(newBlock, randomBlock.getType());
                playSound(newBlock.getLocation(), newBlock.getBlockData().getSoundGroup().getBreakSound().getKey().getKey());
            }

            if (player.getGameMode() != GameMode.CREATIVE) {
                randomBlock.setAmount(randomBlock.getAmount() - 1);
                decrementDurability(vanillaItem, customStack, player);
            }
        }
    }
}
