package org.cubeville.cvranks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CVRanks extends JavaPlugin implements Listener
{
    private int uptime;
    private Map<UUID, Integer> lastHeals;
    private Map<UUID, Location> deathLocation;
    private Set<UUID> stonemasonActive;
    private Set<UUID> mossgardenerActive;
    private Set<UUID> bricklayerActive;
    private Set<UUID> carpenterActive;

    
    public void onEnable() {
        stonemasonActive = new HashSet<>();
        mossgardenerActive = new HashSet<>();
        bricklayerActive = new HashSet<>();
        carpenterActive = new HashSet<>();

        deathLocation = new HashMap<>();
        
        lastHeals = new HashMap<>();
        uptime = 0;
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                public void run() {
                    uptime += 10;
                    for(UUID player: lastHeals.keySet()) {
                        Player p = getServer().getPlayer(player);
                        int t = uptime - lastHeals.get(player);
                        boolean recov = false;
                        if(t > 1200) {
                            recov = true;
                        }
                        else if(t > 600) {
                            if(p != null && p.hasPermission("cvranks.service.svc")) {
                                recov = true;
                            }
                        }
                        if(recov) {
                            if(p != null) {
                                p.sendMessage("Your doctor ability is ready to use.");
                            }
                            lastHeals.remove(player);
                        }
                    }
                }
            }, 200, 200);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player senderPlayer = null;
        UUID senderId = null;
        if(sender instanceof Player) {
            senderPlayer = (Player) sender;
            senderId = senderPlayer.getUniqueId();
        }

        if (command.getName().equals("mason") || command.getName().equals("mg") || command.getName().equals("brick") || command.getName().equals("carp")) {

            Set<UUID> typeSet;
            String typeName;
            String typeCommand = command.getName();
            if(typeCommand.equals("mason")) {
                typeSet = stonemasonActive;
                typeName = "stonemason";
            }
            else if(typeCommand.equals("mg")) {
                typeSet = mossgardenerActive;
                typeName = "moss gardener";
            }
            else if(typeCommand.equals("brick")) {
                typeSet = bricklayerActive;
                typeName = "bricklayer";
            }
            else { //if(typeCommand.equals("carp")) {
                typeSet = carpenterActive;
                typeName = "master carpenter";
            }
            
            if(args.length > 1) {
                sender.sendMessage("Too many arguments.");
                sender.sendMessage("/" + typeCommand + " [on|off]");
            }
            else if(args.length == 0) {
                if(typeSet.contains(senderId)) {
                    sender.sendMessage("§aYour " + typeName + " ability is currently enabled.");
                    sender.sendMessage("§aYou can toggle it with /" + typeCommand + " off");
                }
                else {
                    sender.sendMessage("§cYour " + typeName + " ability is currently disabled.");
                    sender.sendMessage("§cYou may toggle it with /" + typeCommand + " on");
                }
            }
            else {
                if(args[0].equals("on")) {
                    if(typeSet.contains(senderId)) {
                        sender.sendMessage("§cYour " + typeName + " ability is already enabled.");
                    }
                    else {
                        typeSet.add(senderId);
                        sender.sendMessage("§aYour " + typeName + " ability has been enabled.");
                    }
                }
                else if(args[0].equals("off")) {
                    if(typeSet.contains(senderId)) {
                        typeSet.remove(senderId);
                        sender.sendMessage("§aYour " + typeName + " ability has been disabled.");
                    }
                    else {
                        sender.sendMessage("§cYour " + typeName + " ability is already disabled.");
                    }
                }
                else {
                    sender.sendMessage("§c/" + typeCommand + " [on|off]");
                }
            }
            return true;
        }
        
        else if (command.getName().equals("doc")) {
            if(args.length == 0) {
                sender.sendMessage("Doctor Command List"); // TODO: dark green
                sender.sendMessage("----------------------"); // ^
                sender.sendMessage("/doc list - Lists online doctors and their recharge timers"); // TODO: light green
                sender.sendMessage("/doc me - Heals yourself");
                sender.sendMessage("/doc <player> - Heals another player");
            }
            else if(args.length > 1) {
                sender.sendMessage("Too many arguments.");
                sender.sendMessage("/doctor <list|me|player>");
            }
            else {
                if(args[0].equals("list")) {
                    // TODO
                }
                else if(args[0].equals("me")) {
                    if(senderPlayer != null) {
                        docPlayer(sender, senderPlayer);
                    }
                }
                else {
                    String playerName = args[0];
                    Player player = getServer().getPlayer(playerName);
                    if(player == null) {
                        sender.sendMessage("No players by that name found.");
                    }
                    else {
                        docPlayer(sender, player);
                    }
                }
            }
            return true;
        }

        else if (command.getName().equals("respawn")) {
            if(senderPlayer != null && deathLocation.containsKey(senderId)) {
                senderPlayer.teleport(deathLocation.get(senderId));
                deathLocation.remove(senderId);
                sender.sendMessage("§aYou have been returned to the point of your last death!");
            }
            else {
                sender.sendMessage("§cYou don't have a death point to return to.");
            }
        }
        
        return false;
    }
    
    public void docPlayer(CommandSender sender, Player player) {
        boolean used = false;

        String senderName;
        // TODO: maybe "yourself" or something like that if same player?
        if(sender instanceof Player) {
            Player p = (Player) sender;
            if(lastHeals.containsKey(p.getUniqueId())) {
                sender.sendMessage("Your doctor ability is not ready to use yet.");
                return;
            }
            if(p.getUniqueId().equals(player.getUniqueId())) {
                senderName = "yourself";
            }
            else {
                senderName = p.getDisplayName();
            }
        }
        else {
            senderName = "Console";
        }
        
        if (player.getHealth() < player.getMaxHealth()) {
            used = true;
            player.setHealth(player.getMaxHealth());
            player.sendMessage("You have been healed by " + senderName + ".");
        }

        if (player.getFoodLevel() < 20) {
            used = true;
            player.setFoodLevel(20);
            player.setExhaustion(1.0F);
            player.setSaturation(5.0F);
            player.sendMessage("Your hunger has been refilled by " + senderName + ".");
        }

        if(sender.hasPermission("cvranks.service.svc")) {
            if (player.getFireTicks() > 0) {
                used = true;
                player.setFireTicks(0);
                player.sendMessage("You have been extinguished by " + senderName + ".");
            }
            if (player.getRemainingAir() < player.getMaximumAir()) {
                used = true;
                player.setRemainingAir(player.getMaximumAir());
                player.sendMessage("Your air has been refilled by " + senderName + ".");
            }
        }

        if(used) {
            sender.sendMessage(player.getDisplayName() + " has been healed.");
            if(sender instanceof Player) {
                Player p = (Player) sender;
                lastHeals.put(p.getUniqueId(), uptime);
            }
        }
        else {
            sender.sendMessage(player.getDisplayName() + " does not need to be healed!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.isCancelled()) return;

        Material material = event.getBlockPlaced().getType();
        UUID playerId = event.getPlayer().getUniqueId();
        
        if(material == Material.COBBLESTONE) {
            if(stonemasonActive.contains(playerId))
                event.getBlockPlaced().setType(Material.STONE);
        }
        else if(material == Material.CLAY) {
            if(bricklayerActive.contains(playerId))
                event.getBlockPlaced().setType(Material.CLAY_BRICK);
        }
        else if(material == Material.SAND) {
            if(carpenterActive.contains(playerId))
                event.getBlockPlaced().setType(Material.GLASS);
        }
        else if(material == Material.SOUL_SAND) {
            if(carpenterActive.contains(playerId))
                event.getBlockPlaced().setType(Material.OBSIDIAN);
        }
    }

    // Ya know what? I'll just wait and see if anyone complains about this useless thing not working...
    //@EventHandler(priority = EventPriority.HIGHEST)
    //public void onPlayerInteract(PlayerInteractEvent event) {
    //  if(event.isCancelled()) return;
        
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if(player.hasPermission("cvranks.death.ks")) {
            event.setKeepInventory(true);
        }
        if(player.hasPermission("cvranks.death.te")) {
            // Death tax?
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
        if(player.hasPermission("cvranks.death.dm")) {
            deathLocation.put(player.getUniqueId(), player.getLocation());
        }
    }
        

}