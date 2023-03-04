package com.anthonincolas.villagefinder.Mixin;

import com.anthonincolas.villagefinder.Init.ItemInit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    public abstract float getYRot();

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci){
        ItemStack villageCompass = ((Player)(Object)this).getInventory().getSelected();
        if(villageCompass.getItem() == ItemInit.VILLAGE_COMPASS.get()) {
            float angle = ((getYRot() % 360) + 360) % 360;
            villageCompass.getOrCreateTag().putFloat("angle", angle);
        }
    }
}
