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

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Diego D'Onofrio
 * @version 1.0
 *
 */
public class Main extends JavaPlugin {

    WorldManager wm;
    CommandManager cm;
    EventManager em;
    TeamManager tm;
    LangManager lm;
    MapManager mm;
    WorldEditPlugin we;
    RoomManager rm;
    SignManager sm;
    GameManager gm;
    ConfigManager cf;
    PlayerManager pm;

    @Override
    public void onEnable() {

        lm = new LangManager(this);
        we = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        if (we == null) {
            alert(lm.getText("we-not-enabled"));
            return;
        }

        try {
            Class.forName("org.kitteh.tag.TagAPI");
        } catch (ClassNotFoundException ex) {
            alert(lm.getText("ta-not-enabled"));
            return;
        }

        cf = new ConfigManager(this);
        wm = new WorldManager(this);
        tm = new TeamManager(this);
        pm = new PlayerManager(this);

        removeAllItems();

        cm = new CommandManager(this);
        em = new EventManager(this);
        mm = new MapManager(this);
        rm = new RoomManager(this);
        gm = new GameManager(this);
        rm.init();
        sm = new SignManager(this);

        saveDefaultConfig();
        
        alert("Testing version for development, use it under your own risk.");
    }

    public void reload() {
        cf.load();
        wm.load();
        mm.load();
        rm.load();
        sm.load();
    }

    public void save() {
        if (wm != null) {
            wm.persist();
        }

        if (mm != null) {
            mm.persist();
        }

        if (rm != null) {
            rm.persist();
        }

        if (sm != null) {
            sm.persists();
        }

        if (cf != null) {
            cf.persists();
        }
    }

    @Override
    public void onDisable() {
        save();
        moveAllToLobby();
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return wm.getEmptyWorldGenerator();
    }

    public boolean hasPermission(Player player, String permission) {
        return player.hasPermission("ctw." + permission);
    }

    public ConsoleCommandSender getConsole() {
        return getServer().getConsoleSender();
    }

    public void alert(String message) {
        String prefix = ChatColor.YELLOW + "["
                + ChatColor.GOLD + ChatColor.BOLD + this.getName()
                + ChatColor.YELLOW + "]";
        String prefixedMessage = prefix + " " + ChatColor.RED + "(alert) " + message;
        getServer().getConsoleSender().sendMessage(prefixedMessage);
        for (Player player : getServer().getOnlinePlayers()) {
            if (hasPermission(player, "receive-alerts")) {
                player.sendMessage(prefixedMessage);
            }
        }
    }

    public void moveAllToLobby() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (rm.isInGame(player.getWorld())) {
                pm.dress(player);
                player.teleport(wm.getNextLobbySpawn());
            }
        }
    }

    public void removeAllItems() {
        for (World world : getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getType() != EntityType.PLAYER
                        && entity.getType() != EntityType.ITEM_FRAME
                        && entity.getType() != EntityType.UNKNOWN) {
                    entity.remove();
                }
            }
        }
    }
}
