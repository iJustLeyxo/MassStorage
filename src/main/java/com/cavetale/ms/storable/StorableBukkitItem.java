package com.cavetale.ms.storable;

import com.cavetale.core.font.VanillaItems;
import com.cavetale.core.item.ItemKind;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.BlockColor;
import com.cavetale.mytems.util.Text;
import java.util.List;
import java.util.stream.Stream;
import lombok.Value;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.CreativeCategory;
import org.bukkit.inventory.ItemStack;
import static java.util.stream.Collectors.joining;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Value
public final class StorableBukkitItem implements StorableItem {
    protected final String name;
    protected final Component displayName;
    protected final Material material;
    protected final int index;
    protected final String sqlName;
    protected final String category;
    protected final ItemStack prototype;

    protected StorableBukkitItem(final Material material, final int index) {
        this.material = material;
        this.index = index;
        this.sqlName = material.name().toLowerCase();
        this.category = categoryOf(material);
        prototype = new ItemStack(material);
        if (material.name().endsWith("_banner_pattern")) {
            // Super annoying corner case!
            this.name = Text.toCamelCase(material, " ");
            this.displayName = text(name);
        } else {
            ItemKind kind = ItemKind.of(prototype);
            this.name = kind.name(prototype);
            this.displayName = kind.displayName(prototype);
        }
    }

    @Override
    public String toString() {
        return "name=" + name
            + "\nmaterial=" + material
            + "\nindex=" + index
            + "\nsqlName=" + sqlName
            + "\ncategory=" + category
            + "\nproto=" + prototype.getType().getKey() + (prototype.hasItemMeta()
                                                           ? prototype.getItemMeta().getAsString()
                                                           : "");
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.BUKKIT;
    }

    @Override
    public Component getIcon() {
        Component result = VanillaItems.componentOf(material);
        return !empty().equals(result)
            ? result
            : Mytems.QUESTION_MARK.asComponent();
    }

    @Override
    public boolean canStore(ItemStack itemStack) {
        return prototype.isSimilar(itemStack);
    }

    @Override
    public boolean canStack(ItemStack itemStack) {
        return canStore(itemStack);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public ItemStack createItemStack(int amount) {
        return new ItemStack(material, amount);
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(material);
    }

    @Override
    public int getMaxStackSize() {
        return material.getMaxStackSize();
    }

    @Override
    public boolean isShulkerBox() {
        return Tag.SHULKER_BOXES.isTagged(material);
    }

    private static String categoryOf(Material material) {
        BlockColor blockColor = BlockColor.of(material);
        if (blockColor != null) {
            return Text.toCamelCase(blockColor.suffixOf(material), " ");
        }
        switch (material) {
        case FISHING_ROD: case CARROT_ON_A_STICK: case WARPED_FUNGUS_ON_A_STICK:
            return "Fishing Rod";
        case CHEST: case TRAPPED_CHEST: case ENDER_CHEST: case BARREL:
            return "Chest";
        case PODZOL: case FARMLAND:
            return "Dirt";
        default: break;
        }
        String name = material.name();
        for (String part : List.of("MOSSY_COBBLESTONE",
                                   "COBBLESTONE",
                                   "SCULK",
                                   "FROGLIGHT",
                                   "MANGROVE",
                                   "DIRT",
                                   "ANDESITE",
                                   "DIORITE",
                                   "GRANITE",
                                   "BLACKSTONE",
                                   "SANDSTONE",
                                   "DEEPSLATE",
                                   "COAL",
                                   "DIAMOND",
                                   "EMERALD",
                                   "COPPER",
                                   "IRON",
                                   "CHAINMAIL",
                                   "LEATHER",
                                   "LAPIS",
                                   "GOLD",
                                   "REDSTONE",
                                   "GLAZED_TERRACOTTA",
                                   "TERRACOTTA",
                                   "MELON", // slICE
                                   "PUMPKIN",
                                   "BONE",
                                   "AMETHYST",
                                   "ICE",
                                   "CORAL",
                                   "LEAVES",
                                   "PRISMARINE",
                                   "BASALT",
                                   "NETHER",
                                   "PURPUR",
                                   "MUSHROOM",
                                   "DARK_OAK",
                                   "OAK",
                                   "SPRUCE",
                                   "BIRCH",
                                   "JUNGLE",
                                   "ACACIA",
                                   "CRIMSON",
                                   "WARPED",
                                   "SOUL",
                                   "SAND",
                                   "OBSIDIAN",
                                   "SHULKER",
                                   "QUARTZ",
                                   "DYE",
                                   "BUCKET",
                                   "ENDER",
                                   "END_STONE",
                                   "MOSSY",
                                   "MOSS",
                                   "DRIPSTONE",
                                   "RAIL",
                                   "MINECART",
                                   "BANNER_PATTERN",
                                   "FIREWORK",
                                   "MUD",
                                   "STONE",
                                   "TUFF")) {
            if (name.contains(part)) {
                return Stream.of(part.split("_"))
                    .map(Text::toCamelCase)
                    .collect(joining(" "));
            }
        }
        CreativeCategory creativeCategory = material.getCreativeCategory();
        if (creativeCategory != null) return Text.toCamelCase(creativeCategory, " ");
        return "Unknown";
    }
}
