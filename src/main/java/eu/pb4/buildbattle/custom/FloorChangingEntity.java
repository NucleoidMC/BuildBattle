package eu.pb4.buildbattle.custom;

import com.mojang.datafixers.util.Pair;
import eu.pb4.buildbattle.game.map.BuildArena;
import eu.pb4.buildbattle.mixin.VillagerEntityAccessor;
import eu.pb4.buildbattle.other.GeneralUtils;
import eu.pb4.polymer.entity.VirtualEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

public class FloorChangingEntity extends MobEntity implements VirtualEntity {
    public static EntityType<FloorChangingEntity> TYPE = FabricEntityTypeBuilder.<FloorChangingEntity>create(SpawnGroup.MISC, FloorChangingEntity::new).dimensions(EntityDimensions.fixed(0.75f, 2f)).build();
    private BuildArena buildArena;
    private final VillagerData villagerData;
    private ItemStack lastUsedFloor = Items.GRASS_BLOCK.getDefaultStack();

    public FloorChangingEntity(EntityType<FloorChangingEntity> type, World world) {
        super(type, world);
        this.buildArena = null;
        this.setPersistent();
        this.setCustomNameVisible(true);
        this.setSilent(true);
        this.setNoGravity(true);
        this.setCustomName(new TranslatableText("text.buildbattle.floor_change").formatted(Formatting.GOLD));
        this.villagerData = new VillagerData(Registry.VILLAGER_TYPE.getRandom(this.getRandom()), Registry.VILLAGER_PROFESSION.getRandom(this.getRandom()), 3);
    }

    public FloorChangingEntity(World world) {
        this(TYPE, world);
    }

    public FloorChangingEntity(World world, BuildArena arena) {
        this(world);
        this.buildArena = arena;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void tickMovement() {
        this.turnHead(this.getYaw(), this.getYaw());
    }

    @Override
    public boolean canTakeDamage() {
        return false;
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }

    @Override
    public void attachLeash(Entity entity, boolean sendPacket) { }

    @Override
    public EntityType<?> getVirtualEntityType() {
        return EntityType.VILLAGER;
    }

    @Override
    public List<Pair<EquipmentSlot, ItemStack>> getVirtualEntityEquipment(Map<EquipmentSlot, ItemStack> map) {
        return List.of(Pair.of(EquipmentSlot.MAINHAND, this.lastUsedFloor));
    }

    @Override
    public void modifyTrackedData(List<DataTracker.Entry<?>> data) {
        data.add(new DataTracker.Entry<>(VillagerEntityAccessor.get(), this.villagerData));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        Item item = player.getStackInHand(hand).getItem();

        if (this.buildArena != null) {
            if (this.buildArena.isBuilder(player)) {
                BlockState blockState = null;

                if (item instanceof BlockItem) {
                    Block block = ((BlockItem) item).getBlock();

                    if (block instanceof LeavesBlock) {
                        blockState = block.getDefaultState().with(LeavesBlock.PERSISTENT, true);
                    } else if (!(GeneralUtils.equalsOrInstance(block, ChorusPlantBlock.class, AbstractButtonBlock.class, LeverBlock.class, BlockEntityProvider.class,
                            DoorBlock.class, CactusBlock.class, SugarCaneBlock.class, VineBlock.class, PlantBlock.class, SmallDripleafBlock.class, RootsBlock.class,
                            HangingRootsBlock.class, AbstractLichenBlock.class, BambooSaplingBlock.class, BambooBlock.class))
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
                    this.lastUsedFloor = item.getDefaultStack();
                    this.equipStack(EquipmentSlot.MAINHAND, this.lastUsedFloor);
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

