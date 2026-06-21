package eu.pb4.buildbattle.custom;

import com.mojang.datafixers.util.Pair;
import eu.pb4.buildbattle.BuildBattle;
import eu.pb4.buildbattle.game.map.BuildArena;
import eu.pb4.buildbattle.game.stages.BuildingStage;
import eu.pb4.buildbattle.mixin.VillagerEntityAccessor;
import eu.pb4.buildbattle.other.BbUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

import java.util.List;

public class FloorChangingEntity extends LivingEntity implements PolymerEntity {
    private final VillagerData villagerData;
    private ItemStack lastUsedFloor = Items.GRASS_BLOCK.getDefaultInstance();

    public FloorChangingEntity(EntityType<FloorChangingEntity> type, Level world) {
        super(type, world);
        this.setCustomNameVisible(true);
        this.setSilent(true);
        this.setNoGravity(true);
        this.setCustomName(Component.translatable("text.buildbattle.floor_change").withStyle(ChatFormatting.GOLD));
        this.villagerData = new VillagerData(BuiltInRegistries.VILLAGER_TYPE.getRandom(this.getRandom()).get(), BuiltInRegistries.VILLAGER_PROFESSION.getRandom(this.getRandom()).get(), 3);
    }

    public FloorChangingEntity(Level world) {
        this(BBRegistry.FLOOR_CHANGER_ENTITY, world);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void aiStep() {
        this.tickHeadTurn(this.getYRot());
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityTypes.VILLAGER;
    }

    @Override
    public List<Pair<EquipmentSlot, ItemStack>> getPolymerVisibleEquipment(List<Pair<EquipmentSlot, ItemStack>> map, ServerPlayer player) {
        return List.of(Pair.of(EquipmentSlot.MAINHAND, this.lastUsedFloor));
    }

    @Override
    public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
        data.add(SynchedEntityData.DataValue.create(VillagerEntityAccessor.get(), this.villagerData));
    }


    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 pos) {
        BuildArena buildArena = null;
        var game = GameSpaceManager.get().byLevel(this.level());
        if (game != null) {
            BuildingStage stage = game.getAttachment(BuildBattle.ACTIVE_GAME);

            if (stage != null) {
                buildArena = stage.gameMap.getArena(this.blockPosition());
            }
        }


        if (buildArena == null) {
            return InteractionResult.FAIL;
        }


        if (buildArena.isBuilder(player)) {
            BlockState state = BbUtils.getStateFrom((ServerPlayer) player, player.getItemInHand(hand));
            if (state != null) {
                this.lastUsedFloor = player.getItemInHand(hand);
                ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEquipmentPacket(this.getId(),
                        List.of(new Pair<>(EquipmentSlot.MAINHAND, this.lastUsedFloor))));
                for (BlockPos blockPos : buildArena.ground) {
                    this.level().setBlockAndUpdate(blockPos, state);
                }

                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.FAIL;
    }
}

