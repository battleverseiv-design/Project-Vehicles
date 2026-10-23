package com.evgen.cardecor.pack;

import com.google.gson.annotations.SerializedName;
import java.io.File;

public class RimDefinition {
    public String id = "rim_04_stamped_steelie";
    public String name = "Стальная штамповка";
    public String author = "CarDecor";

    @SerializedName(value = "deep_dish_offset", alternate = {"deepDishOffset", "offset"})
    public double deepDishOffset = 0.0; // meters

    @SerializedName(value = "mass_kg", alternate = {"massKg", "mass", "weight_kg"})
    public double massKg = 8.0;

    @SerializedName(value = "model", alternate = {"model_path", "modelPath"})
    public String model = "models/wheels/rims/rim_04_stamped_steelie.obj";

    @SerializedName(value = "texture", alternate = {"texture_path", "texturePath"})
    public String texture = "textures/wheel_palette.png";

    public transient File packDirectory = null;
}
