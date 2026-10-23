package com.evgen.cardecor.pack;

import com.google.gson.annotations.SerializedName;
import java.io.File;

public class BrakeDefinition {
    public String id = "brake_02_stock_vented";
    public String name = "Заводской вентилируемый ротор";
    public String type = "vented"; // "drum", "vented", "sport", "ceramic", "commercial"
    public String author = "CarDecor";

    @SerializedName(value = "brake_force_mult", alternate = {"brakeForceMult", "power", "brakeForce"})
    public double brakeForceMult = 1.0;

    @SerializedName(value = "fade_resistance", alternate = {"fadeResistance", "heatResistance"})
    public double fadeResistance = 1.0;

    @SerializedName(value = "is_drum", alternate = {"isDrum", "drum"})
    public boolean isDrum = false;

    @SerializedName(value = "mass_kg", alternate = {"massKg", "mass", "weight_kg"})
    public double massKg = 6.0;

    @SerializedName(value = "model", alternate = {"model_path", "modelPath"})
    public String model = "models/wheels/brakes/brake_02_stock_vented.obj";

    @SerializedName(value = "texture", alternate = {"texture_path", "texturePath"})
    public String texture = "textures/wheel_palette.png";

    public transient File packDirectory = null;
}
