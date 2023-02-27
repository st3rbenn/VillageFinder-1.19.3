package com.anthonincolas.villagefinder.Item;

import com.anthonincolas.villagefinder.Init.ItemInit;
import com.anthonincolas.villagefinder.Player.PlayerInit;
import net.minecraft.world.item.Item;

public class Utils extends Item {

    public Utils(Properties properties) {
        super(properties);
    }

    public static void GiveCompass() {
        PlayerInit.getPlayer().getInventory().add(ItemInit.VILLAGE_COMPASS.get().getDefaultInstance());
    }
}
