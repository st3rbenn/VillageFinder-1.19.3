package com.anthonincolas.villagefinder.Item;

import com.anthonincolas.villagefinder.ClientAccess;
import com.anthonincolas.villagefinder.Player.PlayerInit;
import com.mojang.blaze3d.audio.Library;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.data.worldgen.Structures;
import net.minecraft.data.worldgen.VillagePools;
import net.minecraft.data.worldgen.placement.VillagePlacements;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GoToClosestVillage;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureFeatureIndexSavedData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.swing.text.html.Option;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VillageCompassProperties extends Item {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int SEARCHING_VILLAGE_RADIUS = 10;

    private int villagePositionX;
    private int villagePositionY;
    private int villagePositionZ;

    private boolean isVillageFound = false;
    private boolean canUserBeTeleported = false;

    private static final int MAX_STACK_SIZE = 1;
    SoundEvent sound = SoundEvents.EXPERIENCE_ORB_PICKUP;

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
            PlayerInit.sendMessage("Recherche en cours...", searchingStyle);
            if(detectNearbyVillage(player)) {
                Style style = Style.EMPTY.withColor(TextColor.parseColor("blue"));
                PlayerInit.sendMessage("Village trouvé !", style);
                PlayerInit.getPlayer().playSound(sound, 1.0F, 1.0F);
                PlayerInit.sendMessage("Position du village : " + villagePositionX + " " + villagePositionY + " " + villagePositionZ, style);
                canUserBeTeleported = true;
            } else {
                Style style = Style.EMPTY.withColor(TextColor.parseColor("red"));
                PlayerInit.sendMessage("Aucun village trouvé", style);
            }
        }

        if(!isClient && isMainHand && Screen.hasShiftDown()) {
            if(canUserBeTeleported) {
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

    public boolean detectNearbyVillage(Player player) {
        int chunkX = player.chunkPosition().x;
        int chunkZ = player.chunkPosition().z;

        Level level = player.getLevel();
        MinecraftServer server = level.getServer();

        for(int x = chunkX - SEARCHING_VILLAGE_RADIUS; x <= chunkX + SEARCHING_VILLAGE_RADIUS; x++) {
            for(int z = chunkZ - SEARCHING_VILLAGE_RADIUS; z <= chunkZ + SEARCHING_VILLAGE_RADIUS; z++) {
                ChunkPos chunkPos = new ChunkPos(x, z);
                ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);
                TagKey<Structure> structureTags = StructureTags.VILLAGE;
                Map<Structure, StructureStart> structures = chunk.getAllStarts();
                if(!structures.isEmpty()){
                    for(Structure structure : structures.keySet()) {
                        Structure village = structures.get(structure).getStructure();
                        boolean chunkContaineVillage = village.modifiableStructureInfo().getOriginalStructureInfo().structureSettings().toString().contains("village");
                        if(chunkContaineVillage) {
                            StructureStart villageStart = structures.get(structure);
                            LOGGER.info("village ? " + village.modifiableStructureInfo().getOriginalStructureInfo().structureSettings().toString().contains("village"));
                            BlockPos villagePos = villageStart.getBoundingBox().getCenter();
                            villagePositionY = villagePos.getY();
                            villagePositionX = villagePos.getX();
                            villagePositionZ = villagePos.getZ();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
