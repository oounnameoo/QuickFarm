package com.example.quickfarm;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class QuickFarmPlugin extends JavaPlugin implements Listener {

    /**
     * Maps each crop's block material to the seed material needed to replant it.
     *
     *  Crop block       | Seed / replant item
     * ──────────────────┼──────────────────────
     *  WHEAT            | WHEAT_SEEDS
     *  CARROTS          | CARROT        (the item IS the seed)
     *  POTATOES         | POTATO        (the item IS the seed)
     *  BEETROOTS        | BEETROOT_SEEDS
     *  NETHER_WART      | NETHER_WART   (the item IS the seed)
     */
    private static final Map<Material, Material> CROP_SEEDS = new EnumMap<>(Material.class);

    static {
        CROP_SEEDS.put(Material.WHEAT,       Material.WHEAT_SEEDS);
        CROP_SEEDS.put(Material.CARROTS,     Material.CARROT);
        CROP_SEEDS.put(Material.POTATOES,    Material.POTATO);
        CROP_SEEDS.put(Material.BEETROOTS,   Material.BEETROOT_SEEDS);
        CROP_SEEDS.put(Material.NETHER_WART, Material.NETHER_WART);
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("QuickFarm enabled.");
        getLogger().info("  Hold the correct seed in your main hand, then right-click a fully grown crop");
        getLogger().info("  to harvest it and instantly replant using 1 seed from your hand.");
        getLogger().info("  Supported: Wheat, Carrots, Potatoes, Beetroots, Nether Wart");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only care about right-clicking an actual block face
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        // Is this one of our supported crops?
        Material seedMaterial = CROP_SEEDS.get(block.getType());
        if (seedMaterial == null) return;

        // Is the crop fully grown?
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() < ageable.getMaximumAge()) return;

        Player player = (Player) event.getPlayer();

        // ── Gate: the player must be holding the correct seed in their main hand ──
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() != seedMaterial) return;

        // Cancel vanilla right-click (prevents accidentally placing blocks / opening GUIs)
        event.setCancelled(true);

        // ── Step 1: Collect natural drops ────────────────────────────────────────
        // Use AIR as the "tool" so the held seed doesn't influence fortune loot tables
        List<ItemStack> drops = new ArrayList<>(block.getDrops(new ItemStack(Material.AIR), player));

        // ── Step 2: Consume exactly 1 seed from the player's hand ────────────────
        if (heldItem.getAmount() > 1) {
            heldItem.setAmount(heldItem.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // ── Step 3: Replant — reset the crop age to 0 (farmland stays intact) ───
        ageable.setAge(0);
        block.setBlockData(ageable);

        // Play the vanilla crop-break sound so the harvest feels satisfying
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CROP_BREAK, 1.0f, 1.0f);

        // ── Step 4: Drop items on the ground at the crop's location ─────────────
        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(dropLoc, drop);
        }
    }
}
