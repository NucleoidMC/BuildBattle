package eu.pb4.buildbattle.custom;

import com.mojang.datafixers.util.Pair;
import eu.pb4.buildbattle.game.map.BuildArena;
import eu.pb4.buildbattle.mixin.BucketItemAccessor;
import eu.pb4.buildbattle.mixin.VillagerEntityAccessor;
import eu.pb4.buildbattle.other.BbUtils;
import eu.pb4.polymer.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerData;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

public class FloorChangingEntity extends MobEntity implements PolymerEntity {
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
        this.setCustomName(Text.translatable("text.buildbattle.floor_change").formatted(Formatting.GOLD));
        this.villagerData = new VillagerData(Registry.VILLAGER_TYPE.getRandom(this.getRandom()).get().value(), Registry.VILLAGER_PROFESSION.getRandom(this.getRandom()).get().value(), 3);
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
    public EntityType<?> getPolymerEntityType() {
        return EntityType.VILLAGER;
    }

    @Override
    public List<Pair<EquipmentSlot, ItemStack>> getPolymerVisibleEquipment(Map<EquipmentSlot, ItemStack> map) {
        return List.of(Pair.of(EquipmentSlot.MAINHAND, this.lastUsedFloor));
    }

    @Override
    public void modifyTrackedData(List<DataTracker.Entry<?>> data) {
        data.add(new DataTracker.Entry<>(VillagerEntityAccessor.get(), this.villagerData));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.buildArena != null) {
            if (this.buildArena.isBuilder(player)) {
                BlockState state = BbUtils.getStateFrom((ServerPlayerEntity) player, player.getStackInHand(hand));
                if (state != null) {
                    this.lastUsedFloor = player.getStackInHand(hand);
                    this.equipStack(EquipmentSlot.MAINHAND, this.lastUsedFloor);
                    for (BlockPos blockPos : this.buildArena.ground) {
                        this.world.setBlockState(blockPos, state);
                    }

                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.FAIL;
    }
}

