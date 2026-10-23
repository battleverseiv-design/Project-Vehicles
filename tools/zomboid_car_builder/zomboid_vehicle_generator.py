#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Project Zomboid Vehicle Studio & 3D Model Generator for CarDecor (Project Vehicles).
Generates high-fidelity low-poly retro 80s/90s American vehicles matching Project Zomboid:
- 5-point curved wheel arches with deep 3D inner tubs
- Automotive sheet-metal A/B/C pillars with aerodynamic tumblehome
- Inset smoky glass and side-view mirrors
- Tapered hoods and wrap-around corner bumpers
- 16-segment stamped steelie wheels with cooling slots and chrome center caps
- Authentic Knox County police gear: PIT push bumper, A-pillar spotlight, dual-tone livery, roof lightbar
- Color-palette point UV shading (zero texture bleeding)
"""

import os
import sys
import math
import json
import argparse
from PIL import Image, ImageDraw

# ==============================================================================
# COLOR PALETTE MATRIX & POINT UV SYSTEM (8x8 Grid on 512x512 Canvas)
# ==============================================================================

SWATCH_GRID = {
    # Row 0: Exterior Primary & Accents
    "body_primary":    (0, 0),  # Main vehicle body paint
    "body_accent":     (1, 0),  # Secondary color (police doors, roof, stripes)
    "plastic_black":   (2, 0),  # Matte deep black/dark charcoal (trim, wells, mirrors)
    "chrome_silver":   (3, 0),  # Polished chrome (bumpers, door handles, hubcaps)
    "glass_tinted":    (4, 0),  # Deep smoky tinted glass
    "headlight_white": (5, 0),  # White headlight lens & reflectors
    "taillight_red":   (6, 0),  # Deep red brake/tail light lens
    "indicator_amber": (7, 0),  # Amber turn signal lens

    # Row 1: Interior, Wheels & Emergency Gear
    "interior_dash":   (0, 1),  # Dashboard dark vinyl
    "interior_seat":   (1, 1),  # Classic seat cloth/vinyl
    "steering_col":    (2, 1),  # Steering wheel rim & column
    "tire_rubber":     (3, 1),  # Heavy matte charcoal tire rubber
    "rim_steel":       (4, 1),  # Stamped steel wheel rim
    "rim_center":      (5, 1),  # Center hubcap / lug bolts
    "siren_blue":      (6, 1),  # Police emergency blue beacon
    "siren_red":       (7, 1),  # Police emergency red beacon
}

PALETTES = {
    "police": {
        "body": (245, 245, 250),        # Knox County White
        "accent": (24, 26, 34),          # Knox County Sheriff Black Doors
        "interior": (48, 50, 56),
    },
    "forest_green": {
        "body": (48, 72, 46),           # Pine Green
        "accent": (28, 38, 26),          # Dark Spruce
        "interior": (62, 54, 46),
    },
    "cherry_red": {
        "body": (155, 32, 32),          # Retro Maroon / Cherry
        "accent": (92, 18, 18),          # Dark Crimson
        "interior": (48, 48, 52),
    },
    "worn_blue": {
        "body": (65, 96, 138),          # Kentucky Sky Blue
        "accent": (38, 56, 82),          # Navy Shadow
        "interior": (52, 52, 56),
    },
    "desert_tan": {
        "body": (175, 154, 122),        # Mojave Tan
        "accent": (112, 96, 74),         # Army Khaki
        "interior": (68, 58, 48),
    },
    "matte_black": {
        "body": (32, 34, 38),           # Midnight Black
        "accent": (20, 22, 24),          # Deep Shadow
        "interior": (38, 38, 42),
    },
    "taxi_yellow": {
        "body": (230, 182, 38),         # Louisville Metro Yellow
        "accent": (26, 26, 28),          # Checker Black
        "interior": (48, 48, 52),
    },
    "rust_orange": {
        "body": (152, 74, 42),          # Oxide Rust
        "accent": (86, 42, 28),          # Burnt Umber
        "interior": (56, 48, 40),
    },
    "ambulance": {
        "body": (242, 242, 242),        # Emergency Medical White
        "accent": (198, 28, 28),         # Paramedic Red
        "interior": (50, 52, 56),
    },
}

def compute_uv_coords():
    coords = {}
    for name, (col, row) in SWATCH_GRID.items():
        u = (col + 0.5) / 8.0
        v = (row + 0.5) / 8.0
        coords[name] = (u, v)
    return coords

UV_COORDS = compute_uv_coords()

def generate_zomboid_texture(filepath, palette_name="worn_blue"):
    pal = PALETTES.get(palette_name, PALETTES["worn_blue"])
    img = Image.new("RGBA", (512, 512), (20, 20, 22, 255))
    draw = ImageDraw.Draw(img)

    colors = {
        "body_primary":    (*pal["body"], 255),
        "body_accent":     (*pal["accent"], 255),
        "plastic_black":   (24, 26, 30, 255),
        "chrome_silver":   (215, 220, 225, 255),
        "glass_tinted":    (28, 40, 52, 235),
        "headlight_white": (252, 252, 246, 255),
        "taillight_red":   (198, 22, 22, 255),
        "indicator_amber": (248, 146, 16, 255),

        "interior_dash":   (*pal["interior"], 255),
        "interior_seat":   (pal["interior"][0] + 16, pal["interior"][1] + 16, pal["interior"][2] + 16, 255),
        "steering_col":    (36, 38, 42, 255),
        "tire_rubber":     (30, 32, 34, 255),
        "rim_steel":       (175, 180, 186, 255),
        "rim_center":      (52, 56, 62, 255),
        "siren_blue":      (26, 88, 230, 255),
        "siren_red":       (228, 24, 24, 255),
    }

    block_size = 64
    for name, (col, row) in SWATCH_GRID.items():
        c = colors.get(name, (128, 128, 128, 255))
        x1 = col * block_size
        y1 = row * block_size
        x2 = x1 + block_size - 1
        y2 = y1 + block_size - 1
        draw.rectangle([x1, y1, x2, y2], fill=c)

    os.makedirs(os.path.dirname(os.path.abspath(filepath)), exist_ok=True)
    img.save(filepath, "PNG")


# ==============================================================================
# OBJ BUILDER WITH POINT-UV PALETTE MAPPING
# ==============================================================================

class OBJBuilder:
    def __init__(self, uv_map=None):
        self.vertices = []
        self.normals = []
        self.uvs = []
        self.uv_indices = {}
        self.groups = {}
        self.current_group = "chassis"

        uvs = uv_map or UV_COORDS
        for name, (u, v) in uvs.items():
            self.uvs.append((round(u, 4), round(v, 4)))
            self.uv_indices[name] = len(self.uvs)

    def set_group(self, group_name):
        self.current_group = group_name
        if group_name not in self.groups:
            self.groups[group_name] = []

    def get_uv(self, swatch):
        return self.uv_indices.get(swatch, 1)

    def add_vertex(self, x, y, z):
        self.vertices.append((round(x, 4), round(y, 4), round(z, 4)))
        return len(self.vertices)

    def add_normal(self, nx, ny, nz):
        l = math.sqrt(nx*nx + ny*ny + nz*nz)
        if l < 1e-6:
            nx, ny, nz = 0.0, 1.0, 0.0
        else:
            nx, ny, nz = nx/l, ny/l, nz/l
        self.normals.append((round(nx, 4), round(ny, 4), round(nz, 4)))
        return len(self.normals)

    def add_triangle(self, v1, v2, v3, swatch, n1, n2=None, n3=None):
        if n2 is None: n2 = n1
        if n3 is None: n3 = n1
        uv_idx = self.get_uv(swatch)
        if self.current_group not in self.groups:
            self.groups[self.current_group] = []
        self.groups[self.current_group].append(((v1, uv_idx, n1), (v2, uv_idx, n2), (v3, uv_idx, n3)))

    def add_quad(self, v1, v2, v3, v4, swatch, n):
        self.add_triangle(v1, v2, v3, swatch, n, n, n)
        self.add_triangle(v1, v3, v4, swatch, n, n, n)

    def add_box(self, x1, y1, z1, x2, y2, z2, swatch="body_primary", group=None,
                top_swatch=None, bot_swatch=None, fwd_swatch=None, back_swatch=None,
                left_swatch=None, right_swatch=None):
        if group:
            self.set_group(group)

        c1 = self.add_vertex(x1, y1, z1)
        c2 = self.add_vertex(x2, y1, z1)
        c3 = self.add_vertex(x2, y2, z1)
        c4 = self.add_vertex(x1, y2, z1)
        c5 = self.add_vertex(x1, y1, z2)
        c6 = self.add_vertex(x2, y1, z2)
        c7 = self.add_vertex(x2, y2, z2)
        c8 = self.add_vertex(x1, y2, z2)

        n_back   = self.add_normal( 0,  0, -1)
        n_front  = self.add_normal( 0,  0,  1)
        n_left   = self.add_normal(-1,  0,  0)
        n_right  = self.add_normal( 1,  0,  0)
        n_top    = self.add_normal( 0,  1,  0)
        n_bottom = self.add_normal( 0, -1,  0)

        # Front (+Z)
        self.add_quad(c5, c6, c7, c8, fwd_swatch or swatch, n_front)
        # Back (-Z)
        self.add_quad(c2, c1, c4, c3, back_swatch or swatch, n_back)
        # Left (-X)
        self.add_quad(c1, c5, c8, c4, left_swatch or swatch, n_left)
        # Right (+X)
        self.add_quad(c6, c2, c3, c7, right_swatch or swatch, n_right)
        # Top (+Y)
        self.add_quad(c4, c8, c7, c3, top_swatch or swatch, n_top)
        # Bottom (-Y)
        self.add_quad(c5, c1, c2, c6, bot_swatch or swatch, n_bottom)

    def add_cylinder(self, p1, p2, radius, segments, swatch="plastic_black", group=None):
        if group:
            self.set_group(group)
        x1, y1, z1 = p1
        x2, y2, z2 = p2
        dx, dy, dz = x2 - x1, y2 - y1, z2 - z1
        length = math.sqrt(dx*dx + dy*dy + dz*dz)
        if length < 1e-5:
            return

        fx, fy, fz = dx/length, dy/length, dz/length
        if abs(fy) < 0.99:
            rx, ry, rz = fz, 0, -fx
        else:
            rx, ry, rz = 0, fz, -fy
        rl = math.sqrt(rx*rx + ry*ry + rz*rz)
        rx, ry, rz = rx/rl, ry/rl, rz/rl
        ux, uy, uz = fy*rz - fz*ry, fz*rx - fx*rz, fx*ry - fy*rx

        v_start = []
        v_end = []
        n_indices = []

        for i in range(segments):
            ang = 2.0 * math.pi * i / segments
            c = math.cos(ang) * radius
            s = math.sin(ang) * radius
            nx, ny, nz = rx * c + ux * s, ry * c + uy * s, rz * c + uz * s
            norm_idx = self.add_normal(nx, ny, nz)
            n_indices.append(norm_idx)

            v_start.append(self.add_vertex(x1 + nx, y1 + ny, z1 + nz))
            v_end.append(self.add_vertex(x2 + nx, y2 + ny, z2 + nz))

        for i in range(segments):
            next_i = (i + 1) % segments
            vs1 = v_start[i]
            ve1 = v_end[i]
            vs2 = v_start[next_i]
            ve2 = v_end[next_i]
            n1 = n_indices[i]
            n2 = n_indices[next_i]

            self.add_triangle(vs1, ve1, ve2, swatch, n1, n1, n2)
            self.add_triangle(vs1, ve2, vs2, swatch, n1, n2, n2)

    def write_obj(self, filepath):
        os.makedirs(os.path.dirname(os.path.abspath(filepath)), exist_ok=True)
        with open(filepath, "w", encoding="utf-8") as f:
            f.write("# Project Zomboid Vehicle Model\n")
            f.write("# Generated by CarDecor Zomboid Vehicle Studio\n\n")

            for v in self.vertices:
                f.write(f"v {v[0]:.4f} {v[1]:.4f} {v[2]:.4f}\n")
            f.write("\n")

            for vt in self.uvs:
                f.write(f"vt {vt[0]:.4f} {vt[1]:.4f}\n")
            f.write("\n")

            for vn in self.normals:
                f.write(f"vn {vn[0]:.4f} {vn[1]:.4f} {vn[2]:.4f}\n")
            f.write("\n")

            for group_name, faces in self.groups.items():
                if not faces:
                    continue
                f.write(f"o {group_name}\n")
                for tri in faces:
                    f1, f2, f3 = tri
                    f.write(f"f {f1[0]}/{f1[1]}/{f1[2]} {f2[0]}/{f2[1]}/{f2[2]} {f3[0]}/{f3[1]}/{f3[2]}\n")
                f.write("\n")


# ==============================================================================
# 16-SEGMENT AUTHENTIC STAMPED STEELIE WHEEL (ZOMBOID QUALITY)
# ==============================================================================

def generate_zomboid_wheel(filepath):
    """
    Builds a high-detail 16-segment retro steelie wheel:
    Thick matte charcoal tire tread + curved sidewall bevels +
    recessed stamped rim dish with 8 cooling slots + chrome dome center cap.
    """
    b = OBJBuilder()
    b.set_group("wheel")

    segments = 16
    radius = 0.38
    half_width = 0.11

    x_in = -half_width
    x_out = half_width
    rim_r = 0.23
    hub_r = 0.085
    rim_recess_x = 0.05

    angles = [2.0 * math.pi * i / segments for i in range(segments)]
    cos_s = [math.cos(a) for a in angles]
    sin_s = [math.sin(a) for a in angles]

    # Outer tire rings
    v_tread_in = [b.add_vertex(x_in, cos_s[i]*radius, sin_s[i]*radius) for i in range(segments)]
    v_tread_out = [b.add_vertex(x_out, cos_s[i]*radius, sin_s[i]*radius) for i in range(segments)]
    v_side_out = [b.add_vertex(x_out, cos_s[i]*rim_r, sin_s[i]*rim_r) for i in range(segments)]
    v_side_in = [b.add_vertex(x_in, cos_s[i]*rim_r, sin_s[i]*rim_r) for i in range(segments)]

    # Recessed steel rim dish
    v_dish_rim = [b.add_vertex(rim_recess_x, cos_s[i]*rim_r, sin_s[i]*rim_r) for i in range(segments)]
    v_dish_hub = [b.add_vertex(rim_recess_x, cos_s[i]*hub_r, sin_s[i]*hub_r) for i in range(segments)]

    # Chrome center dome hubcap
    v_cap_base = [b.add_vertex(x_out - 0.02, cos_s[i]*hub_r, sin_s[i]*hub_r) for i in range(segments)]
    v_cap_tip = b.add_vertex(x_out + 0.025, 0, 0)
    v_back_center = b.add_vertex(x_in, 0, 0)

    n_out = b.add_normal(1, 0, 0)
    n_in = b.add_normal(-1, 0, 0)

    for i in range(segments):
        nxt = (i + 1) % segments
        c1, s1 = cos_s[i], sin_s[i]
        c2, s2 = cos_s[nxt], sin_s[nxt]

        n1 = b.add_normal(0, c1, s1)
        n2 = b.add_normal(0, c2, s2)

        # 1. Outer Tread
        b.add_triangle(v_tread_in[i], v_tread_out[i], v_tread_out[nxt], "tire_rubber", n1, n1, n2)
        b.add_triangle(v_tread_in[i], v_tread_out[nxt], v_tread_in[nxt], "tire_rubber", n1, n2, n2)

        # 2. Outer Sidewall (facing camera)
        b.add_triangle(v_tread_out[i], v_side_out[i], v_side_out[nxt], "tire_rubber", n_out, n_out, n_out)
        b.add_triangle(v_tread_out[i], v_side_out[nxt], v_tread_out[nxt], "tire_rubber", n_out, n_out, n_out)

        # 3. Inner Sidewall (facing frame)
        b.add_triangle(v_tread_in[i], v_tread_in[nxt], v_side_in[nxt], "tire_rubber", n_in, n_in, n_in)
        b.add_triangle(v_tread_in[i], v_side_in[nxt], v_side_in[i], "tire_rubber", n_in, n_in, n_in)

        # 4. Inward Rim Flange (into dish)
        n_flange = b.add_normal(-0.5, c1*0.86, s1*0.86)
        b.add_triangle(v_side_out[i], v_dish_rim[i], v_dish_rim[nxt], "rim_steel", n_flange, n_flange, n_flange)
        b.add_triangle(v_side_out[i], v_dish_rim[nxt], v_side_out[nxt], "rim_steel", n_flange, n_flange, n_flange)

        # 5. Stamped Steel Rim Dish with 8 cooling slots (alternating black slots)
        dish_swatch = "plastic_black" if (i % 2 == 1) else "rim_steel"
        b.add_triangle(v_dish_rim[i], v_dish_hub[i], v_dish_hub[nxt], dish_swatch, n_out, n_out, n_out)
        b.add_triangle(v_dish_rim[i], v_dish_hub[nxt], v_dish_rim[nxt], dish_swatch, n_out, n_out, n_out)

        # 6. Center Hubcap Cylinder & Chrome Cap
        n_hub_ring = b.add_normal(0.4, c1*0.9, s1*0.9)
        b.add_triangle(v_dish_hub[i], v_cap_base[i], v_cap_base[nxt], "rim_center", n_hub_ring, n_hub_ring, n_hub_ring)
        b.add_triangle(v_dish_hub[i], v_cap_base[nxt], v_dish_hub[nxt], "rim_center", n_hub_ring, n_hub_ring, n_hub_ring)

        b.add_triangle(v_cap_base[i], v_cap_tip, v_cap_base[nxt], "chrome_silver", n_out, n_out, n_out)

        # 7. Inner Hub Plate
        b.add_triangle(v_side_in[i], v_side_in[nxt], v_back_center, "plastic_black", n_in, n_in, n_in)

    b.write_obj(filepath)


# ==============================================================================
# AUTHENTIC 80s/90s STEERING WHEEL
# ==============================================================================

def generate_steering_wheel(filepath):
    b = OBJBuilder()
    b.set_group("steering_wheel")

    radius = 0.20
    tube_r = 0.02
    segments = 12

    for i in range(segments):
        ang1 = 2.0 * math.pi * i / segments
        ang2 = 2.0 * math.pi * (i + 1) / segments
        p1 = (math.cos(ang1) * radius, math.sin(ang1) * radius, 0)
        p2 = (math.cos(ang2) * radius, math.sin(ang2) * radius, 0)
        b.add_cylinder(p1, p2, tube_r, 6, "steering_col", "steering_wheel")

    # Center Horn Pad
    b.add_box(-0.06, -0.05, -0.015, 0.06, 0.05, 0.015, "interior_dash", "steering_wheel")

    # Horizontal Spokes
    b.add_box(-radius + 0.02, -0.025, -0.01, -0.04, 0.025, 0.01, "steering_col", "steering_wheel")
    b.add_box(0.04, -0.025, -0.01, radius - 0.02, 0.025, 0.01, "steering_col", "steering_wheel")

    b.write_obj(filepath)


# ==============================================================================
# 5-POINT CURVED WHEEL ARCH CONSTRUCTOR
# ==============================================================================

def add_wheel_arch_fender(builder, sign, cz, arch_r, z_front, z_rear, half_w, chassis_bot_y, hood_y, swatch="body_primary", group="chassis_base"):
    """
    Builds an authentic 5-point curved wheel arch cutout with outward lip
    and deep 3D black inner wheel liner tub.
    sign = +1 for Left fender (+X), -1 for Right fender (-X).
    """
    builder.set_group(group)
    x_outer = sign * half_w
    x_inner = sign * (half_w - 0.18)
    n_outer = builder.add_normal(sign, 0, 0)
    n_tub = builder.add_normal(0, 1, 0)

    cy = 0.38
    # 5 points along circular arch curve
    arch_pts = [
        (chassis_bot_y, cz + arch_r),                                               # P0: bottom fwd
        (cy + arch_r * math.sin(math.radians(35)), cz + arch_r * math.cos(math.radians(35))), # P1: diag fwd
        (cy + arch_r, cz),                                                         # P2: apex top
        (cy + arch_r * math.sin(math.radians(35)), cz - arch_r * math.cos(math.radians(35))), # P3: diag rear
        (chassis_bot_y, cz - arch_r),                                               # P4: bottom rear
    ]

    # Outer arch vertices
    v_arch_out = [builder.add_vertex(x_outer, pt[0], pt[1]) for pt in arch_pts]
    # Inner wheel well tub vertices
    v_arch_in  = [builder.add_vertex(x_inner, pt[0], pt[1]) for pt in arch_pts]

    # Fender outer bounding vertices
    vf_top_fwd  = builder.add_vertex(x_outer, hood_y, z_front)
    vf_bot_fwd  = builder.add_vertex(x_outer, chassis_bot_y, z_front)
    vf_top_afwd = builder.add_vertex(x_outer, hood_y, cz + arch_r)
    vf_top_mid  = builder.add_vertex(x_outer, hood_y, cz)
    vf_top_arear= builder.add_vertex(x_outer, hood_y, cz - arch_r)
    vf_top_rear = builder.add_vertex(x_outer, hood_y, z_rear)
    vf_bot_rear = builder.add_vertex(x_outer, chassis_bot_y, z_rear)

    # Helper to add quad oriented according to sign
    def add_fender_quad(v1, v2, v3, v4, col):
        if sign > 0:
            builder.add_quad(v1, v2, v3, v4, col, n_outer)
        else:
            builder.add_quad(v4, v3, v2, v1, col, n_outer)

    def add_fender_tri(v1, v2, v3, col):
        if sign > 0:
            builder.add_triangle(v1, v2, v3, col, n_outer)
        else:
            builder.add_triangle(v3, v2, v1, col, n_outer)

    # 1. Front solid fender block ahead of arch
    add_fender_quad(vf_bot_fwd, v_arch_out[0], vf_top_afwd, vf_top_fwd, swatch)

    # 2. Rear solid fender block behind arch
    add_fender_quad(v_arch_out[4], vf_bot_rear, vf_top_rear, vf_top_arear, swatch)

    # 3. Crown above the curved arch
    add_fender_tri(v_arch_out[0], v_arch_out[1], vf_top_afwd, swatch)
    add_fender_quad(v_arch_out[1], v_arch_out[2], vf_top_mid, vf_top_afwd, swatch)
    add_fender_quad(v_arch_out[2], v_arch_out[3], vf_top_arear, vf_top_mid, swatch)
    add_fender_tri(v_arch_out[3], v_arch_out[4], vf_top_arear, swatch)

    # 4. Deep 3D Black Inner Wheel Tub (prevents see-through)
    for i in range(4):
        p_o1 = v_arch_out[i]
        p_o2 = v_arch_out[i+1]
        p_i1 = v_arch_in[i]
        p_i2 = v_arch_in[i+1]
        if sign > 0:
            builder.add_quad(p_o1, p_i1, p_i2, p_o2, "plastic_black", n_tub)
        else:
            builder.add_quad(p_o2, p_i2, p_i1, p_o1, "plastic_black", n_tub)


# ==============================================================================
# HIGH-FIDELITY PROJECT ZOMBOID VEHICLE BUILDER
# ==============================================================================

def build_zomboid_vehicle(vehicle_type="sedan_90s", has_bullbar=False, has_roof_rack=False, has_window_mesh=False, palette="worn_blue"):
    b = OBJBuilder()

    is_police = (vehicle_type == "police_cruiser")
    is_pickup = (vehicle_type == "pickup_utility")
    is_suv = (vehicle_type == "suv_offroad")
    is_muscle = (vehicle_type == "muscle_classic")

    half_w = 0.92
    fwd_z = 1.45
    rear_z = -1.45
    wheelbase = fwd_z - rear_z # 2.90m
    front_bumper_z = 2.26
    rear_bumper_z = -2.34

    chassis_bot_y = 0.28
    hood_y = 0.82
    roof_y = 1.40
    roof_w = 0.78 # Aerodynamic inward taper

    if is_pickup:
        hood_y = 0.86
        roof_y = 1.46
        cab_front_z = 0.60
        cab_rear_z = -0.40
    elif is_suv:
        hood_y = 0.88
        roof_y = 1.54
        roof_w = 0.82
        cab_front_z = 0.60
        cab_rear_z = -1.80
    elif is_muscle:
        hood_y = 0.74
        roof_y = 1.28
        roof_w = 0.74
        fwd_z = 1.35
        cab_front_z = 0.40
        cab_rear_z = -0.75
    else: # Sedan 90s & Police Cruiser
        cab_front_z = 0.65
        cab_rear_z = -0.85

    body_main = "body_primary"
    # Police Cruiser has black doors (classic Knox County black-and-white)
    door_color = "body_accent" if is_police else "body_primary"

    # --------------------------------------------------------------------------
    # 1. UNDERBODY FLOORPAN & SILLS
    # --------------------------------------------------------------------------
    b.add_box(-half_w * 0.85, chassis_bot_y, rear_bumper_z + 0.15, half_w * 0.85, chassis_bot_y + 0.08, front_bumper_z - 0.15, "plastic_black", "chassis_base")

    # Rocker panel sill (between front and rear wheel arches)
    arch_r = 0.41
    sill_front_z = fwd_z - arch_r
    sill_rear_z = rear_z + arch_r
    b.add_box(-half_w, chassis_bot_y, sill_rear_z, -half_w + 0.06, chassis_bot_y + 0.12, sill_front_z, "plastic_black", "chassis_base")
    b.add_box( half_w - 0.06, chassis_bot_y, sill_rear_z,  half_w, chassis_bot_y + 0.12, sill_front_z, "plastic_black", "chassis_base")

    # --------------------------------------------------------------------------
    # 2. 5-POINT CURVED WHEEL ARCHES (FRONT & REAR, LEFT & RIGHT)
    # --------------------------------------------------------------------------
    # Left & Right Front Wheel Arches
    add_wheel_arch_fender(b, +1, fwd_z, arch_r, front_bumper_z - 0.08, cab_front_z, half_w, chassis_bot_y, hood_y, body_main, "chassis_base")
    add_wheel_arch_fender(b, -1, fwd_z, arch_r, front_bumper_z - 0.08, cab_front_z, half_w, chassis_bot_y, hood_y, body_main, "chassis_base")

    # Left & Right Rear Wheel Arches
    rear_fender_front_z = cab_rear_z if not is_suv else rear_z + arch_r
    add_wheel_arch_fender(b, +1, rear_z, arch_r, rear_fender_front_z, rear_bumper_z + 0.08, half_w, chassis_bot_y, hood_y, body_main, "chassis_base")
    add_wheel_arch_fender(b, -1, rear_z, arch_r, rear_fender_front_z, rear_bumper_z + 0.08, half_w, chassis_bot_y, hood_y, body_main, "chassis_base")

    # --------------------------------------------------------------------------
    # 3. WRAP-AROUND BEVELED BUMPERS WITH AIR DAM
    # --------------------------------------------------------------------------
    bumper_y = chassis_bot_y + 0.05
    bumper_h = 0.20
    b_thick = 0.14
    b_swatch = "chrome_silver" if not is_police else "plastic_black"

    # Front Main Impact Bar
    b.add_box(-half_w * 0.85, bumper_y, front_bumper_z - b_thick, half_w * 0.85, bumper_y + bumper_h, front_bumper_z, b_swatch, "bumpers")
    # Front Chamfered Corner Wraps (hugging the wings)
    b.add_box(-half_w - 0.02, bumper_y, front_bumper_z - b_thick - 0.14, -half_w * 0.85, bumper_y + bumper_h, front_bumper_z - 0.03, b_swatch, "bumpers")
    b.add_box( half_w * 0.85, bumper_y, front_bumper_z - b_thick - 0.14,  half_w + 0.02, bumper_y + bumper_h, front_bumper_z - 0.03, b_swatch, "bumpers")
    # Front Rubber Rub Strip
    b.add_box(-half_w * 0.82, bumper_y + 0.06, front_bumper_z, half_w * 0.82, bumper_y + 0.14, front_bumper_z + 0.015, "plastic_black", "bumpers")
    # Front Chin Spoiler (Lower Air Dam)
    b.add_box(-half_w * 0.82, bumper_y - 0.06, front_bumper_z - b_thick + 0.02, half_w * 0.82, bumper_y, front_bumper_z - 0.02, "plastic_black", "bumpers")

    # Rear Main Impact Bar
    b.add_box(-half_w * 0.85, bumper_y, rear_bumper_z, half_w * 0.85, bumper_y + bumper_h, rear_bumper_z + b_thick, b_swatch, "bumpers")
    # Rear Chamfered Corner Wraps
    b.add_box(-half_w - 0.02, bumper_y, rear_bumper_z + 0.03, -half_w * 0.85, bumper_y + bumper_h, rear_bumper_z + b_thick + 0.14, b_swatch, "bumpers")
    b.add_box( half_w * 0.85, bumper_y, rear_bumper_z + 0.03,  half_w + 0.02, bumper_y + bumper_h, rear_bumper_z + b_thick + 0.14, b_swatch, "bumpers")
    # Rear Rubber Rub Strip
    b.add_box(-half_w * 0.82, bumper_y + 0.06, rear_bumper_z - 0.015, half_w * 0.82, bumper_y + 0.14, rear_bumper_z, "plastic_black", "bumpers")

    # --------------------------------------------------------------------------
    # 4. FRONT FASCIA: RECESSED GRILLE & DUAL HEADLIGHTS
    # --------------------------------------------------------------------------
    front_face_z = front_bumper_z - 0.08
    grille_bot_y = bumper_y + bumper_h
    grille_top_y = hood_y - 0.02

    # Black Grille Mesh
    b.add_box(-0.42, grille_bot_y, front_face_z - 0.04, 0.42, grille_top_y, front_face_z, "plastic_black", "grille")
    # Horizontal Chrome Slats
    slat_y1 = grille_bot_y + (grille_top_y - grille_bot_y) * 0.35
    slat_y2 = grille_bot_y + (grille_top_y - grille_bot_y) * 0.70
    b.add_box(-0.40, slat_y1 - 0.01, front_face_z - 0.04, 0.40, slat_y1 + 0.01, front_face_z + 0.005, "chrome_silver", "grille")
    b.add_box(-0.40, slat_y2 - 0.01, front_face_z - 0.04, 0.40, slat_y2 + 0.01, front_face_z + 0.005, "chrome_silver", "grille")

    # Dual Headlights (White glass lens + Chrome bezel)
    hl_w = 0.28
    # Left Headlight
    b.add_box(-half_w * 0.80, grille_bot_y + 0.02, front_face_z - 0.02, -half_w * 0.80 + hl_w, grille_top_y - 0.01, front_face_z + 0.015, "headlight_white", "headlights")
    b.add_box(-half_w * 0.81, grille_bot_y + 0.01, front_face_z - 0.03, -half_w * 0.80 + hl_w + 0.01, grille_top_y, front_face_z - 0.02, "chrome_silver", "headlights")
    # Right Headlight
    b.add_box( half_w * 0.80 - hl_w, grille_bot_y + 0.02, front_face_z - 0.02,  half_w * 0.80, grille_top_y - 0.01, front_face_z + 0.015, "headlight_white", "headlights")
    b.add_box( half_w * 0.80 - hl_w - 0.01, grille_bot_y + 0.01, front_face_z - 0.03,  half_w * 0.81, grille_top_y, front_face_z - 0.02, "chrome_silver", "headlights")

    # Wrap-around Amber Turn Signals (on outer fender corners)
    b.add_box(-half_w * 0.94, grille_bot_y + 0.02, front_face_z - 0.03, -half_w * 0.81, grille_top_y - 0.01, front_face_z + 0.02, "indicator_amber", "headlights")
    b.add_box( half_w * 0.81, grille_bot_y + 0.02, front_face_z - 0.03,  half_w * 0.94, grille_top_y - 0.01, front_face_z + 0.02, "indicator_amber", "headlights")

    # Rear Taillight Clusters & License Plate Recess
    tl_z = rear_bumper_z + 0.08
    tl_w = 0.28
    tl_bot_y = bumper_y + bumper_h + 0.02
    tl_top_y = hood_y - 0.02
    b.add_box(-half_w * 0.80, tl_bot_y, tl_z - 0.02, -half_w * 0.80 + tl_w, tl_top_y, tl_z + 0.015, "taillight_red", "taillights")
    b.add_box( half_w * 0.80 - tl_w, tl_bot_y, tl_z - 0.02,  half_w * 0.80, tl_top_y, tl_z + 0.015, "taillight_red", "taillights")
    b.add_box(-0.25, tl_bot_y + 0.02, rear_bumper_z + 0.06, 0.25, tl_top_y - 0.02, rear_bumper_z + 0.10, "plastic_black", "taillights")

    # --------------------------------------------------------------------------
    # 5. TAPERED HOOD (ANIMATED LIFT)
    # --------------------------------------------------------------------------
    hood_w = half_w * 0.80
    b.set_group("hood")
    # Tapered downward slope from cowl to grille
    b.add_box(-hood_w, hood_y - 0.04, front_face_z - 0.02, hood_w, hood_y, cab_front_z, body_main, "hood")
    # Cowl / wiper tray panel between windshield and hood
    b.add_box(-hood_w, hood_y - 0.02, cab_front_z - 0.06, hood_w, hood_y + 0.01, cab_front_z, "plastic_black", "chassis_base")

    # --------------------------------------------------------------------------
    # 6. AUTOMOTIVE PILLARS, TUMBLEHOME ROOF & INSET GLASS
    # --------------------------------------------------------------------------
    windshield_top_z = cab_front_z - 0.50

    # Flat Sheet-Metal A-Pillars (Connecting fender to roof at 50-degree rake)
    b.set_group("chassis_roof")
    # Left A-Pillar
    b.add_box(-half_w, hood_y, cab_front_z - 0.08, -roof_w, roof_y, cab_front_z, body_main, "chassis_roof")
    # Right A-Pillar
    b.add_box(roof_w, hood_y, cab_front_z - 0.08, half_w, roof_y, cab_front_z, body_main, "chassis_roof")

    # Inset Windshield Glass
    b.set_group("glass_windshield")
    b.add_box(-roof_w * 0.94, hood_y + 0.02, cab_front_z - 0.04, roof_w * 0.94, roof_y - 0.02, windshield_top_z, "glass_tinted", "glass_windshield")

    if is_pickup:
        cab_back_z = cab_rear_z
        # Roof panel
        b.add_box(-roof_w, roof_y - 0.06, cab_back_z, roof_w, roof_y, windshield_top_z, body_main, "chassis_roof")
        # Rear cab wall & glass
        b.add_box(-roof_w * 0.95, hood_y, cab_back_z - 0.04, roof_w * 0.95, roof_y - 0.06, cab_back_z, body_main, "chassis_roof")
        b.add_box(-roof_w * 0.70, hood_y + 0.12, cab_back_z - 0.03, roof_w * 0.70, roof_y - 0.10, cab_back_z + 0.01, "glass_tinted", "glass_windows")
        # Cargo Bed
        bed_front_z = cab_back_z
        bed_rear_z = rear_bumper_z + 0.16
        b.add_box(-half_w, hood_y - 0.04, bed_rear_z, -half_w * 0.80, hood_y + 0.18, bed_front_z, body_main, "chassis_base")
        b.add_box( half_w * 0.80, hood_y - 0.04, bed_rear_z,  half_w, hood_y + 0.18, bed_front_z, body_main, "chassis_base")
        b.add_box(-half_w * 0.80, chassis_bot_y + 0.10, bed_rear_z, half_w * 0.80, chassis_bot_y + 0.14, bed_front_z, "plastic_black", "chassis_base")
        b.add_box(-half_w * 0.80, chassis_bot_y + 0.14, bed_rear_z - 0.05, half_w * 0.80, hood_y + 0.18, bed_rear_z, body_main, "trunk")

    elif is_suv:
        # Full Length SUV Roof & Rear Hatch
        b.add_box(-roof_w, roof_y - 0.06, cab_rear_z + 0.10, roof_w, roof_y, windshield_top_z, body_main, "chassis_roof")
        b.add_box(-roof_w * 0.95, hood_y, cab_rear_z - 0.08, roof_w * 0.95, roof_y, cab_rear_z + 0.12, body_main, "trunk")
        b.add_box(-roof_w * 0.75, hood_y + 0.10, cab_rear_z - 0.06, roof_w * 0.75, roof_y - 0.08, cab_rear_z, "glass_tinted", "trunk")

    else:
        # 4-Door Notchback Sedan (Nyala / Police Cruiser)
        c_pillar_top_z = cab_rear_z
        c_pillar_bot_z = cab_rear_z - 0.44
        trunk_end_z = rear_bumper_z + 0.12

        # Roof Panel with beveled drip rails
        b.add_box(-roof_w, roof_y - 0.06, c_pillar_top_z, roof_w, roof_y, windshield_top_z, body_main, "chassis_roof")

        # C-Pillars (Wide sheet-metal rear pillars)
        b.add_box(-half_w, hood_y, c_pillar_bot_z, -roof_w, roof_y, c_pillar_top_z, body_main, "chassis_roof")
        b.add_box( roof_w, hood_y, c_pillar_bot_z,  half_w, roof_y, c_pillar_top_z, body_main, "chassis_roof")

        # Inset Rear Window Glass
        b.add_box(-roof_w * 0.94, hood_y + 0.02, c_pillar_bot_z + 0.04, roof_w * 0.94, roof_y - 0.02, c_pillar_top_z, "glass_tinted", "glass_windshield")

        # Trunk Decklid
        b.add_box(-hood_w, hood_y - 0.04, trunk_end_z, hood_w, hood_y, c_pillar_bot_z, body_main, "trunk")

    # --------------------------------------------------------------------------
    # 7. MODULAR DOORS WITH INSET WINDOWS & SIDE MIRRORS
    # --------------------------------------------------------------------------
    door_front_z = cab_front_z
    door_split_z = (cab_front_z + cab_rear_z) * 0.5 if not is_pickup else cab_rear_z
    door_rear_z = cab_rear_z
    door_thick = 0.05

    # Front-Left Door (FL)
    b.add_box(-half_w, chassis_bot_y + 0.12, door_split_z, -half_w + door_thick, hood_y, door_front_z, door_color, "door_fl")
    # FL Window Frame & Tilted Inset Glass
    b.add_box(-half_w + 0.01, hood_y, door_split_z + 0.03, -roof_w + 0.01, roof_y - 0.04, door_front_z - 0.03, "glass_tinted", "door_fl")
    # FL Chrome Door Handle
    b.add_box(-half_w - 0.015, hood_y - 0.10, door_split_z + 0.08, -half_w, hood_y - 0.06, door_split_z + 0.22, "chrome_silver", "door_fl")

    # Front-Right Door (FR)
    b.add_box(half_w - door_thick, chassis_bot_y + 0.12, door_split_z, half_w, hood_y, door_front_z, door_color, "door_fr")
    # FR Window Frame & Tilted Inset Glass
    b.add_box(roof_w - 0.01, hood_y, door_split_z + 0.03, half_w - 0.01, roof_y - 0.04, door_front_z - 0.03, "glass_tinted", "door_fr")
    # FR Chrome Door Handle
    b.add_box(half_w, hood_y - 0.10, door_split_z + 0.08, half_w + 0.015, hood_y - 0.06, door_split_z + 0.22, "chrome_silver", "door_fr")

    # Side View Mirrors on Front Doors
    mirror_z = cab_front_z - 0.08
    mirror_y = hood_y + 0.03
    # Left Mirror (with chrome reflector facing rear)
    b.add_box(-half_w - 0.11, mirror_y, mirror_z - 0.08, -half_w + 0.01, mirror_y + 0.09, mirror_z, "plastic_black", "door_fl",
              back_swatch="chrome_silver")
    # Right Mirror (with chrome reflector facing rear)
    b.add_box( half_w - 0.01, mirror_y, mirror_z - 0.08,  half_w + 0.11, mirror_y + 0.09, mirror_z, "plastic_black", "door_fr",
              back_swatch="chrome_silver")

    # B-Pillar divider & Rear Doors for 4-Door versions
    if not is_pickup:
        # B-Pillars (Vertical roof pillar)
        b.add_box(-half_w + 0.01, hood_y, door_split_z - 0.04, -roof_w + 0.01, roof_y - 0.02, door_split_z + 0.04, "plastic_black", "chassis_roof")
        b.add_box( roof_w - 0.01, hood_y, door_split_z - 0.04,  half_w - 0.01, roof_y - 0.02, door_split_z + 0.04, "plastic_black", "chassis_roof")

        # Rear-Left Door (RL)
        b.add_box(-half_w, chassis_bot_y + 0.12, door_rear_z, -half_w + door_thick, hood_y, door_split_z, door_color, "door_rl")
        b.add_box(-half_w + 0.01, hood_y, door_rear_z + 0.03, -roof_w + 0.01, roof_y - 0.04, door_split_z - 0.03, "glass_tinted", "door_rl")
        b.add_box(-half_w - 0.015, hood_y - 0.10, door_rear_z + 0.08, -half_w, hood_y - 0.06, door_rear_z + 0.22, "chrome_silver", "door_rl")

        # Rear-Right Door (RR)
        b.add_box(half_w - door_thick, chassis_bot_y + 0.12, door_rear_z, half_w, hood_y, door_split_z, door_color, "door_rr")
        b.add_box(roof_w - 0.01, hood_y, door_rear_z + 0.03, half_w - 0.01, roof_y - 0.04, door_split_z - 0.03, "glass_tinted", "door_rr")
        b.add_box(half_w, hood_y - 0.10, door_rear_z + 0.08, half_w + 0.015, hood_y - 0.06, door_rear_z + 0.22, "chrome_silver", "door_rr")

    # --------------------------------------------------------------------------
    # 8. RETRO INTERIOR (Dashboard, Column & Bench/Seats)
    # --------------------------------------------------------------------------
    dash_z = cab_front_z - 0.12
    b.add_box(-roof_w * 0.90, hood_y - 0.22, dash_z - 0.04, roof_w * 0.90, hood_y + 0.02, cab_front_z + 0.02, "interior_dash", "interior")
    b.add_box(-roof_w * 0.80, hood_y + 0.02, dash_z, -roof_w * 0.35, hood_y + 0.08, cab_front_z, "interior_dash", "interior")

    seat_z = (door_front_z + door_split_z) * 0.5
    seat_w = 0.35
    seat_bot_y = chassis_bot_y + 0.14
    # Driver Cushion & Backrest
    b.add_box(-half_w * 0.70, seat_bot_y, seat_z - 0.20, -half_w * 0.70 + seat_w, seat_bot_y + 0.18, seat_z + 0.22, "interior_seat", "interior")
    b.add_box(-half_w * 0.70, seat_bot_y + 0.18, seat_z - 0.20, -half_w * 0.70 + seat_w, seat_bot_y + 0.65, seat_z - 0.08, "interior_seat", "interior")
    # Passenger Cushion & Backrest
    b.add_box( half_w * 0.70 - seat_w, seat_bot_y, seat_z - 0.20,  half_w * 0.70, seat_bot_y + 0.18, seat_z + 0.22, "interior_seat", "interior")
    b.add_box( half_w * 0.70 - seat_w, seat_bot_y + 0.18, seat_z - 0.20,  half_w * 0.70, seat_bot_y + 0.65, seat_z - 0.08, "interior_seat", "interior")

    if not is_pickup:
        rear_seat_z = (door_split_z + door_rear_z) * 0.5
        b.add_box(-half_w * 0.72, seat_bot_y, rear_seat_z - 0.22, half_w * 0.72, seat_bot_y + 0.18, rear_seat_z + 0.22, "interior_seat", "interior")
        b.add_box(-half_w * 0.72, seat_bot_y + 0.18, rear_seat_z - 0.22, half_w * 0.72, seat_bot_y + 0.65, rear_seat_z - 0.10, "interior_seat", "interior")

    # --------------------------------------------------------------------------
    # 9. SURVIVAL UPGRADES & POLICE GEAR
    # --------------------------------------------------------------------------
    if has_bullbar or is_police:
        # Authentic Heavy PIT Push Bumper
        bb_z = front_bumper_z + 0.10
        bb_r = 0.026
        # Upper & Lower horizontal tubes
        b.add_cylinder((-half_w * 0.75, bumper_y + 0.08, bb_z), (half_w * 0.75, bumper_y + 0.08, bb_z), bb_r, 6, "plastic_black", "bullbar")
        b.add_cylinder((-half_w * 0.68, hood_y + 0.05, bb_z), (half_w * 0.68, hood_y + 0.05, bb_z), bb_r, 6, "plastic_black", "bullbar")
        # Heavy Vertical Uprights with thick rubber front pads
        b.add_box(-0.35, bumper_y - 0.04, bb_z - 0.04, -0.28, hood_y + 0.08, bb_z + 0.03, "plastic_black", "bullbar")
        b.add_box( 0.28, bumper_y - 0.04, bb_z - 0.04,  0.35, hood_y + 0.08, bb_z + 0.03, "plastic_black", "bullbar")
        # Headlight wrap-around brush bars
        b.add_cylinder((-half_w * 0.75, bumper_y + 0.08, bb_z), (-half_w * 0.85, hood_y + 0.02, bb_z - 0.12), bb_r, 6, "plastic_black", "bullbar")
        b.add_cylinder(( half_w * 0.75, bumper_y + 0.08, bb_z), ( half_w * 0.85, hood_y + 0.02, bb_z - 0.12), bb_r, 6, "plastic_black", "bullbar")
        # Mounting struts into chassis
        b.add_box(-0.34, bumper_y + 0.02, front_bumper_z - 0.10, -0.29, bumper_y + 0.16, bb_z, "plastic_black", "bullbar")
        b.add_box( 0.29, bumper_y + 0.02, front_bumper_z - 0.10,  0.34, bumper_y + 0.16, bb_z, "plastic_black", "bullbar")

    if has_roof_rack:
        rr_z1 = cab_rear_z + 0.10
        rr_z2 = windshield_top_z - 0.08
        rr_y = roof_y + 0.05
        rr_h = 0.12
        r_pipe = 0.020
        b.add_cylinder((-roof_w * 0.90, rr_y + rr_h, rr_z1), (-roof_w * 0.90, rr_y + rr_h, rr_z2), r_pipe, 6, "plastic_black", "roof_rack")
        b.add_cylinder(( roof_w * 0.90, rr_y + rr_h, rr_z1), ( roof_w * 0.90, rr_y + rr_h, rr_z2), r_pipe, 6, "plastic_black", "roof_rack")
        for s in range(5):
            sz = rr_z1 + (rr_z2 - rr_z1) * (s / 4.0)
            b.add_cylinder((-roof_w * 0.90, rr_y, sz), (roof_w * 0.90, rr_y, sz), r_pipe * 0.8, 6, "plastic_black", "roof_rack")
        b.add_box(-roof_w * 0.92, roof_y, rr_z1, -roof_w * 0.86, rr_y + 0.05, rr_z1 + 0.06, "plastic_black", "roof_rack")
        b.add_box( roof_w * 0.86, roof_y, rr_z1,  roof_w * 0.92, rr_y + 0.05, rr_z1 + 0.06, "plastic_black", "roof_rack")
        b.add_box(-roof_w * 0.92, roof_y, rr_z2 - 0.06, -roof_w * 0.86, rr_y + 0.05, rr_z2, "plastic_black", "roof_rack")
        b.add_box( roof_w * 0.86, roof_y, rr_z2 - 0.06,  roof_w * 0.92, rr_y + 0.05, rr_z2, "plastic_black", "roof_rack")

    if has_window_mesh:
        for bar_idx in range(5):
            by = hood_y + 0.10 + bar_idx * 0.10
            bz = cab_front_z - 0.08 - bar_idx * 0.09
            b.add_cylinder((-roof_w * 0.88, by, bz), (roof_w * 0.88, by, bz), 0.012, 4, "chrome_silver", "window_mesh")

    if is_police:
        # 1. Driver's A-Pillar Police Spotlight (Authentic Zomboid Feature)
        sp_base_y = hood_y + 0.14
        sp_base_z = cab_front_z - 0.10
        # Chrome bracket out from pillar
        b.add_cylinder((-half_w + 0.02, sp_base_y, sp_base_z), (-half_w - 0.11, sp_base_y + 0.04, sp_base_z + 0.05), 0.014, 4, "chrome_silver", "chassis_base")
        # Spotlight cylindrical lamp housing
        b.add_cylinder((-half_w - 0.11, sp_base_y + 0.04, sp_base_z + 0.05), (-half_w - 0.11, sp_base_y + 0.04, sp_base_z + 0.14), 0.045, 8, "chrome_silver", "chassis_base")
        # Front white glass lens of spotlight
        b.add_cylinder((-half_w - 0.11, sp_base_y + 0.04, sp_base_z + 0.14), (-half_w - 0.11, sp_base_y + 0.04, sp_base_z + 0.15), 0.042, 8, "headlight_white", "chassis_base")

        # 2. Sleek Dual-Pod Emergency Lightbar on Roof
        lb_y = roof_y + 0.02
        lb_z = (windshield_top_z + cab_rear_z) * 0.5
        lb_w = roof_w * 0.78
        # Left Red Flasher Pod
        b.add_box(-lb_w, lb_y, lb_z - 0.10, -0.09, lb_y + 0.12, lb_z + 0.10, "siren_red", "lightbar")
        # Right Blue Flasher Pod
        b.add_box(0.09, lb_y, lb_z - 0.10, lb_w, lb_y + 0.12, lb_z + 0.10, "siren_blue", "lightbar")
        # Center Chrome Siren Speaker Box
        b.add_box(-0.09, lb_y, lb_z - 0.08, 0.09, lb_y + 0.10, lb_z + 0.08, "chrome_silver", "lightbar")
        # Mounting feet to roof
        b.add_box(-lb_w, roof_y, lb_z - 0.04, -lb_w + 0.06, lb_y, lb_z + 0.04, "plastic_black", "lightbar")
        b.add_box( lb_w - 0.06, roof_y, lb_z - 0.04,  lb_w, lb_y, lb_z + 0.04, "plastic_black", "lightbar")

    return b, {
        "half_w": half_w,
        "fwd_z": fwd_z,
        "rear_z": rear_z,
        "wheelbase": wheelbase,
        "hood_y": hood_y,
        "roof_y": roof_y,
        "front_bumper_z": front_bumper_z,
        "rear_bumper_z": rear_bumper_z,
        "cab_front_z": cab_front_z,
        "cab_rear_z": cab_rear_z,
        "door_split_z": door_split_z,
        "is_pickup": is_pickup,
        "is_suv": is_suv
    }


# ==============================================================================
# CARDECOR VEHICLE CONFIG JSON GENERATOR
# ==============================================================================

def generate_cardecor_json(vehicle_id, vehicle_name, vehicle_type, dims, pack_dir):
    half_w = dims["half_w"]
    wb = dims["wheelbase"]
    fwd_z = dims["fwd_z"]
    rear_z = dims["rear_z"]
    front_bumper_z = dims["front_bumper_z"]
    rear_bumper_z = dims["rear_bumper_z"]
    is_pickup = dims["is_pickup"]

    # Wheel sits nicely tucked inside fender flare
    wheel_track_x = half_w - 0.09 # 0.83m
    wheel_y = 0.38

    seat_driver_z = (dims["cab_front_z"] + dims["door_split_z"]) * 0.5

    data = {
        "id": vehicle_id,
        "name": vehicle_name,
        "category": "pickup" if is_pickup else "car",
        "default_engine": "ford_302_v8" if is_pickup else "om617_diesel",
        "default_wheel": "standard_road_wheel",
        "default_seat": "standard_seat",
        "models": {
            "chassis": f"models/{vehicle_id}_chassis.obj",
            "wheel": "models/zomboid_wheel.obj",
            "steering_wheel": "models/zomboid_steering_wheel.obj",
            "flip_v": False
        },
        "textures": {
            "chassis": f"textures/{vehicle_id}_skin.png"
        },
        "physics": {
            "mass": 1650.0 if is_pickup else 1450.0,
            "horsepower": 185.0 if is_pickup else 165.0,
            "torque_nm": 290.0 if is_pickup else 245.0,
            "max_speed": 78.0,
            "boost_speed": 95.0,
            "gears": 5,
            "engine_power": 13000.0,
            "drivetrain": "rwd" if not dims.get("is_suv", False) else "awd",
            "wheelbase": round(wb, 2),
            "track_width": round(wheel_track_x * 2.0, 2),
            "wheel_radius": 0.38,
            "front_overhang": round(front_bumper_z - fwd_z, 2),
            "rear_overhang": round(abs(rear_bumper_z) - abs(rear_z), 2),
            "max_steer_angle": 38.0,
            "brake_power": 19500.0,
            "suspension_stiffness": 1.15,
            "fuel_capacity_liters": 65.0
        },
        "wheels": {
            "radius": 0.38,
            "wheel_hp": 150.0,
            "front_left":  [ round( wheel_track_x, 2), wheel_y, round( fwd_z, 2) ],
            "front_right": [ round(-wheel_track_x, 2), wheel_y, round( fwd_z, 2) ],
            "rear_left":   [ round( wheel_track_x, 2), wheel_y, round( rear_z, 2) ],
            "rear_right":  [ round(-wheel_track_x, 2), wheel_y, round( rear_z, 2) ]
        },
        "steering_wheel": {
            "enabled": True,
            "pos": [ round(-half_w * 0.52, 2), round(dims["hood_y"] + 0.12, 2), round(dims["cab_front_z"] - 0.18, 2) ],
            "tilt_angle_deg": -18.0,
            "rotation_multiplier": 14.0
        },
        "seats": [
            {
                "seat_index": 0,
                "name": "Водитель",
                "pos": [ round(-half_w * 0.52, 2), 0.24, round(seat_driver_z, 2) ],
                "is_driver": True,
                "lock_yaw": True
            },
            {
                "seat_index": 1,
                "name": "Передний пассажир",
                "pos": [ round(half_w * 0.52, 2), 0.24, round(seat_driver_z, 2) ],
                "is_driver": False,
                "lock_yaw": True
            }
        ],
        "calibrated_bounds": {
            "half_width": round(half_w + 0.05, 2),
            "front_dist": round(front_bumper_z + 0.05, 2),
            "rear_dist": round(abs(rear_bumper_z) + 0.05, 2),
            "hood_z": round(dims["cab_front_z"], 2),
            "cabin_rear_z": round(dims["cab_rear_z"], 2),
            "trunk_height": round(dims["hood_y"], 2),
            "roof_y": round(dims["roof_y"], 2)
        },
        "exhaust": {
            "pos": [ round(half_w * 0.72, 2), 0.30, round(rear_bumper_z - 0.05, 2) ]
        },
        "headlights": {
            "flares": [
                { "pos": [ round(-half_w * 0.65, 2), round(dims["hood_y"] - 0.10, 2), round(front_bumper_z - 0.04, 2) ], "size": 0.45 },
                { "pos": [ round( half_w * 0.65, 2), round(dims["hood_y"] - 0.10, 2), round(front_bumper_z - 0.04, 2) ], "size": 0.45 }
            ]
        },
        "taillights": {
            "flares": [
                { "pos": [ round(-half_w * 0.65, 2), round(dims["hood_y"] - 0.10, 2), round(rear_bumper_z + 0.04, 2) ], "size": 0.35 },
                { "pos": [ round( half_w * 0.65, 2), round(dims["hood_y"] - 0.10, 2), round(rear_bumper_z + 0.04, 2) ], "size": 0.35 }
            ]
        },
        "animations": [
            { "part": "door_fl", "type": "rotation", "variable": "door_left",  "axis": [0, 1, 0], "min": 0, "max": 65, "factor": 65.0, "center": [ round(-half_w, 2), 0.55, round(dims["cab_front_z"], 2) ] },
            { "part": "door_fr", "type": "rotation", "variable": "door_right", "axis": [0, 1, 0], "min": -65, "max": 0, "factor": -65.0, "center": [ round(half_w, 2), 0.55, round(dims["cab_front_z"], 2) ] },
            { "part": "hood",    "type": "rotation", "variable": "hood",       "axis": [1, 0, 0], "min": -45, "max": 0, "factor": -45.0, "center": [ 0, dims["hood_y"], dims["cab_front_z"] ] },
            { "part": "trunk",   "type": "rotation", "variable": "trunk",      "axis": [1, 0, 0], "min": 0, "max": 50, "factor": 50.0, "center": [ 0, dims["hood_y"], dims["cab_rear_z"] ] }
        ]
    }

    if not is_pickup:
        rear_seat_z = (dims["door_split_z"] + dims["cab_rear_z"]) * 0.5
        data["seats"].append({
            "seat_index": 2,
            "name": "Заднее левое",
            "pos": [ round(-half_w * 0.52, 2), 0.24, round(rear_seat_z, 2) ],
            "is_driver": False
        })
        data["seats"].append({
            "seat_index": 3,
            "name": "Заднее правое",
            "pos": [ round(half_w * 0.52, 2), 0.24, round(rear_seat_z, 2) ],
            "is_driver": False
        })

    json_path = os.path.join(pack_dir, "vehicles", f"{vehicle_id}.json")
    os.makedirs(os.path.dirname(os.path.abspath(json_path)), exist_ok=True)
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


# ==============================================================================
# MAIN CLI ENTRY POINT
# ==============================================================================

def main():
    parser = argparse.ArgumentParser(description="Project Zomboid Vehicle Studio for CarDecor")
    parser.add_argument("--type", default="sedan_90s", choices=["sedan_90s", "pickup_utility", "suv_offroad", "police_cruiser", "muscle_classic"], help="Vehicle body preset")
    parser.add_argument("--id", default=None, help="Vehicle ID (e.g. zomboid_nyala)")
    parser.add_argument("--name", default=None, help="Display Name (e.g. Chevalier Nyala 1992)")
    parser.add_argument("--palette", default="worn_blue", choices=list(PALETTES.keys()), help="Color theme")
    parser.add_argument("--bullbar", action="store_true", help="Add heavy steel front push bumper")
    parser.add_argument("--roof-rack", action="store_true", help="Add roof cargo rack")
    parser.add_argument("--window-mesh", action="store_true", help="Add reinforced metal window armor bars")
    parser.add_argument("--out", default=r"run\cardecor_packs\cardecor_official_pack", help="Destination pack directory")

    args = parser.parse_args()

    v_type = args.type
    v_id = args.id or f"pz_{v_type}"
    v_name = args.name or {
        "sedan_90s": "Chevalier Nyala 1992",
        "pickup_utility": "Chevalier D6 Squarebody 1988",
        "suv_offroad": "Dash Rancher All-Terrain 1990",
        "police_cruiser": "Knox County Police Cruiser",
        "muscle_classic": "Chevalier Cossette 1986",
    }.get(v_type, "Project Zomboid Vehicle")

    pack_dir = os.path.abspath(args.out)

    print(f"[Zomboid Studio] Building vehicle '{v_name}' ({v_id})...")
    print(f"  Type: {v_type} | Palette: {args.palette} | Bullbar: {args.bullbar} | Roof Rack: {args.roof_rack} | Mesh: {args.window_mesh}")

    # 1. Build 3D Chassis Model
    builder, dims = build_zomboid_vehicle(
        vehicle_type=v_type,
        has_bullbar=args.bullbar,
        has_roof_rack=args.roof_rack,
        has_window_mesh=args.window_mesh,
        palette=args.palette
    )
    chassis_obj_path = os.path.join(pack_dir, "models", f"{v_id}_chassis.obj")
    builder.write_obj(chassis_obj_path)
    print(f"  [+] Chassis OBJ written: {chassis_obj_path}")

    # 2. Build Authentic Zomboid Steelie Wheel
    wheel_obj_path = os.path.join(pack_dir, "models", "zomboid_wheel.obj")
    generate_zomboid_wheel(wheel_obj_path)
    print(f"  [+] Zomboid Steelie Wheel OBJ written: {wheel_obj_path}")

    # 3. Build Retro Steering Wheel
    steering_obj_path = os.path.join(pack_dir, "models", "zomboid_steering_wheel.obj")
    generate_steering_wheel(steering_obj_path)
    print(f"  [+] Steering Wheel OBJ written: {steering_obj_path}")

    # 4. Build Clean 512x512 Palette Texture
    skin_png_path = os.path.join(pack_dir, "textures", f"{v_id}_skin.png")
    generate_zomboid_texture(skin_png_path, palette_name=args.palette)
    print(f"  [+] Pure Palette Texture written: {skin_png_path}")

    # 5. Build CarDecor JSON Config
    generate_cardecor_json(v_id, v_name, v_type, dims, pack_dir)
    v_json_path = os.path.join(pack_dir, "vehicles", f"{v_id}.json")
    print(f"  [+] Vehicle JSON written: {v_json_path}")

    print("[Zomboid Studio] Vehicle generation COMPLETE! Ready for game deployment.")

if __name__ == "__main__":
    main()
