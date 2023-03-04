package com.anthonincolas.villagefinder.Item;

import com.anthonincolas.villagefinder.ClientAccess;
import com.anthonincolas.villagefinder.Utils.MessageDisplay;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
    private static final int MIN_DISTANCE_BETWEEN_VILLAGES = 200;
    private static BlockPos previousVillagePosition;
    private static final SoundEvent ORB_PICKUP_SOUND = SoundEvents.EXPERIENCE_ORB_PICKUP;

    private static int villagePositionX;
    private static int villagePositionY;
    private static int villagePositionZ;

    private static GlobalPos villagePosition;

    private static boolean isVillageFound = false;
    private static boolean canUserBeTeleported = false;

    private static final int MAX_STACK_SIZE = 1;

    public VillageCompassProperties(Item.Properties properties) {
        super(properties);

        villagePositionX = 0;
        villagePositionY = 0;
        villagePositionZ = 0;
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
                canUserBeTeleported = true;
            } else {
                player.getCooldowns().removeCooldown(this);
                Style style = Style.EMPTY.withColor(TextColor.parseColor("red"));
                MessageDisplay.sendMessage(player, "Aucun village trouvé", style, true);
            }
        }

        if(!isClient && Screen.hasShiftDown() && isMainHand) {
            if(canUserBeTeleported) {
                MessageDisplay.sendMessage(player, "Téléportation en cours...", MessageDisplay.messageColorBlue, true);
                player.teleportTo(villagePositionX, villagePositionY, villagePositionZ);
            }
        }

        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack item, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag tooltip) {
        super.appendHoverText(item, level, components, tooltip);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAccess.advancedItemTooltip(components, "Appuyer sur shift pour plus d'information"));
    }

    public static void detectNearbyVillage(Player player) {
        /*if (isVillageFound) {
            return;
        }*/

        int playerChunkX = player.chunkPosition().x;
        int playerChunkZ = player.chunkPosition().z;
        Level level = player.getLevel();

        for (int chunkX = playerChunkX - SEARCHING_VILLAGE_RADIUS; chunkX <= playerChunkX + SEARCHING_VILLAGE_RADIUS; chunkX++) {
            for (int chunkZ = playerChunkZ - SEARCHING_VILLAGE_RADIUS; chunkZ <= playerChunkZ + SEARCHING_VILLAGE_RADIUS; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                BlockPos villagePos = chunkPos.getWorldPosition();

                if (!PROCESSED_CHUNKS.add(chunkPos)) {
                    MessageDisplay.sendMessage(player, "Chunk déjà traité", MessageDisplay.messageColorRed, true);
                    continue;
                }

                if (!level.isAreaLoaded(villagePos, 16)) {
                    MessageDisplay.sendMessage(player, "Chunk non chargé", MessageDisplay.messageColorRed, true);
                    continue;
                }

                /*if(previousVillagePosition != null) {
                    int distanceBetweenVillages = (int) Math.sqrt(Math.pow(previousVillagePosition.getX() - villagePos.getX(), 2) + Math.pow(previousVillagePosition.getZ() - villagePos.getZ(), 2));
                    if(distanceBetweenVillages < MIN_DISTANCE_BETWEEN_VILLAGES) {
                        MessageDisplay.sendMessage(player, "Vous êtes trop proche ", MessageDisplay.messageColorRed, true);
                        continue;
                    }
                }*/

                ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);
                /*MessageDisplay.sendMessage(player, "Chunk : " + chunkPos.x + " " + chunkPos.z, MessageDisplay.messageColorBlue);*/
                Map<Structure, StructureStart> structures = chunk.getAllStarts();

                if(!PROCESSED_CHUNKS.isEmpty())
                    PROCESSED_CHUNKS.clear();

                for (Map.Entry<Structure, StructureStart> entry : structures.entrySet()) {
                    Structure feature = entry.getKey();
                    boolean chunkContainsVillage = feature.modifiableStructureInfo().getOriginalStructureInfo()
                            .structureSettings().toString().contains("village");
                    MessageDisplay.sendMessage(player, "Village ICI", MessageDisplay.messageColorGreen, true);

                    if(chunkContainsVillage) {
                        StructureStart villageStart = structures.get(feature);
                        villagePos = villageStart.getBoundingBox().getCenter();
                        villagePositionX = villagePos.getX();
                        villagePositionY = villagePos.getY();
                        villagePositionZ = villagePos.getZ();
                        villagePosition = GlobalPos.of(level.dimension(), villagePos);
                        canUserBeTeleported = true;
                        isVillageFound = true;
                        MessageDisplay.sendMessage(player, "Village trouvé !", MessageDisplay.messageColorGreen, true);
                        MessageDisplay.sendMessage(player, "Position du village : " + villagePos.getX() + " " + villagePos.getY() + " " + villagePos.getZ(), MessageDisplay.messageColorBlue);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if(worldIn.isClientSide && entityIn instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entityIn;
            stack.getOrCreateTag().putFloat("angle", livingEntity.yRotO + (livingEntity.yBodyRot - livingEntity.yRotO) * 0.5f);
        }
    }

    public float getVillagePosition() {
        return villagePositionX;
    }

}
