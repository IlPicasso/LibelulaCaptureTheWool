/*
 *            This file is part of Libelula Capture The Wool plugin.
 *
 *  Libelula Capture The Wool is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Libelula Capture The Wool is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Libelula Capture The Wool. 
 *  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package me.libelula.capturethewool;

import java.util.TreeMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;
import org.kitteh.tag.AsyncPlayerReceiveNameTagEvent;

/**
 *
 * @author Diego D'Onofrio
 * @version 1.0
 *
 */
public final class EventManager {

    private final Main plugin;
    private final GameListeners gameEvents;
    private final TreeMap<Player, SetupListeners> playerSetup;

    /**
     *
     */
    public enum SetUpAction {

        RED_WIN_WOOL, BLUE_WIN_WOOL, WOOL_SPAWNER
    }

    private class SetupListeners implements Listener {

        private final SetUpAction action;

        public SetupListeners(SetUpAction action) {
            this.action = action;
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockBreakEvent(BlockBreakEvent e) {
            if (!playerSetup.containsKey(e.getPlayer())) {
                return;
            }
            Location currLoc;
            if (e.getBlock().getType() != Material.WOOL) {
                if (e.getBlock().getType() == Material.MOB_SPAWNER) {
                    Wool wool = new Wool(e.getBlock().getType(), e.getBlock().getData());
                    currLoc = plugin.mm.getWoolSpawnerLocation(e.getBlock().getWorld(), wool.getColor());
                    if (currLoc != null) {
                        plugin.mm.delWoolSpawner(e.getBlock());
                        plugin.lm.sendMessage("spawner-deleted", e.getPlayer());
                        return;
                    }
                }
                plugin.lm.sendMessage("not-a-wool", e.getPlayer());
                e.setCancelled(true);
                return;
            }

            Wool wool = new Wool(e.getBlock().getType(), e.getBlock().getData());
            currLoc = plugin.mm.getBlueWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
            if (currLoc != null) {
                plugin.lm.sendMessage("cappoint-deleted", e.getPlayer());
                plugin.mm.delBlueWoolWinPoint(e.getBlock());
                return;
            }

            currLoc = plugin.mm.getRedWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
            if (currLoc != null) {
                plugin.lm.sendMessage("cappoint-deleted", e.getPlayer());
                plugin.mm.delRedWoolWinPoint(e.getBlock());
                return;
            }

        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockPlaceEvent(BlockPlaceEvent e) {
            if (!playerSetup.containsKey(e.getPlayer())) {
                return;
            }
            if (e.getBlock().getType() != Material.WOOL) {
                plugin.lm.sendMessage("not-a-wool", e.getPlayer());
                e.setCancelled(true);
                return;
            }
            Wool wool = new Wool(e.getBlock().getType(), e.getBlock().getData());
            Location currLoc;
            if (action == SetUpAction.BLUE_WIN_WOOL || action == SetUpAction.RED_WIN_WOOL) {
                currLoc = plugin.mm.getBlueWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
                if (currLoc != null) {
                    e.getPlayer().sendMessage(plugin.lm.getText("woolwin-already-blueteam")
                            .replace("%X%", currLoc.getBlockX() + "").replace("%Y%", currLoc.getBlockY() + "")
                            .replace("%Z%", currLoc.getBlockZ() + ""));
                    e.setCancelled(true);
                    return;
                }
                currLoc = plugin.mm.getRedWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
                if (currLoc != null) {
                    e.getPlayer().sendMessage(plugin.lm.getText("woolwin-already-redteam")
                            .replace("%X%", currLoc.getBlockX() + "").replace("%Y%", currLoc.getBlockY() + "")
                            .replace("%Z%", currLoc.getBlockZ() + ""));
                    e.setCancelled(true);
                    return;
                }
            } else {
                currLoc = plugin.mm.getWoolSpawnerLocation(e.getBlock().getWorld(), wool.getColor());
                if (currLoc != null) {
                    e.getPlayer().sendMessage(plugin.lm.getText("spawner-already-exists")
                            .replace("%X%", currLoc.getBlockX() + "").replace("%Y%", currLoc.getBlockY() + "")
                            .replace("%Z%", currLoc.getBlockZ() + ""));
                    e.setCancelled(true);
                    return;
                }
            }

            switch (action) {
                case BLUE_WIN_WOOL:
                    if (plugin.mm.addBlueWoolWinPoint(e.getBlock())) {
                        plugin.lm.sendMessage("blue-wool-winpoint-placed", e.getPlayer());
                    }
                    break;
                case RED_WIN_WOOL:
                    if (plugin.mm.addRedWoolWinPoint(e.getBlock())) {
                        plugin.lm.sendMessage("red-wool-winpoint-placed", e.getPlayer());
                    }
                    break;
                case WOOL_SPAWNER:
                    if (plugin.mm.addwoolSpawner(e.getBlock())) {
                        e.getPlayer().sendMessage(plugin.lm.getText("spawner-placed")
                                .replace("%WOOL%", wool.getColor().toString()));
                    }
                    break;
            }
        }

    }

    private class GameListeners implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onWeatherChange(WeatherChangeEvent e) {
            plugin.gm.ajustWeather(e);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerMove(PlayerMoveEvent e) {
            plugin.gm.denyEnterToProhibitedZone(e);
            if (!e.isCancelled()) {
                plugin.mm.announceAreaBoundering(e);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onRespawn(PlayerRespawnEvent e) {
            String roomName = plugin.rm.getRoom(e.getPlayer().getWorld());
            if (roomName != null) {
                switch (plugin.pm.getTeamId(e.getPlayer())) {
                    case RED:
                        e.setRespawnLocation(plugin.gm.getRedSpawn(roomName));
                        plugin.pm.disguise(e.getPlayer(), TeamManager.TeamId.RED);
                        break;
                    case BLUE:
                        e.setRespawnLocation(plugin.gm.getBlueSpawn(roomName));
                        plugin.pm.disguise(e.getPlayer(), TeamManager.TeamId.BLUE);
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onDeath(PlayerDeathEvent e) {
            e.setDeathMessage("");
            plugin.tm.manageDeath(e);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onSignChange(SignChangeEvent e) {
            plugin.sm.checkForGameInPost(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
        public void onNameTag(AsyncPlayerReceiveNameTagEvent e) {
            Player player = e.getNamedPlayer();
            e.setTag(plugin.pm.getChatColor(player) + player.getName());
        }

        @EventHandler(ignoreCancelled = true)
        public void onItemSpawnEvent(ItemSpawnEvent e) {
            plugin.tm.onArmourDrop(e);
            if (!e.isCancelled()) {
                if (plugin.rm.isInGame(e.getEntity().getWorld())) {
                    if (plugin.rm.isProhibited(e.getEntity())) {
                        e.setCancelled(true);
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerChat(AsyncPlayerChatEvent e) {
            plugin.tm.playerChat(e);
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
        public void onPlayerInteract(PlayerInteractEvent e) {

            plugin.tm.cancelSpectator(e);

            if (e.isCancelled()) {
                return;
            }

            if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                    || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                if (e.getClickedBlock().getType() == Material.WALL_SIGN
                        || e.getClickedBlock().getType() == Material.SIGN_POST) {
                    plugin.sm.checkForPlayerJoin(e);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerDrop(PlayerDropItemEvent e
        ) {
            plugin.tm.cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
        public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
            if (e.isCancelled() && e.getEntity() instanceof Player) {
                TeamManager.TeamId teamId = plugin.pm.getTeamId((Player) e.getEntity());
                if (teamId != null && teamId != TeamManager.TeamId.SPECTATOR) {
                    e.setCancelled(false);
                }
            }

            if (!e.isCancelled()) {
                plugin.tm.cancelSpectator(e);
            }
            if (!e.isCancelled()) {
                plugin.tm.cancelSameTeam(e);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerInteractEntity(PlayerInteractEntityEvent e
        ) {
            plugin.tm.cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onInventoryClick(InventoryClickEvent e) {
            plugin.tm.cancelSpectator(e);
            if (!e.isCancelled()) {
                plugin.gm.checkTarget(e);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerTeleport(PlayerTeleportEvent e) {
            if (!e.getFrom().getWorld().getName().equals(e.getTo().getWorld().getName())) {
                if (plugin.rm.isInGame(e.getTo().getWorld())) { // Getting in a game
                    final Player player = e.getPlayer();
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.gm.movePlayerTo(player, TeamManager.TeamId.SPECTATOR);
                        }
                    }, 5);
                } else {
                    if (plugin.rm.isInGame(e.getFrom().getWorld())) {
                        plugin.gm.playerLeftGame(e.getPlayer());
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerQuit(PlayerQuitEvent e) {
            if (plugin.rm.isInGame(e.getPlayer().getWorld())) {
                e.getPlayer().teleport(plugin.wm.getNextLobbySpawn());
            }
            e.setQuitMessage("");
            e.getPlayer().teleport(plugin.wm.getNextLobbySpawn());
            String joinMessage = plugin.lm.getText("left-message")
                    .replace("%PLAYER%", e.getPlayer().getDisplayName());
            for (Player player : plugin.wm.getLobbyWorld().getPlayers()) {
                plugin.lm.sendMessage(joinMessage, player);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onJoinEvent(PlayerJoinEvent e) {
            World lobbyWorld = plugin.wm.getLobbyWorld();
            if (lobbyWorld == null) {
                if (plugin.hasPermission(e.getPlayer(), "setup")) {
                    final Player player = e.getPlayer();
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.lm.sendText("unconfigured-lobby", player);
                        }
                    }, 30);
                }
            } else {
                e.setJoinMessage("");
                e.getPlayer().teleport(plugin.wm.getNextLobbySpawn());
                String joinMessage = plugin.lm.getText("join-message")
                        .replace("%PLAYER%", e.getPlayer().getDisplayName());
                for (Player player : plugin.wm.getLobbyWorld().getPlayers()) {
                    plugin.lm.sendMessage(joinMessage, player);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockPlaceEvent(BlockPlaceEvent e) {
            plugin.tm.cancelSpectator(e);
            if (!e.isCancelled()) {
                plugin.gm.events.cancelEditProtectedAreas(e);
            }
            if (e.isCancelled()) {
                plugin.gm.checkTarget(e);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockBreakEvent(BlockBreakEvent e) {
            plugin.tm.cancelSpectator(e);
            plugin.gm.events.cancelEditProtectedAreas(e);
            /*
             if (!e.isCancelled()) {
             plugin.wm.addModificationPoint(e.getBlock().getLocation());
             }
             */
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerPickupItem(PlayerPickupItemEvent e) {
            plugin.tm.cancelSpectator(e);
            if (!e.isCancelled()) {
                plugin.gm.checkTarget(e);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onEntityTarget(EntityTargetEvent e
        ) {
            plugin.tm.cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockDamage(BlockDamageEvent e
        ) {
            plugin.tm.cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onEntityDamage(EntityDamageEvent e
        ) {
            plugin.tm.cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onFoodLevelChange(FoodLevelChangeEvent e) {
            plugin.tm.cancelSpectator(e);
            if (!e.isCancelled() && e.getEntity() instanceof Player) {
                Player player = (Player) e.getEntity();
            }
        }

    }

    public EventManager(Main plugin) {
        this.plugin = plugin;
        gameEvents = new GameListeners();
        playerSetup = new TreeMap<>(new Tools.PlayerComparator());
        registerGameEvents();
    }

    public void registerGameEvents() {
        plugin.getServer().getPluginManager().registerEvents(gameEvents, plugin);
    }

    public void registerSetupEvents(Player player, SetUpAction action) {
        unregisterSetUpEvents(player);
        SetupListeners sl = new SetupListeners(action);
        plugin.getServer().getPluginManager().registerEvents(sl, plugin);
        playerSetup.put(player, sl);
    }

    public void unregisterGameEvents() {
        HandlerList.unregisterAll(gameEvents);
    }

    public void unregisterSetUpEvents(Player player) {
        SetupListeners sl = playerSetup.remove(player);
        if (sl != null) {
            HandlerList.unregisterAll(sl);
        }
    }
}
