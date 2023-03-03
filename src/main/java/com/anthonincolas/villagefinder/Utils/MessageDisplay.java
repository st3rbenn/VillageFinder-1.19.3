package com.anthonincolas.villagefinder.Utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;

public class MessageDisplay {

    public static final Style messageColorBlue = Style.EMPTY.withColor(TextColor.parseColor("blue"));
    public static final Style messageColorGreen = Style.EMPTY.withColor(TextColor.parseColor("green"));
    public static final Style messageColorRed = Style.EMPTY.withColor(TextColor.parseColor("red"));


    public static void sendMessage(Player player, String message) {
        Style style = Style.EMPTY.withColor(TextColor.parseColor("white"));
        Component messageToDisplay = Component.nullToEmpty(message).copy().withStyle(style);
        player.sendSystemMessage(messageToDisplay);
    }

    public static void sendMessage(Player player, String message, Style style) {
        Component messageToDisplay = Component.nullToEmpty(message).copy().withStyle(style);
        player.sendSystemMessage(messageToDisplay);
    }

    public static void sendMessage(Player player, String message, boolean isClientMessage) {
        Style style = Style.EMPTY.withColor(TextColor.parseColor("white"));
        Component messageToDisplay = Component.nullToEmpty(message).copy().withStyle(style);
        player.displayClientMessage(messageToDisplay, isClientMessage);
    }

    public static void sendMessage(Player player, String message, Style style , boolean isClientMessage) {
        Component messageToDisplay = Component.nullToEmpty(message).copy().withStyle(style);
        player.displayClientMessage(messageToDisplay, isClientMessage);
    }
}
