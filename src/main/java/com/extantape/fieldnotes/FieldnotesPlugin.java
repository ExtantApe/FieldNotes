package com.extantape.fieldnotes;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * /bug notes, and a short rolling record of each watched player's recent actions, written so that as little as
 * possible is lost if the game or the server stops suddenly. See the README for what can't be caught.
 */
public final class FieldnotesPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, Recent> recent = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Map<UUID, String> kickReasons = new ConcurrentHashMap<>();
    private int max;
    private Path dir;
    private Thread shutdownHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        max = Math.max(10, Math.min(1000, getConfig().getInt("recent-actions", 80)));
        int flush = Math.max(1, Math.min(60, getConfig().getInt("flush-seconds", 5)));
        dir = getDataPath();
        getServer().getPluginManager().registerEvents(this, this);
        for (Player p : getServer().getOnlinePlayers()) watch(p, "already online when Fieldnotes started");
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::saveChanged, 20L * flush, 20L * flush);
        // Catches a normal JVM exit (a stop, Ctrl-C, a crash that unwinds). A kill -9 or power loss can't be caught;
        // the timer above is what limits the loss then.
        shutdownHook = new Thread(() -> saveAll("the server process is shutting down"), "Fieldnotes save on exit");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, e ->
                e.registrar().register("bug", "Leave a private note for the people running this test.", List.of("fieldnote"), new BugCommand()));
    }

    @Override
    public void onDisable() {
        saveAll("the plugin was disabled (server stopping or reloading)");
        try {
            if (shutdownHook != null) Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException alreadyShuttingDown) {
            // The hook is running or has run; nothing to remove.
        }
    }

    private final class BugCommand implements BasicCommand {
        @Override
        public void execute(CommandSourceStack source, String[] args) {
            if (!(source.getSender() instanceof Player p)) {
                source.getSender().sendMessage("Only players can leave a note.");
                return;
            }
            if (!p.hasPermission("fieldnotes.note")) {
                p.sendMessage(Component.text("You can't leave notes here.", NamedTextColor.RED));
                return;
            }
            String text = String.join(" ", args);
            if (text.isBlank()) {
                p.sendMessage(Component.text("Say what happened: /bug the boat sank when I jumped", NamedTextColor.YELLOW));
                return;
            }
            try {
                Files.append(dir.resolve("notes.log"), List.of(noteLine(p, text, System.currentTimeMillis())));
                record(p, "left a note");
                save(p.getUniqueId(), null);
                p.sendMessage(Component.text("Noted, thank you. Only the people running the test can read it.", NamedTextColor.GREEN));
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Couldn't save a note", e);
                p.sendMessage(Component.text("Your note couldn't be saved. Please tell someone running the test.", NamedTextColor.RED));
            }
        }

        @Override
        public boolean canUse(org.bukkit.command.CommandSender sender) {
            return sender.hasPermission("fieldnotes.note");
        }
    }

    static String noteLine(Player p, String text, long now) {
        Location l = p.getLocation();
        return Line.time(now) + " | " + p.getName() + " | " + where(l) + " | facing " + Line.facing(l.getYaw())
                + " (yaw " + Math.round(l.getYaw()) + ", pitch " + Math.round(l.getPitch()) + ") | " + p.getGameMode().name().toLowerCase()
                + " | " + Line.clean(text, 1000);
    }

    static String where(Location l) {
        return l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ();
    }

    // ---- the rolling record ----

    private void watch(Player p, String why) {
        if (!p.hasPermission("fieldnotes.watched")) return;
        names.put(p.getUniqueId(), p.getName());
        recent.computeIfAbsent(p.getUniqueId(), k -> new Recent(max)).add(Line.time(System.currentTimeMillis()) + " | " + why + " | " + where(p.getLocation()));
    }

    private void record(Player p, String what) {
        Recent r = recent.get(p.getUniqueId());
        if (r != null) r.add(Line.time(System.currentTimeMillis()) + " | " + Line.clean(what, 300) + " | " + where(p.getLocation()));
    }

    /** Writes each record that changed since the last write. Runs off the main thread. */
    private void saveChanged() {
        for (UUID id : recent.keySet()) {
            Recent r = recent.get(id);
            if (r != null && r.takeChanged()) save(id, null);
        }
    }

    /** Rewrites a player's recent-actions file, with an ending line if they've left. */
    private void save(UUID id, String ending) {
        Recent r = recent.get(id);
        String name = names.getOrDefault(id, id.toString());
        if (r == null) return;
        List<String> lines = r.snapshot();
        if (ending != null) lines.add(Line.time(System.currentTimeMillis()) + " | " + ending);
        try {
            Files.replace(dir.resolve("recent").resolve(name + ".log"), lines);
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Couldn't save " + name + "'s recent actions", e);
        }
    }

    private void saveAll(String why) {
        for (UUID id : recent.keySet()) save(id, "saved because " + why);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        watch(e.getPlayer(), "joined");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent e) {
        kickReasons.put(e.getPlayer().getUniqueId(), net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.reason()));
    }

    /**
     * The client crashing shows here as a lost connection or a time-out, so this is where a crashed game is caught.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        String kick = kickReasons.remove(p.getUniqueId());
        String how = switch (e.getReason()) {
            case DISCONNECTED -> "left the game, or the connection was lost";
            case KICKED -> "was kicked" + (kick == null ? "" : ": " + Line.clean(kick, 300));
            case TIMED_OUT -> "timed out: the game stopped answering, which is what a crashed or frozen game looks like";
            case ERRONEOUS_STATE -> "was dropped because the connection went wrong";
        };
        record(p, "left: " + how);
        UUID id = p.getUniqueId();
        save(id, "saved on leaving");
        try {
            List<String> session = new java.util.ArrayList<>();
            session.add("== " + Line.time(System.currentTimeMillis()) + " | " + p.getName() + " " + how);
            session.addAll(recent.get(id) == null ? List.of() : recent.get(id).snapshot());
            Files.append(dir.resolve("sessions.log"), session);
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Couldn't save " + p.getName() + "'s session", ex);
        }
        recent.remove(id);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        record(e.getPlayer(), "command " + Line.commandName(e.getMessage()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        record(e.getPlayer(), "broke " + e.getBlock().getType().name().toLowerCase() + " at " + where(e.getBlock().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        record(e.getPlayer(), "placed " + e.getBlockPlaced().getType().name().toLowerCase() + " at " + where(e.getBlockPlaced().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == org.bukkit.event.block.Action.PHYSICAL) return;
        String item = e.getItem() == null ? "empty hand" : e.getItem().getType().name().toLowerCase();
        String on = e.getClickedBlock() == null ? "" : " on " + e.getClickedBlock().getType().name().toLowerCase();
        record(e.getPlayer(), e.getAction().name().toLowerCase().replace('_', ' ') + " with " + item + on);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        record(e.getPlayer(), "teleported (" + e.getCause().name().toLowerCase() + ") to " + where(e.getTo()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorld(PlayerChangedWorldEvent e) {
        record(e.getPlayer(), "arrived in " + e.getPlayer().getWorld().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) record(p, "hurt by " + e.getCause().name().toLowerCase() + " (" + Math.round(e.getFinalDamage()) + ")");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        record(e.getPlayer(), "died: " + (e.deathMessage() == null ? "" : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.deathMessage())));
        save(e.getPlayer().getUniqueId(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p) record(p, "opened " + e.getInventory().getType().name().toLowerCase());
    }
}
