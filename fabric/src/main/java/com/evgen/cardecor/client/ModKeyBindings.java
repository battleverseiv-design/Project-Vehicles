package com.evgen.cardecor.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final String KEY_CATEGORY = "key.categories.cardecor";

    public static final KeyMapping KEY_START_ENGINE = new KeyMapping(
        "key.cardecor.start_engine",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_HEADLIGHTS = new KeyMapping(
        "key.cardecor.headlights",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_L,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_SHIFT_UP = new KeyMapping(
        "key.cardecor.shift_up",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_SHIFT_DOWN = new KeyMapping(
        "key.cardecor.shift_down",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_F,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_CLUTCH = new KeyMapping(
        "key.cardecor.clutch",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_ALT,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_CABIN_LIGHT = new KeyMapping(
        "key.cardecor.cabin_light",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_I,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_HORN = new KeyMapping(
        "key.cardecor.horn",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_BOOST = new KeyMapping(
        "key.cardecor.boost",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_B,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_SWITCH_SEAT = new KeyMapping(
        "key.cardecor.switch_seat",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_X,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_HANDBRAKE = new KeyMapping(
        "key.cardecor.handbrake",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_SPACE,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_PARKING_BRAKE = new KeyMapping(
        "key.cardecor.parking_brake",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_P,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_DISMOUNT = new KeyMapping(
        "key.cardecor.dismount",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_U,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_SOUND_SETTINGS = new KeyMapping(
        "key.cardecor.sound_settings",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_F8,
        KEY_CATEGORY
    );

    public static final KeyMapping KEY_RADIAL_MENU = new KeyMapping(
        "key.cardecor.radial_menu",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        KEY_CATEGORY
    );
}
