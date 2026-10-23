package com.evgen.cardecor.pack;

import com.google.gson.annotations.SerializedName;
import java.io.File;

public class TireDefinition {
    public String id = "tire_03_classic_radial";
    public String name = "Classic Radial (Ретро)";
    public String type = "classic"; // "offroad", "all_terrain", "classic", "muscle", "sport", "commercial"
    public String author = "CarDecor";

    @SerializedName(value = "radius", alternate = {"tire_radius", "wheel_radius"})
    public double radius = 0.38; // meters

    @SerializedName(value = "width", alternate = {"tire_width", "wheel_width"})
    public double width = 0.22; // meters

    @SerializedName(value = "grip_asphalt", alternate = {"gripAsphalt", "asphalt_grip"})
    public double gripAsphalt = 1.0;

    @SerializedName(value = "grip_dirt", alternate = {"gripDirt", "dirt_grip", "offroad_grip"})
    public double gripDirt = 0.85;

    @SerializedName(value = "grip_ice", alternate = {"gripIce", "ice_grip"})
    public double gripIce = 0.25;

    @SerializedName(value = "mass_kg", alternate = {"massKg", "mass", "weight_kg"})
    public double massKg = 10.0;

    @SerializedName(value = "durability_hp", alternate = {"durabilityHp", "tire_hp", "hp"})
    public double durabilityHp = 150.0;

    @SerializedName(value = "model", alternate = {"model_path", "modelPath"})
    public String model = "models/wheels/tires/tire_03_classic_radial.obj";

    @SerializedName(value = "texture", alternate = {"texture_path", "texturePath"})
    public String texture = "textures/wheel_palette.png";

    public transient File packDirectory = null;
}
