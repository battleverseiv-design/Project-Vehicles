package com.evgen.cardecor.pack;

import com.google.gson.annotations.SerializedName;

public class WheelDefinition {
    public String id = "default_wheel";
    public String name = "Стандартное колесо";
    public String type = "wheel"; // "wheel", "offroad", "sport", "truck"
    public String author = "CarDecor";
    public transient java.io.File packDirectory = null;

    @SerializedName(value = "radius", alternate = {"wheel_radius", "wheelRadius"})
    public double radius = 0.38; // meters

    @SerializedName(value = "width", alternate = {"wheel_width", "wheelWidth"})
    public double width = 0.22; // meters

    @SerializedName(value = "grip_asphalt", alternate = {"gripAsphalt", "asphalt_grip"})
    public double gripAsphalt = 1.0;

    @SerializedName(value = "grip_dirt", alternate = {"gripDirt", "dirt_grip", "offroad_grip"})
    public double gripDirt = 0.85;

    @SerializedName(value = "grip_ice", alternate = {"gripIce", "ice_grip"})
    public double gripIce = 0.25;

    @SerializedName(value = "mass_kg", alternate = {"massKg", "mass", "weight_kg"})
    public double massKg = 18.0;

    @SerializedName(value = "durability_hp", alternate = {"durabilityHp", "wheel_hp", "hp"})
    public double durabilityHp = 150.0;

    @SerializedName(value = "model", alternate = {"model_path", "modelPath"})
    public String model = "models/wheel.obj";

    @SerializedName(value = "texture", alternate = {"texture_path", "texturePath"})
    public String texture = null;
}
