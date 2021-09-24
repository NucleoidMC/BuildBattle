package eu.pb4.buildbattle;

import eu.pb4.buildbattle.custom.BBItems;
import eu.pb4.buildbattle.custom.FloorChangingEntity;
import eu.pb4.buildbattle.themes.ThemesRegistry;
import eu.pb4.polymer.entity.EntityHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.plasmid.game.GameType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.pb4.buildbattle.game.BuildBattleConfig;
import eu.pb4.buildbattle.game.stages.WaitingStage;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public class BuildBattle implements ModInitializer {

    public static final String ID = "buildbattle";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<BuildBattleConfig> TYPE = GameType.register(
            new Identifier(ID, "buildbattle"),
            BuildBattleConfig.CODEC,
            WaitingStage::open
    );


    public static final GameRuleType CREATIVE_LIMIT = GameRuleType.create();

    @Override
    public void onInitialize() {
        BBItems.register();
        Registry.register(Registry.ENTITY_TYPE, new Identifier(ID, "floor_changer"), FloorChangingEntity.TYPE);
        FabricDefaultAttributeRegistry.register(FloorChangingEntity.TYPE, FloorChangingEntity.createMobAttributes());
        EntityHelper.registerVirtualEntityType(FloorChangingEntity.TYPE);

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
