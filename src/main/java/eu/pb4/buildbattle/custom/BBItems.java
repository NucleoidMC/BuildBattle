package eu.pb4.buildbattle.custom;

import eu.pb4.buildbattle.custom.items.SimpleGlowingItem;
import eu.pb4.buildbattle.custom.items.VotingItem;
import eu.pb4.buildbattle.custom.items.WrappedItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;

import static eu.pb4.buildbattle.other.BbUtils.id;

public class BBItems {
    public static VotingItem VOTE_TERRIBLE = new VotingItem(0, Formatting.DARK_RED, Items.NETHER_BRICK, new Item.Settings());
    public static VotingItem VOTE_BAD = new VotingItem(1, Formatting.RED, Items.NETHER_WART, new Item.Settings());
    public static VotingItem VOTE_NOT_BAD = new VotingItem(2, Formatting.GOLD, Items.COAL, new Item.Settings());
    public static VotingItem VOTE_OKAY = new VotingItem(3, Formatting.YELLOW, Items.IRON_INGOT, new Item.Settings());
    public static VotingItem VOTE_GOOD = new VotingItem(4, Formatting.BLUE, Items.GOLD_INGOT, new Item.Settings());
    public static VotingItem VOTE_GREAT = new VotingItem(5, Formatting.DARK_GREEN, Items.EMERALD, new Item.Settings());
    public static VotingItem VOTE_WOW = new VotingItem(6, Formatting.GREEN, Items.DIAMOND, new Item.Settings());

    public static WrappedItem WRAPPED = new WrappedItem(new Item.Settings());

    public static Item FILL_WAND = new SimpleGlowingItem(new Item.Settings().maxCount(1), Items.BLAZE_ROD);
    public static Item UTIL_OPENER = new SimpleGlowingItem(new Item.Settings().maxCount(1), Items.PAPER);

    public static void register() {
        register("vote/terrible", VOTE_TERRIBLE);
        register("vote/bad", VOTE_BAD);
        register("vote/not_bad", VOTE_NOT_BAD);
        register("vote/okay", VOTE_OKAY);
        register("vote/good", VOTE_GOOD);
        register("vote/great", VOTE_GREAT);
        register("vote/wow", VOTE_WOW);

        register("wrapped_item", WRAPPED);
        register("util_opener", UTIL_OPENER);
        register("fill_wand", FILL_WAND);
    }

    private static <T extends Item> T register(String id, T item) {
        Registry.register(Registries.ITEM, id(id), item);
        return item;
    };
}
