package eu.pb4.buildbattle;

import com.mojang.logging.LogUtils;
import eu.pb4.buildbattle.custom.BBItems;
import eu.pb4.buildbattle.custom.FloorChangingEntity;
import eu.pb4.buildbattle.themes.ThemesRegistry;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import xyz.nucleoid.plasmid.game.GameType;
import net.minecraft.util.Identifier;
import eu.pb4.buildbattle.game.BuildBattleConfig;
import eu.pb4.buildbattle.game.stages.WaitingStage;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public class BuildBattle implements ModInitializer {

    public static final String ID = "buildbattle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final GameType<BuildBattleConfig> TYPE = GameType.register(
            new Identifier(ID, "buildbattle"),
            BuildBattleConfig.CODEC,
            WaitingStage::open
    );


    public static final GameRuleType CREATIVE_LIMIT = GameRuleType.create();

    public static final TagKey<Item> BANNED_ITEMS = TagKey.of(RegistryKeys.ITEM, new Identifier(ID, "banned"));

    @Override
    public void onInitialize() {
        BBItems.register();
        Registry.register(Registries.ENTITY_TYPE, new Identifier(ID, "floor_changer"), FloorChangingEntity.TYPE);
        FabricDefaultAttributeRegistry.register(FloorChangingEntity.TYPE, FloorChangingEntity.createLivingAttributes());
        PolymerEntityUtils.registerType(FloorChangingEntity.TYPE);

        ThemesRegistry.register();
    }

    public static StimulusEvent<BucketUsage> ON_BUCKET_USAGE = StimulusEvent.create(BucketUsage.class, ctx -> (player, blockPos) -> {
        try {
            for (var listener : ctx.getListeners()) {
                var result = listener.onUse(player, blockPos);
                if (result != ActionResult.PASS) {
                    return result;
                }
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
        return ActionResult.PASS;
    });


    public interface BucketUsage {
        ActionResult onUse(ServerPlayerEntity player, BlockPos pos);
    }
}
