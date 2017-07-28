package com.winthier.massstorage;

import com.winthier.massstorage.sql.SQLItem;
import com.winthier.massstorage.sql.SQLPlayer;
import com.winthier.massstorage.vault.VaultHandler;
import com.winthier.sql.SQLDatabase;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class MassStoragePlugin extends JavaPlugin {
    @Getter private static MassStoragePlugin instance;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final List<Category> categories = new ArrayList<>();
    private Set<Material> materialBlacklist = null;
    private VaultHandler vaultHandler = null;
    private SQLDatabase db;
    private final MassStorageCommand massStorageCommand = new MassStorageCommand(this);

    @Override
    public void onEnable() {
        instance = this;
        reloadAll();
        getCommand("massstorage").setExecutor(massStorageCommand);
        getCommand("massstorageadmin").setExecutor(new AdminCommand(this));
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        db = new SQLDatabase(this);
        db.registerTables(SQLItem.class, SQLPlayer.class);
        db.createAllTables();
    }

    @Override
    public void onDisable() {
        for (Session session: sessions.values()) session.close();
        sessions.clear();
    }

    Session getSession(Player player) {
        final UUID uuid = player.getUniqueId();
        Session result = sessions.get(uuid);
        if (result == null) {
            result = new Session(uuid);
            sessions.put(uuid, result);
        }
        return result;
    }

    Set<Material> getMaterialBlacklist() {
        if (materialBlacklist == null) {
            materialBlacklist = EnumSet.noneOf(Material.class);
            for (String str: getConfig().getStringList("MaterialBlacklist")) {
                try {
                    Material mat = Material.valueOf(str.toUpperCase());
                    materialBlacklist.add(mat);
                } catch (IllegalArgumentException iae) {
                    getLogger().warning("Unknown material: " + str);
                }
            }
        }
        return materialBlacklist;
    }

    VaultHandler getVaultHandler() {
        if (vaultHandler == null) {
            if (null != getServer().getPluginManager().getPlugin("Vault")) {
                vaultHandler = new VaultHandler();
            } else {
                getLogger().warning("Could not find Vault!");
            }
        }
        return vaultHandler;
    }

    void reloadAll() {
        // saveDefaultConfig(); TODO TODO FIXME !!!!!
        reloadConfig();
        materialBlacklist = null;
        if (sessions != null) {
            for (Session session: sessions.values()) {
                session.flush();
            }
        }
        categories.clear();
        Set<Material> miscMaterials = EnumSet.allOf(Material.class);
        ConfigurationSection menuConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("menu.yml")));
        for (Map<?, ?> map: menuConfig.getMapList("Categories")) {
            ConfigurationSection section = menuConfig.createSection("tmp", map);
            try {
                boolean misc = section.getBoolean("Misc");
                Set<Material> materials;
                if (misc) {
                    materials = miscMaterials;
                } else {
                    materials = EnumSet.copyOf(section.getStringList("Materials").stream().map(s -> Material.valueOf(s)).collect(Collectors.toList()));
                    miscMaterials.removeAll(materials);
                }
                Set<Item> items = new HashSet<>();
                if (section.isSet("Items")) {
                    for (Object o: section.getList("Items")) {
                        Material mat;
                        int data;
                        if (o instanceof List) {
                            List ls = (List)o;
                            mat = Material.valueOf((String)ls.get(0));
                            data = ((Number)ls.get(1)).intValue();
                        } else {
                            mat = Material.valueOf((String)o);
                            data = 0;
                        }
                        items.add(new Item(mat.getId(), data));
                    }
                }
                String name = section.getString("Name");
                ItemStack icon;
                if (section.isList("Icon")) {
                    List<?> il = section.getList("Icon");
                    icon = new ItemStack(Material.valueOf((String)il.get(0)),
                                         1, ((Number)il.get(1)).shortValue());
                } else {
                    icon = new ItemStack(Material.valueOf(section.getString("Icon")));
                }
                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName(ChatColor.RESET + name);
                icon.setItemMeta(meta);
                Category category = new Category(name, icon, misc, materials, items);
                categories.add(category);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    boolean permitNonStackingItems() {
        return getConfig().getBoolean("PermitNonStackingItems", true);
    }
}

@RequiredArgsConstructor
class Category {
    final String name;
    final ItemStack icon;
    final boolean misc;
    final Set<Material> materials;
    final Set<Item> items;
}
