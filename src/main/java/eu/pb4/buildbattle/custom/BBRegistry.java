package eu.pb4.buildbattle.custom;

import eu.pb4.buildbattle.custom.items.SimpleGlowingItem;
import eu.pb4.buildbattle.custom.items.VotingItem;
import eu.pb4.buildbattle.custom.items.WrappedItem;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Formatting;

import java.util.function.Function;

import static eu.pb4.buildbattle.other.BbUtils.id;

public interface BBRegistry {
    VotingItem VOTE_TERRIBLE = registerItem("vote/terrible", (settings) -> new VotingItem(0, Formatting.DARK_RED, Items.NETHER_BRICK, settings));
    VotingItem VOTE_BAD = registerItem("vote/bad", (settings) -> new VotingItem(1, Formatting.RED, Items.NETHER_WART, settings));
    VotingItem VOTE_NOT_BAD = registerItem("vote/not_bad", (settings) -> new VotingItem(2, Formatting.GOLD, Items.COAL, settings));
    VotingItem VOTE_OKAY = registerItem("vote/okay", (settings) -> new VotingItem(3, Formatting.YELLOW, Items.IRON_INGOT, settings));
    VotingItem VOTE_GOOD = registerItem("vote/good", (settings) -> new VotingItem(4, Formatting.BLUE, Items.GOLD_INGOT, settings));
    VotingItem VOTE_GREAT =  registerItem("vote/great", (settings) ->  new VotingItem(5, Formatting.DARK_GREEN, Items.EMERALD, settings));
    VotingItem VOTE_WOW = registerItem("vote/wow", (settings) ->  new VotingItem(6, Formatting.GREEN, Items.DIAMOND, settings));

    WrappedItem WRAPPED = registerItem("wrapped_item", WrappedItem::new);

    Item FILL_WAND = registerItem("fill_wand", (settings) ->  new SimpleGlowingItem(settings.maxCount(1), Items.BLAZE_ROD));
    Item UTIL_OPENER = registerItem("util_opener", (settings) ->  new SimpleGlowingItem(settings.maxCount(1), Items.PAPER));

    ComponentType<ItemStack> WRAPPED_ITEM = register("wrapped", Registries.DATA_COMPONENT_TYPE,
            ComponentType.<ItemStack>builder().codec(ItemStack.OPTIONAL_CODEC).build());

    EntityType<FloorChangingEntity> FLOOR_CHANGER_ENTITY = registerEntity("floor_changer", EntityType.Builder.<FloorChangingEntity>create(FloorChangingEntity::new, SpawnGroup.MISC).dimensions(0.75f, 2f));

    static void registerItem() {
        PolymerComponent.registerDataComponent(WRAPPED_ITEM);
    }

    private static <T extends Item> T registerItem(String path, Function<Item.Settings, T> function) {
        var id = id(path);
        var item = function.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        Registry.register(Registries.ITEM, id, item);
        return item;
    }

    private static <T extends Entity> EntityType<T> registerEntity(String path, EntityType.Builder<T> builder) {
        var id = id(path);
        var item = builder.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, id));
        Registry.register(Registries.ENTITY_TYPE, id, item);
        PolymerEntityUtils.registerType(item);
        return item;
    }

    private static <T, B extends T> B register(String path, Registry<T> registry, B object) {
        var id = id(path);
        Registry.register(registry, id, object);
        return object;
    }

}
