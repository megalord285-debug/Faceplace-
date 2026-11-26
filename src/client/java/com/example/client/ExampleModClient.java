package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {

    static boolean aimEnabled = false;
    static boolean fastPlaceEnabled = false;
    static KeyMapping aimKey;
    static KeyMapping fastPlaceKey;

    static final double AIM_RANGE = 3.2;
    static final float SMOOTH = 0.6f;
    static final float MAX_STEP = 45f;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category cat = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("aimassist", "main"));

        aimKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aimassist.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, cat));
        fastPlaceKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.fastplace.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, cat));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (aimKey.consumeClick()) {
                aimEnabled = !aimEnabled;
                msg(client, "AimAssist: " + (aimEnabled ? "ON" : "OFF"));
            }
            while (fastPlaceKey.consumeClick()) {
                fastPlaceEnabled = !fastPlaceEnabled;
                msg(client, "FastPlace: " + (fastPlaceEnabled ? "ON" : "OFF"));
            }

            if (client.player == null || client.level == null
                    || client.screen != null || client.gameMode == null) return;

            LocalPlayer me = client.player;

            if (aimEnabled) {
                Player t = nearest(client, me, AIM_RANGE);
                if (t != null) aim(me, t);
            }

            if (fastPlaceEnabled && client.options.keyUse.isDown()
                    && me.getMainHandItem().getItem() instanceof BlockItem
                    && client.hitResult != null
                    && client.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult hit = (BlockHitResult) client.hitResult;
                client.gameMode.useItemOn(me, InteractionHand.MAIN_HAND, hit);
                me.swing(InteractionHand.MAIN_HAND);
            }
        });
    }

    static void msg(Minecraft client, String text) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(text), true);
        }
    }

    static Player nearest(Minecraft client, LocalPlayer me, double range) {
        Player best = null;
        double bestDist = range;
        for (Player p : client.level.players()) {
            if (p == me || !p.isAlive() || p.isSpectator()) continue;
            double dist = me.distanceTo(p);
            if (dist <= bestDist) { bestDist = dist; best = p; }
        }
        return best;
    }

    static void aim(LocalPlayer me, Player target) {
        Vec3 aim = target.getBoundingBox().getCenter();
        Vec3 d = aim.subtract(me.getEyePosition());

        float tYaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float tPitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));

        float dYaw = Mth.wrapDegrees(tYaw - me.getYRot());
        float dPitch = tPitch - me.getXRot();

        me.setYRot(me.getYRot() + Mth.clamp(dYaw * SMOOTH, -MAX_STEP, MAX_STEP));
        me.setXRot(Mth.clamp(
                me.getXRot() + Mth.clamp(dPitch * SMOOTH, -MAX_STEP, MAX_STEP),
                -90f, 90f));
    }
				}
