package eu.pb4.buildbattle.custom;

import com.mojang.datafixers.util.Pair;
import eu.pb4.buildbattle.game.map.BuildArena;
import eu.pb4.buildbattle.game.stages.BuildingStage;
import eu.pb4.buildbattle.mixin.VillagerEntityAccessor;
import eu.pb4.buildbattle.other.BbUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillagerData;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.game.manager.GameSpaceManager;

import java.util.List;

public class FloorChangingEntity extends LivingEntity implements PolymerEntity {
    public static EntityType<FloorChangingEntity> TYPE = FabricEntityTypeBuilder.<FloorChangingEntity>create(SpawnGroup.MISC, FloorChangingEntity::new).dimensions(EntityDimensions.fixed(0.75f, 2f)).build();
    private final VillagerData villagerData;
    private ItemStack lastUsedFloor = Items.GRASS_BLOCK.getDefaultStack();

    public FloorChangingEntity(EntityType<FloorChangingEntity> type, World world) {
        super(type, world);
        this.setCustomNameVisible(true);
        this.setSilent(true);
        this.setNoGravity(true);
        this.setCustomName(Text.translatable("text.buildbattle.floor_change").formatted(Formatting.GOLD));
        this.villagerData = new VillagerData(Registries.VILLAGER_TYPE.getRandom(this.getRandom()).get().value(), Registries.VILLAGER_PROFESSION.getRandom(this.getRandom()).get().value(), 3);
    }

    public FloorChangingEntity(World world) {
        this(TYPE, world);
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
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    public boolean canTakeDamage() {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return List.of();
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {

    }

    @Override
    public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return EntityType.VILLAGER;
    }

    @Override
    public List<Pair<EquipmentSlot, ItemStack>> getPolymerVisibleEquipment(List<Pair<EquipmentSlot, ItemStack>> map, ServerPlayerEntity player) {
        return List.of(Pair.of(EquipmentSlot.MAINHAND, this.lastUsedFloor));
    }

    @Override
    public void modifyRawTrackedData(List<DataTracker.SerializedEntry<?>> data, ServerPlayerEntity player, boolean initial) {
        data.add(DataTracker.SerializedEntry.of(VillagerEntityAccessor.get(), this.villagerData));
    }


    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        BuildArena buildArena = null;
        var game = GameSpaceManager.get().byWorld(this.getWorld());
        if (game != null) {
            BuildingStage stage = game.getAttachment("game_active");

            if (stage != null) {
                buildArena = stage.gameMap.getArena(this.getBlockPos());
            }
        }


        if (buildArena == null) {
            return ActionResult.FAIL;
        }


        if (buildArena.isBuilder(player)) {
            BlockState state = BbUtils.getStateFrom((ServerPlayerEntity) player, player.getStackInHand(hand));
            if (state != null) {
                this.lastUsedFloor = player.getStackInHand(hand);
                this.equipStack(EquipmentSlot.MAINHAND, this.lastUsedFloor);
                for (BlockPos blockPos : buildArena.ground) {
                    this.getWorld().setBlockState(blockPos, state);
                }

                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.FAIL;
    }
}

