package scripts.java;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.bukkit.Axis;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;
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
    private static final Random RANDOM = new Random();

    private static final Set<String> EXCLUDED_NAMES = Set.of("CACTUS", "OBSERVER", "AMETHYST_BUD", "AMETHYST_CLUSTER", "LARGE_AMETHYST_BUD", "SMALL_AMETHYST_BUD", "POINTED_DRIPSTONE", "SCULK_VEIN", "GRINDSTONE");

    private boolean isPlaceable(ItemStack item) {
        Material type = item.getType();
        if (!type.isBlock()) return false;
        if (type.createBlockData().createBlockState() instanceof org.bukkit.block.TileState) return false;
        if (isExcludedByName(type)) return false;
        if (type.createBlockData() instanceof Rotatable) return false;
        return CustomBlock.byItemStack(item) != null || type.isSolid();
    }

    private boolean isExcludedByName(Material material) {
        String name = material.name();
        return EXCLUDED_NAMES.contains(name)
        || name.endsWith("_LANTERN")
        || name.endsWith("_LIGHTNING_ROD")
        || name.endsWith("_PRESSURE_PLATE")
        || name.endsWith("_DOOR")
        || name.endsWith("_SHULKER_BOX")
        || name.endsWith("_BED");
    }

    private boolean isTopHalfClick(PlayerInteractEvent e) {
        if (e.getBlockFace() == BlockFace.DOWN) return true;
        if (e.getBlockFace() == BlockFace.UP) return false;

        var interactionPoint = e.getInteractionPoint();
        if (interactionPoint == null) return false;

        double yInBlock = interactionPoint.getY() - interactionPoint.getBlockY();
        return yInBlock > 0.5;
    }

    private BlockData getBlockData(Material material, PlayerInteractEvent e, Player player) {
        BlockData data = material.createBlockData();
        BlockFace against = e.getBlockFace();
        boolean topHalf = isTopHalfClick(e);

        if (data instanceof Bisected bisected) {
            bisected.setHalf(topHalf ? Bisected.Half.TOP : Bisected.Half.BOTTOM);
        } else if (data instanceof Slab slab) {
            slab.setType(topHalf ? Slab.Type.TOP : Slab.Type.BOTTOM);
        }

        if (data instanceof Directional directional) {
            applyDirectional(directional, against, player);
        } else if (data instanceof Orientable orientable) {
            orientable.setAxis(axisFromFace(against));
        }
        
        return data;
    }

    private Axis axisFromFace(BlockFace face) {
        return switch (face) {
            case UP, DOWN -> Axis.Y;
            case EAST, WEST -> Axis.X;
            default -> Axis.Z;
        };
    }

    private void applyDirectional(Directional directional, BlockFace against, Player player) {
        BlockFace playerFacing = player.getFacing();
        BlockFace facing;

        if (directional instanceof Stairs) {
            facing = playerFacing; 
        } else {
            facing = (against == BlockFace.UP || against == BlockFace.DOWN) ? playerFacing.getOppositeFace() : against;
        }

        try {
            directional.setFacing(facing);
        } catch (IllegalArgumentException e) {
            directional.setFacing(player.getFacing().getOppositeFace());
        }
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
            List<ItemStack> blocks = Arrays.stream(player.getInventory().getContents(), 0, 9)
                .filter(Objects::nonNull).filter(this::isPlaceable).toList();

            if (blocks.isEmpty()) return;

            ItemStack randomBlock = blocks.get(RANDOM.nextInt(blocks.size()));
            var clickedBlock = interactEvent.getClickedBlock();
            var newBlock = clickedBlock.getRelative(interactEvent.getBlockFace());
            
            if (newBlock.getType() != Material.AIR && newBlock.getType() != Material.LIGHT) return;

            CustomBlock custom = CustomBlock.byItemStack(randomBlock);
            if (custom != null) {
                placeBlock(newBlock, custom);
                custom.playPlaceSound();
            } else {
                BlockData data = getBlockData(randomBlock.getType(), interactEvent, player);
                newBlock.setBlockData(data, true);
                playSound(newBlock.getLocation(), newBlock.getBlockData().getSoundGroup().getPlaceSound().getKey().getKey());
            }

            if (player.getGameMode() != GameMode.CREATIVE) {
                randomBlock.setAmount(randomBlock.getAmount() - 1);
                decrementDurability(vanillaItem, customStack, player);
            }
        }
    }
}
