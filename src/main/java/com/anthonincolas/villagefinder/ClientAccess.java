package com.anthonincolas.villagefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ClientAccess {
    public static void advancedItemTooltip(List<Component> components, String shiftMessage) {
        components.add(Component.nullToEmpty(shiftMessage));
    }
    public static void advancedItemTooltip(List<Component> components, String shiftMessage, String noShiftMessage) {
        if(Screen.hasShiftDown()) {
            components.add(Component.nullToEmpty(noShiftMessage));
        } else {
            components.add(Component.nullToEmpty(shiftMessage));
        }
    }
}
