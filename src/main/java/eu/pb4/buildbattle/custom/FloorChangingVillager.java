package eu.pb4.buildbattle.custom;

import eu.pb4.buildbattle.game.BBActive;
import eu.pb4.buildbattle.game.map.BuildArena;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
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
        this.setNoGravity(true);
        this.setPersistent();
        this.setCustomName(new TranslatableText("buildbattle.text.floorchange").formatted(Formatting.GOLD));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        Item item = player.getStackInHand(hand).getItem();

        if (this.buildArena != null && this.game != null) {
            if (this.buildArena.players.contains(this.game.participants.get(PlayerRef.of(player)))
                    && !this.game.stageManager.isVoting
                    && this.game.stageManager.waitTime == -1) {

                BlockState blockState = null;


                if (item instanceof BlockItem) {
                    Block block = ((BlockItem) item).getBlock();

                    if (!(block instanceof ChorusFlowerBlock)
                            && !(block instanceof ChorusPlantBlock)
                            && !(block instanceof StoneButtonBlock)
                            && !(block instanceof WoodenButtonBlock)
                            && !(block instanceof LeverBlock)
                            && !(block instanceof AbstractSkullBlock)
                            && !(block instanceof DoorBlock)
                            && !(block instanceof CactusBlock)
                            && !(block instanceof SugarCaneBlock)
                            && !(block instanceof VineBlock)
                    ) {
                        blockState = block.getDefaultState();
                    }
                } else if (item == Items.BUCKET) {
                    blockState = Blocks.AIR.getDefaultState();
                } else if (item == Items.WATER_BUCKET) {
                    blockState = Blocks.WATER.getDefaultState();
                } else if (item == Items.LAVA_BUCKET) {
                    blockState = Blocks.LAVA.getDefaultState();
                }

                if (blockState != null) {
                    for (BlockPos blockPos : this.buildArena.ground) {
                        this.world.setBlockState(blockPos, blockState);
                    }

                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.FAIL;
    }

}

