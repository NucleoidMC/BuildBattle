package eu.pb4.buildbattle;

import com.mojang.logging.LogUtils;
import eu.pb4.buildbattle.custom.BBRegistry;
import eu.pb4.buildbattle.custom.FloorChangingEntity;
import eu.pb4.buildbattle.game.stages.BuildingStage;
import eu.pb4.buildbattle.themes.ThemesRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import xyz.nucleoid.plasmid.api.game.GameAttachment;
import xyz.nucleoid.plasmid.api.game.GameType;
import eu.pb4.buildbattle.game.BuildBattleConfig;
import eu.pb4.buildbattle.game.stages.WaitingStage;
import xyz.nucleoid.plasmid.api.game.GameTypes;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.StimulusEvent;

import static eu.pb4.buildbattle.other.BbUtils.id;

public class BuildBattle implements ModInitializer {

    public static final String ID = "buildbattle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final GameType<BuildBattleConfig> TYPE = GameTypes.register(
            Identifier.fromNamespaceAndPath(ID, "buildbattle"),
            BuildBattleConfig.CODEC,
            WaitingStage::open
    );


    public static final GameAttachment<BuildingStage> ACTIVE_GAME = GameAttachment.create(id("active_game"));


    public static final GameRuleType CREATIVE_LIMIT = GameRuleType.create();

    public static final TagKey<Item> BANNED_ITEMS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "banned"));

    @Override
    public void onInitialize() {
        BBRegistry.registerItem();
        FabricDefaultAttributeRegistry.register(BBRegistry.FLOOR_CHANGER_ENTITY, FloorChangingEntity.createLivingAttributes());
        ThemesRegistry.register();
    }

    public static StimulusEvent<BucketUsage> ON_BUCKET_USAGE = StimulusEvent.create(BucketUsage.class, ctx -> (player, blockPos) -> {
        try {
            for (var listener : ctx.getListeners()) {
                var result = listener.onUse(player, blockPos);
                if (result != InteractionResult.PASS) {
                    return result;
                }
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
        return InteractionResult.PASS;
    });


    public interface BucketUsage {
        InteractionResult onUse(ServerPlayer player, BlockPos pos);
    }
}
