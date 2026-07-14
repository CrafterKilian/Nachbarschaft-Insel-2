package de.nachbarschaftsinsel.randomtp;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class RandomTpPlugin extends JavaPlugin {

    /** Maximale Würfelversuche pro Spieler, bevor aufgegeben wird. */
    private static final int MAX_ATTEMPTS = 30;

    /** Blöcke, auf denen nicht gelandet werden darf, obwohl sie ggf. solide sind. */
    private static final Set<Material> UNSAFE_GROUND = EnumSet.of(
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.POWDER_SNOW,
            Material.POINTED_DRIPSTONE
    );

    @Override
    public void onEnable() {
        saveDefaultConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("start")) {
            return false;
        }
        if (!sender.hasPermission("randomtp.admin")) {
            sender.sendMessage(Component.text("Dazu hast du keine Berechtigung.", NamedTextColor.RED));
            return true;
        }

        int radius = Math.max(1, getConfig().getInt("radius", 2000));
        boolean sendMessage = getConfig().getBoolean("send-message", true);

        List<Player> players = List.copyOf(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) {
            sender.sendMessage(Component.text("Es sind keine Spieler online.", NamedTextColor.YELLOW));
            return true;
        }

        int teleported = 0;
        int failed = 0;
        for (Player player : players) {
            Location target = findSafeLocation(player.getWorld(), radius);
            if (target == null) {
                failed++;
                continue;
            }
            player.teleportAsync(target).thenAccept(success -> {
                if (success && sendMessage) {
                    player.sendMessage(Component.text("Du wurdest teleportiert!", NamedTextColor.GREEN));
                }
            });
            teleported++;
        }

        sender.sendMessage(Component.text(
                teleported + (teleported == 1 ? " Spieler wurde" : " Spieler wurden") + " teleportiert.",
                NamedTextColor.GREEN));
        if (failed > 0) {
            sender.sendMessage(Component.text(
                    "Für " + failed + " Spieler wurde keine sichere Position gefunden.",
                    NamedTextColor.YELLOW));
        }
        return true;
    }

    /**
     * Würfelt bis zu {@link #MAX_ATTEMPTS} zufällige X/Z-Positionen im Radius um den
     * Weltspawn und gibt die erste sichere Position zurück (oder null, falls keine
     * gefunden wurde). Positionen außerhalb der World Border werden verworfen.
     */
    private Location findSafeLocation(World world, int radius) {
        Location spawn = world.getSpawnLocation();
        WorldBorder border = world.getWorldBorder();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int x = spawn.getBlockX() + random.nextInt(-radius, radius + 1);
            int z = spawn.getBlockZ() + random.nextInt(-radius, radius + 1);

            if (!border.isInside(new Location(world, x + 0.5, spawn.getY(), z + 0.5))) {
                continue;
            }

            int y = world.getHighestBlockYAt(x, z);
            if (y <= world.getMinHeight()) {
                continue; // Void bzw. keine feste Oberfläche (z. B. leere Spalte)
            }

            Block ground = world.getBlockAt(x, y, z);
            if (!ground.getType().isSolid() || UNSAFE_GROUND.contains(ground.getType())) {
                continue;
            }

            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);
            if (feet.isLiquid() || head.isLiquid() || !feet.isPassable() || !head.isPassable()) {
                continue;
            }

            return new Location(world, x + 0.5, y + 1.0, z + 0.5);
        }
        return null;
    }
}
