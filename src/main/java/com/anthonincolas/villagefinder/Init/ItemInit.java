package com.anthonincolas.villagefinder.Init;

import com.anthonincolas.villagefinder.Item.VillageCompassProperties;
import com.anthonincolas.villagefinder.villagefinder;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit {

    public ItemInit() {
        super();
    }

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, villagefinder.MODID);

    public static final RegistryObject<VillageCompassProperties> VILLAGE_COMPASS =
            ITEMS.register("village_compass", () -> new VillageCompassProperties(compassProps(new Item.Properties())));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }


    public static Item.Properties compassProps(Item.Properties props) {
        return props.stacksTo(1);
    }

}
