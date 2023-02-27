package com.anthonincolas.villagefinder.Player;

import com.anthonincolas.villagefinder.Init.ItemInit;
import com.anthonincolas.villagefinder.Item.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerInit extends PlayerEvent {

    private static final int COLOR_RED = 0xFF0000;
    private static final int COLOR_GREEN = 0x00FF00;
    private static final int COLOR_BLUE = 0x0000FF;

    private static Player player;
    private static CompoundTag nbt;

    private static Level level;

    public PlayerInit() {
        super(player);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        player = event.getEntity();
        nbt = player.getPersistentData();
        level = player.level;
        checkIfIsFirstPlayerConnectionToWorld();
    }

    public static void register() {
        PlayerInit playerInit = new PlayerInit();
        MinecraftForge.EVENT_BUS.register(playerInit);
    }

    public static void sendMessage(String message) {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(COLOR_BLUE + COLOR_RED));
        Component messageToDisplay = Component.nullToEmpty(message).copy().withStyle(style);
        player.sendSystemMessage(messageToDisplay);
    }

    public static void sendMessage(String message, Style style) {
        Component messageToDisplay = Component.nullToEmpty(message).copy().withStyle(style);
        player.displayClientMessage(messageToDisplay, true);
    }

    public static String getPlayerName() {
        return player.getName().getString();
    }

    public static Player getPlayer() {
        return player;
    }

    public static Level getLevel() {
        return level;
    }

    private void checkIfIsFirstPlayerConnectionToWorld() {
        if(nbt.contains("villagefinder")) {
            sendMessage("Welcome back " + getPlayerName() + " Mod Village Finder Load");
        } else {
            sendMessage("Welcome " + getPlayerName() + " Mod Village Finder Load");
            nbt.putBoolean("villagefinder", true);
            Utils.GiveCompass();
        }
    }
}
