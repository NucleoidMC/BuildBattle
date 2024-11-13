package eu.pb4.buildbattle;

import com.mojang.logging.LogUtils;
import eu.pb4.buildbattle.custom.BBRegistry;
import eu.pb4.buildbattle.custom.FloorChangingEntity;
import eu.pb4.buildbattle.game.stages.BuildingStage;
import eu.pb4.buildbattle.themes.ThemesRegistry;
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
import xyz.nucleoid.plasmid.api.game.GameAttachment;
import xyz.nucleoid.plasmid.api.game.GameType;
import net.minecraft.util.Identifier;
import eu.pb4.buildbattle.game.BuildBattleConfig;
import eu.pb4.buildbattle.game.stages.WaitingStage;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.StimulusEvent;

import static eu.pb4.buildbattle.other.BbUtils.id;

public class BuildBattle implements ModInitializer {

    public static final String ID = "buildbattle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final GameType<BuildBattleConfig> TYPE = GameType.register(
            Identifier.of(ID, "buildbattle"),
            BuildBattleConfig.CODEC,
            WaitingStage::open
    );


    public static final GameAttachment<BuildingStage> ACTIVE_GAME = GameAttachment.create(id("active_game"));


    public static final GameRuleType CREATIVE_LIMIT = GameRuleType.create();

    public static final TagKey<Item> BANNED_ITEMS = TagKey.of(RegistryKeys.ITEM, Identifier.of(ID, "banned"));

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
