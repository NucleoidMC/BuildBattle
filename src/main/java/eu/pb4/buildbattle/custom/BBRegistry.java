package eu.pb4.buildbattle.custom;

import eu.pb4.buildbattle.custom.items.SimpleGlowingItem;
import eu.pb4.buildbattle.custom.items.VotingItem;
import eu.pb4.buildbattle.custom.items.WrappedItem;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static eu.pb4.buildbattle.other.BbUtils.id;

public interface BBRegistry {
    VotingItem VOTE_TERRIBLE = registerItem("vote/terrible", (settings) -> new VotingItem(0, ChatFormatting.DARK_RED, Items.NETHER_BRICK, settings));
    VotingItem VOTE_BAD = registerItem("vote/bad", (settings) -> new VotingItem(1, ChatFormatting.RED, Items.NETHER_WART, settings));
    VotingItem VOTE_NOT_BAD = registerItem("vote/not_bad", (settings) -> new VotingItem(2, ChatFormatting.GOLD, Items.COAL, settings));
    VotingItem VOTE_OKAY = registerItem("vote/okay", (settings) -> new VotingItem(3, ChatFormatting.YELLOW, Items.IRON_INGOT, settings));
    VotingItem VOTE_GOOD = registerItem("vote/good", (settings) -> new VotingItem(4, ChatFormatting.BLUE, Items.GOLD_INGOT, settings));
    VotingItem VOTE_GREAT =  registerItem("vote/great", (settings) ->  new VotingItem(5, ChatFormatting.DARK_GREEN, Items.EMERALD, settings));
    VotingItem VOTE_WOW = registerItem("vote/wow", (settings) ->  new VotingItem(6, ChatFormatting.GREEN, Items.DIAMOND, settings));

    WrappedItem WRAPPED = registerItem("wrapped_item", WrappedItem::new);

    Item FILL_WAND = registerItem("fill_wand", (settings) ->  new SimpleGlowingItem(settings.stacksTo(1), Items.BLAZE_ROD));
    Item UTIL_OPENER = registerItem("util_opener", (settings) ->  new SimpleGlowingItem(settings.stacksTo(1), Items.PAPER));

    DataComponentType<ItemStack> WRAPPED_ITEM = register("wrapped", BuiltInRegistries.DATA_COMPONENT_TYPE,
            DataComponentType.<ItemStack>builder().persistent(ItemStack.OPTIONAL_CODEC).build());

    EntityType<FloorChangingEntity> FLOOR_CHANGER_ENTITY = registerEntity("floor_changer", EntityType.Builder.<FloorChangingEntity>of(FloorChangingEntity::new, MobCategory.MISC).sized(0.75f, 2f));

    static void registerItem() {
        PolymerComponent.registerDataComponent(WRAPPED_ITEM);
    }

    private static <T extends Item> T registerItem(String path, Function<Item.Properties, T> function) {
        var id = id(path);
        var item = function.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)));
        Registry.register(BuiltInRegistries.ITEM, id, item);
        return item;
    }

    private static <T extends Entity> EntityType<T> registerEntity(String path, EntityType.Builder<T> builder) {
        var id = id(path);
        var item = builder.build(ResourceKey.create(Registries.ENTITY_TYPE, id));
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id, item);
        PolymerEntityUtils.registerType(item);
        return item;
    }

    private static <T, B extends T> B register(String path, Registry<T> registry, B object) {
        var id = id(path);
        Registry.register(registry, id, object);
        return object;
    }

}
