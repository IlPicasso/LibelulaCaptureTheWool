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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.kitteh.tag.TagAPI;

/**
 *
 * @author Diego D'Onofrio
 * @version 1.0
 * 
 */

public class PlayerManager {

    private class PlayerOptions {

        boolean viewOthersSpectators;
        boolean viewOthersDeathMessages;
        boolean viewBlood;
    }

    private final Main plugin;
    private final File playersFile;
    private final YamlConfiguration playersConfig;
    private final TreeMap<String, PlayerOptions> playerOptions;
    private final TreeMap<String, TeamManager.TeamId> playerTeam;
    private final ReentrantLock _playerTeam_mutex;
    private final ItemStack helpBook;
    private final ItemStack joinMenuItem;

    public PlayerManager(Main plugin) {
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        playersConfig = new YamlConfiguration();
        this.plugin = plugin;
        playerOptions = new TreeMap<>();
        playerTeam = new TreeMap<>();
        _playerTeam_mutex = new ReentrantLock(true);
        helpBook = plugin.lm.getHelpBook();

        ItemStack menuItem = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta im = menuItem.getItemMeta();
        im.setDisplayName(plugin.lm.getText("help-menu-item.title"));
        menuItem.setItemMeta(im);
        joinMenuItem = menuItem;
    }

    public ItemStack getMenuItem() {
        return joinMenuItem;
    }

    public void load() {
        try {
            playersConfig.load(playersFile);
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.getLogger().severe(ex.toString());
        }
        for (String playerName : playersConfig.getKeys(false)) {
            PlayerOptions po = new PlayerOptions();
            po.viewOthersSpectators = playersConfig.getBoolean(playerName + "." + "view.others-spectators");
            po.viewOthersDeathMessages = playersConfig.getBoolean(playerName + "." + "view.others-deadMessages");
            po.viewBlood = playersConfig.getBoolean(playerName + "." + "view.blood");
            playerOptions.put(playerName, po);
        }
    }

    public boolean toggleSeeOthersSpectators(Player player) {
        PlayerOptions po = playerOptions.get(player.getName());
        if (po != null) {
            po.viewOthersSpectators = !po.viewOthersSpectators;
        } else {
            po = new PlayerOptions();
            po.viewOthersSpectators = !canSeeOthersSpectators(player);
        }
        playerOptions.put(player.getName(), po);
        updateCanSee(player);
        plugin.getLogger().info("Debug:" + player.getName() + "ViewOthes: " + canSeeOthersSpectators(player));
        return po.viewOthersSpectators;
    }

    public boolean toogleOthersDeathMessages(Player player) {
        PlayerOptions po = playerOptions.get(player.getName());
        if (po != null) {
            po.viewOthersDeathMessages = !po.viewOthersDeathMessages;
        } else {
            po = new PlayerOptions();
            po.viewOthersDeathMessages = !canSeeOthersDeathMessages(player);
        }
        playerOptions.put(player.getName(), po);
        return po.viewOthersDeathMessages;
    }

    public boolean canSeeOthersSpectators(Player player) {
        PlayerOptions po = playerOptions.get(player.getName());
        return po == null || po.viewOthersSpectators;
    }

    public boolean canSeeOthersDeathMessages(Player player) {
        PlayerOptions po = playerOptions.get(player.getName());
        return po == null || po.viewOthersDeathMessages;
    }

    public ChatColor getChatColor(Player player) {
        ChatColor cc = ChatColor.WHITE;
        TeamManager.TeamId teamId = playerTeam.get(player.getName());
        if (teamId != null) {
            cc = plugin.tm.getChatColor(teamId);
        }
        return cc;
    }

    public void addPlayerTo(Player player, TeamManager.TeamId teamId) {
        _playerTeam_mutex.lock();
        try {
            TeamManager.TeamId previousTeam = playerTeam.put(player.getName(), teamId);
            if (previousTeam != null) {
                plugin.tm.removeFromTeam(player, previousTeam);
            }
            plugin.tm.addToTeam(player, teamId);
            clearInventory(player);
            if (teamId != TeamManager.TeamId.SPECTATOR) {
                disguise(player, teamId);
            } else {
                setSpectator(player);
            }
            updateCanSee(player);
            player.sendMessage(plugin.lm.getMessage("moved-to-" + teamId.name().toLowerCase()));
        } finally {
            _playerTeam_mutex.unlock();
        }
    }

    public TeamManager.TeamId clearTeam(Player player) {
        TeamManager.TeamId teamId;
        _playerTeam_mutex.lock();
        try {
            teamId = playerTeam.remove(player.getName());
            plugin.tm.removeFromTeam(player, teamId);
            clearInventory(player);
            dress(player);

        } finally {
            _playerTeam_mutex.unlock();
        }
        return teamId;
    }

    public TeamManager.TeamId getTeamId(Player player) {
        return playerTeam.get(player.getName());
    }

    public boolean isSpectator(Player player) {
        TeamManager.TeamId teamId = playerTeam.get(player.getName());
        return teamId != null && teamId == TeamManager.TeamId.SPECTATOR;
    }

    public void clearInventory(Player player) {
        ItemStack air = new ItemStack(Material.AIR);
        player.getInventory().clear();
        player.getInventory().setBoots(air);
        player.getInventory().setChestplate(air);
        player.getInventory().setHelmet(air);
        player.getInventory().setLeggings(air);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    public void dress(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        clearInventory(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public void updateCanSee(Player player) {
        boolean playerCanSeeOtherHidden = canSeeOthersSpectators(player);
        boolean playerIsSpect = isSpectator(player);

        for (Player other : player.getWorld().getPlayers()) {
            boolean otherIsSpectator = isSpectator(other);
            boolean canSeeOthersSpectators = canSeeOthersSpectators(other);
            
            if (playerIsSpect) {
                if (!otherIsSpectator) {
                    other.hidePlayer(player);
                } else {
                    if (canSeeOthersSpectators) {
                        other.showPlayer(player);
                    } else {
                        other.hidePlayer(player);
                    }
                }
            } else {
                other.showPlayer(player);
            }
            
            if (!otherIsSpectator) {
                player.showPlayer(other);
            } else {
                if (!playerIsSpect) {
                    player.hidePlayer(other);
                } else {
                    if (playerCanSeeOtherHidden) {
                        player.showPlayer(other);
                    } else {
                        player.hidePlayer(other);
                    }
                }
            }
            
        }
    }

    public void disguise(Player player, TeamManager.TeamId teamId) {

        LeatherArmorMeta leatherMeta;
        List<String> armourBrand = new ArrayList<>();
        armourBrand.add(plugin.tm.armourBrandName);

        Color tshirtColor = plugin.tm.getTshirtColor(teamId);
        ChatColor teamChatColor = plugin.tm.getChatColor(teamId);
        String teamName = plugin.tm.getName(teamId);

        clearInventory(player);

        ItemStack tshirt = new ItemStack(Material.LEATHER_CHESTPLATE);
        leatherMeta = (LeatherArmorMeta) tshirt.getItemMeta();
        leatherMeta.setColor(tshirtColor);
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        tshirt.setItemMeta(leatherMeta);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        leatherMeta = (LeatherArmorMeta) boots.getItemMeta();
        leatherMeta.setColor(tshirtColor);
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        boots.setItemMeta(leatherMeta);

        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        leatherMeta = (LeatherArmorMeta) leggings.getItemMeta();
        leatherMeta.setColor(tshirtColor);
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        leggings.setItemMeta(leatherMeta);

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        leatherMeta = (LeatherArmorMeta) helmet.getItemMeta();
        leatherMeta.setColor(tshirtColor);
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        helmet.setItemMeta(leatherMeta);

        player.setDisplayName(teamChatColor + player.getName());
        player.getInventory().setBoots(boots);
        player.getInventory().setChestplate(tshirt);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setHelmet(helmet);

        player.setGameMode(GameMode.SURVIVAL);

        updateCanSee(player);

        try {
            TagAPI.refreshPlayer(player);
        } catch (Exception ex) {
            plugin.getLogger().warning(ex.toString());
        }
    }

    private void setSpectator(Player player) {
        player.setHealth(20);
        player.setFoodLevel(20);
        plugin.pm.clearInventory(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.getInventory().addItem(helpBook);
        player.getInventory().addItem(joinMenuItem);
        updateCanSee(player);
    }
}
