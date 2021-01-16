package eu.pb4.buildbattle.custom;

import eu.pb4.buildbattle.BuildBattle;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class BBItems {
    public static VotingItem WT = new VotingItem(0, Formatting.DARK_RED, Items.NETHER_BRICK, new FabricItemSettings());
    public static VotingItem BAD = new VotingItem(1, Formatting.RED, Items.NETHER_WART, new FabricItemSettings());
    public static VotingItem NB = new VotingItem(2, Formatting.GOLD, Items.COAL, new FabricItemSettings());
    public static VotingItem OKAY = new VotingItem(3, Formatting.YELLOW, Items.IRON_INGOT, new FabricItemSettings());
    public static VotingItem GOOD = new VotingItem(4, Formatting.BLUE, Items.GOLD_INGOT, new FabricItemSettings());
    public static VotingItem GREAT = new VotingItem(5, Formatting.DARK_GREEN, Items.EMERALD, new FabricItemSettings());
    public static VotingItem WOW = new VotingItem(6, Formatting.GREEN, Items.DIAMOND, new FabricItemSettings());


    public static void register() {
        Registry.register(Registry.ITEM, new Identifier(BuildBattle.ID, "wt"), WT);
        Registry.register(Registry.ITEM, new Identifier(BuildBattle.ID, "bad"), BAD);
        Registry.register(Registry.ITEM, new Identifier(BuildBattle.ID, "nb"), NB);
        Registry.register(Registry.ITEM, new Identifier(BuildBattle.ID, "okay"), OKAY);
        Registry.register(Registry.ITEM, new Identifier(BuildBattle.ID, "good"), GOOD);
        Registry.register(Registry.ITEM, new Identifier(BuildBattle.ID, "great"), GREAT);
        Registry.register(Registry.ITEM, new Identifier(BuildBattle.ID, "wow"), WOW);
    }
}
