package eu.pb4.buildbattle.custom;

import eu.pb4.buildbattle.game.BBActive;
import eu.pb4.buildbattle.game.map.BuildArena;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.game.ManagedGameSpace;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class FloorChangingVillager extends VillagerEntity {
    public final BuildArena buildArena;
    private final BBActive game;

    public FloorChangingVillager(World world) {
        super(EntityType.VILLAGER, world, VillagerType.PLAINS);
        this.buildArena = null;
        this.game = null;

    }

    public FloorChangingVillager(BuildArena arena, BBActive game, World world) {
        super(EntityType.VILLAGER, world, VillagerType.PLAINS);
        this.setInvisible(true);
        this.setAiDisabled(true);
        this.buildArena = arena;
        this.game = game;
        this.setCustomNameVisible(true);
        this.setSilent(true);
        this.setCustomName(new TranslatableText("buildbattle.text.floorchange").formatted(Formatting.GOLD));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.buildArena != null && this.game != null) {
            Item item = player.getStackInHand(hand).getItem();
            if (this.buildArena.players.contains(this.game.participants.get(PlayerRef.of(player)))
                    && item instanceof BlockItem
                    && !(((BlockItem) item).getBlock() instanceof PlantBlock)
                    && !this.game.stageManager.isVoting
                    && this.game.stageManager.waitTime == -1) {

                BlockState blockState = ((BlockItem) player.getStackInHand(hand).getItem()).getBlock().getDefaultState();

                for (BlockPos blockPos : this.buildArena.ground) {
                   this.world.setBlockState(blockPos, blockState);
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.FAIL;
    }

}

