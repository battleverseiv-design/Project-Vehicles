package com.evgen.cardecor.pack;

import com.google.gson.annotations.SerializedName;

public class SeatDefinition {
    public String id = "default_seat";
    public String name = "Стандартное автомобильное сиденье";
    public String type = "seat"; // "seat", "bucket", "bench", "leather"
    public String author = "CarDecor";
    public transient java.io.File packDirectory = null;

    @SerializedName(value = "sitting_height_offset", alternate = {"sittingHeightOffset", "height_offset"})
    public float sittingHeightOffset = 0.0F; // vertical adjustment for player ride pos

    @SerializedName(value = "comfort_factor", alternate = {"comfortFactor", "comfort"})
    public double comfortFactor = 1.0; // softens camera vibration

    @SerializedName(value = "mass_kg", alternate = {"massKg", "mass"})
    public double massKg = 12.0;

    @SerializedName(value = "model", alternate = {"model_path", "modelPath"})
    public String model = null; // optional custom seat 3D model

    @SerializedName(value = "texture", alternate = {"texture_path", "texturePath"})
    public String texture = null;
}
