package com.anthonincolas.villagefinder;

import com.anthonincolas.villagefinder.Player.PlayerInit;
import com.anthonincolas.villagefinder.Init.ItemInit;
import com.anthonincolas.villagefinder.Utils.MessageDisplay;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(villagefinder.MODID)
public class villagefinder
{
    public static final String MODID = "villagefinder";
    public villagefinder()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ItemInit.register(modEventBus);
        PlayerInit.register();


        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
    }

    private void addCreative(CreativeModeTabEvent.BuildContents event)
    {
        if(event.getTab() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ItemInit.VILLAGE_COMPASS);
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> {
                // do something that can only be done on the client
                ItemProperties.register(ItemInit.VILLAGE_COMPASS.get(), new ResourceLocation("angle"), (ItemStack stack, ClientLevel level, LivingEntity entity, int id) -> {
                    if(entity == null || !entity.isAlive()) {
                        return 0.0F;
                    } else {
                        float angle = stack.getOrCreateTag().getFloat("angle");
                        float animationAngle = Math.floorMod((int)(angle * 10), 3600) / 10.0F;
                        MessageDisplay.sendMessage(PlayerInit.getPlayer(), "Angle: " + animationAngle);
                        return animationAngle / 360.0F;
                    }
                });
            });
        }
    }
}
