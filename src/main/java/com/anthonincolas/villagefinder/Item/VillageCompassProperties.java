package com.anthonincolas.villagefinder.Item;

import com.anthonincolas.villagefinder.ClientAccess;
import com.anthonincolas.villagefinder.Player.PlayerInit;
import com.anthonincolas.villagefinder.Utils.MessageDisplay;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class VillageCompassProperties extends Item {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SEARCHING_VILLAGE_RADIUS = 50;
    private static final Set<ChunkPos> PROCESSED_CHUNKS = new HashSet<>();
    private static final SoundEvent ORB_PICKUP_SOUND = SoundEvents.EXPERIENCE_ORB_PICKUP;
    private static int villagePositionX;
    private static int villagePositionY;
    private static int villagePositionZ;
    private static BlockPos villagePosition;
    private static boolean isVillageFound = false;
    private static boolean isPlayerCanBeTeleported = false;

    public VillageCompassProperties(Item.Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);
        boolean isClient = level.isClientSide;
        boolean isMainHand = hand == InteractionHand.MAIN_HAND;

        if(!isClient && isMainHand) {
            Style searchingStyle = Style.EMPTY.withColor(TextColor.parseColor("blue"));
            MessageDisplay.sendMessage(player, "Recherche en cours...", searchingStyle, true);
            player.getCooldowns().addCooldown(this, 50000);
            detectNearbyVillage(player);
            if(isVillageFound) {
                player.playSound(ORB_PICKUP_SOUND, 1.0f, 1.0f);
                player.getCooldowns().removeCooldown(this);
                isPlayerCanBeTeleported = true;
            } else {
                player.getCooldowns().removeCooldown(this);
                Style style = Style.EMPTY.withColor(TextColor.parseColor("red"));
                MessageDisplay.sendMessage(player, "Aucun village trouvé", style, true);
            }
        }

        if(!isClient && isMainHand && isPlayerCanBeTeleported && Screen.hasShiftDown()) {
            player.teleportTo(villagePositionX, villagePositionY, villagePositionZ);
            isPlayerCanBeTeleported = false;
        }

        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack item, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag tooltip) {
        super.appendHoverText(item, level, components, tooltip);
        if(villagePositionX != 0 && villagePositionY != 0 && villagePositionZ != 0) {
            Style style = Style.EMPTY.withColor(TextColor.parseColor("blue"));
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAccess.advancedItemTooltip(components, "Appuyer sur shift pour connaitre la position du dernier village trouvé", "Position du dernier village trouvé : " + villagePositionX + " " + villagePositionY + " " + villagePositionZ));
        }
    }

    public static void detectNearbyVillage(Player player) {
        isVillageFound = false;

        final int lastVillageX = villagePositionX;
        final int lastVillageY = villagePositionY;
        final int lastVillageZ = villagePositionZ;
        if(lastVillageX > 0 && lastVillageY > 0 && lastVillageZ > 0) {
            MessageDisplay.sendMessage(player, "Last village position : " + lastVillageX + " " + lastVillageY + " " + lastVillageZ, Style.EMPTY);
        }

        int playerChunkX = player.chunkPosition().x;
        int playerChunkZ = player.chunkPosition().z;
        Level level = player.getLevel();

        for (int chunkX = playerChunkX - SEARCHING_VILLAGE_RADIUS; chunkX <= playerChunkX + SEARCHING_VILLAGE_RADIUS; chunkX++) {
            for (int chunkZ = playerChunkZ - SEARCHING_VILLAGE_RADIUS; chunkZ <= playerChunkZ + SEARCHING_VILLAGE_RADIUS; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                BlockPos villagePos = chunkPos.getWorldPosition();
                MessageDisplay.sendMessage(player, "Chunk : " + chunkPos, Style.EMPTY, true);

                if (!PROCESSED_CHUNKS.add(chunkPos))
                    continue;

                if (!level.isAreaLoaded(villagePos, 24)){
                    continue;
                }

                ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);
                Map<Structure, StructureStart> structures = chunk.getAllStarts();

                if(!PROCESSED_CHUNKS.isEmpty())
                    PROCESSED_CHUNKS.clear();

                for (Map.Entry<Structure, StructureStart> entry : structures.entrySet()) {
                    Structure feature = entry.getKey();
                    boolean chunkContainsVillage = feature.modifiableStructureInfo().getOriginalStructureInfo()
                            .structureSettings().toString().contains("village");

                    if(chunkContainsVillage) {

                        StructureStart villageStart = structures.get(feature);
                        villagePos = villageStart.getBoundingBox().getCenter();
                        villagePositionX = villagePos.getX();
                        villagePositionY = villagePos.getY();
                        villagePositionZ = villagePos.getZ();
                        villagePosition = villagePos;
                        if(villagePositionX == lastVillageX && villagePositionY == lastVillageY && villagePositionZ == lastVillageZ) {
                            MessageDisplay.sendMessage(player, "Village déjà trouvé", Style.EMPTY);
                            continue;
                        } else {
                            MessageDisplay.sendMessage(player, "Village trouvé : " + villagePositionX + " " + villagePositionY + " " + villagePositionZ, Style.EMPTY, true);
                            isVillageFound = true;
                            return;
                        }
                    }
                }
            }
        }
    }

    public static GlobalPos getVillagePosition(Level level) {
        BlockPos villagePos = new BlockPos(villagePositionX, villagePositionY, villagePositionZ);
        return GlobalPos.of(level.dimension(), villagePos);
    }

    public boolean isCurrentVillageSameAsLastVillage() {
        if(villagePositionX == villagePosition.getX() && villagePositionY == villagePosition.getY() && villagePositionZ == villagePosition.getZ()) {
            return true;
        }
        return false;
    }

}
