package eu.pb4.buildbattle.custom;

import eu.pb4.buildbattle.other.BbUtils;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;

import static eu.pb4.buildbattle.other.BbUtils.id;

public class BBItems {
    public static VotingItem WT = new VotingItem(0, Formatting.DARK_RED, Items.NETHER_BRICK, new Item.Settings());
    public static VotingItem BAD = new VotingItem(1, Formatting.RED, Items.NETHER_WART, new Item.Settings());
    public static VotingItem NB = new VotingItem(2, Formatting.GOLD, Items.COAL, new Item.Settings());
    public static VotingItem OKAY = new VotingItem(3, Formatting.YELLOW, Items.IRON_INGOT, new Item.Settings());
    public static VotingItem GOOD = new VotingItem(4, Formatting.BLUE, Items.GOLD_INGOT, new Item.Settings());
    public static VotingItem GREAT = new VotingItem(5, Formatting.DARK_GREEN, Items.EMERALD, new Item.Settings());
    public static VotingItem WOW = new VotingItem(6, Formatting.GREEN, Items.DIAMOND, new Item.Settings());
    public static WrappedItem WRAPPED = new WrappedItem(new Item.Settings());
    protected static Map<String, ItemStack> WRAPPED_ITEMS = new HashMap<>();

    public static void register() {
        Registry.register(Registry.ITEM, id("wt"), WT);
        Registry.register(Registry.ITEM, id("bad"), BAD);
        Registry.register(Registry.ITEM, id("nb"), NB);
        Registry.register(Registry.ITEM, id("okay"), OKAY);
        Registry.register(Registry.ITEM, id("good"), GOOD);
        Registry.register(Registry.ITEM, id("great"), GREAT);
        Registry.register(Registry.ITEM, id("wow"), WOW);

        Registry.register(Registry.ITEM, id("wrapped_item"), WRAPPED);

        registerWrappedItem("tiny_potato", new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(BbUtils.getText("item", "tiny_potato"))
                .setSkullOwner("ewogICJ0aW1lc3RhbXAiIDogMTYwNjIyODAxMzY0NCwKICAicHJvZmlsZUlkIiA6ICJiMGQ0YjI4YmMxZDc0ODg5YWYwZTg2NjFjZWU5NmFhYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaW5lU2tpbl9vcmciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTczNTE0YTIzMjQ1ZjE1ZGJhZDVmYjRlNjIyMTYzMDIwODY0Y2NlNGMxNWQ1NmRlM2FkYjkwZmE1YTcxMzdmZCIKICAgIH0KICB9Cn0=", null, null)
                .asStack());
    }


    public static void registerWrappedItem(String id, ItemStack stack) {
        WRAPPED_ITEMS.put(id, stack);
    }
}
