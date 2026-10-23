import math
import os

dirs = [
    r"B:\Developing\Battleverse\projects\car_decorations_mod\run\cardecor_packs\cardecor_official_pack",
    r"C:\Users\evgen\AppData\Roaming\PrismLauncher\instances\1.20.1(2)\.minecraft\cardecor_packs\cardecor_official_pack",
    r"C:\Users\evgen\AppData\Roaming\PrismLauncher\instances\1.20.1\.minecraft\cardecor_packs\cardecor_official_pack",
    r"C:\Users\evgen\AppData\Roaming\PrismLauncher\instances\1.20.1(1)\.minecraft\cardecor_packs\cardecor_official_pack",
]

# Generate 8-sided shaft geometry
# Center: (0, 0) in wheel coordinates
# Radius: 0.05
# Z range: 0.0 to -0.32 (into the dashboard)
r = 0.048
z_top = 0.00
z_bot = -0.32
num_sides = 8

def generate_shaft_obj(existing_v_count, existing_vt_count, existing_vn_count):
    lines = []
    lines.append("\n# Rotating Steering Column Shaft")
    
    # 8 top vertices
    for i in range(num_sides):
        ang = i * (2.0 * math.pi / num_sides)
        x = r * math.cos(ang)
        y = r * math.sin(ang)
        lines.append(f"v {x:.6f} {y:.6f} {z_top:.6f}")
    
    # 8 bottom vertices
    for i in range(num_sides):
        ang = i * (2.0 * math.pi / num_sides)
        x = r * math.cos(ang)
        y = r * math.sin(ang)
        lines.append(f"v {x:.6f} {y:.6f} {z_bot:.6f}")
    
    # 1 dummy vt and vn
    lines.append("vt 0.500000 0.500000")
    lines.append("vn 0.000000 0.000000 1.000000")
    
    vt_idx = existing_vt_count + 1
    vn_idx = existing_vn_count + 1
    
    # Quad faces as 2 triangles
    for i in range(num_sides):
        next_i = (i + 1) % num_sides
        v_top1 = existing_v_count + 1 + i
        v_top2 = existing_v_count + 1 + next_i
        v_bot1 = existing_v_count + 1 + num_sides + i
        v_bot2 = existing_v_count + 1 + num_sides + next_i
        
        lines.append(f"f {v_top1}/{vt_idx}/{vn_idx} {v_top2}/{vt_idx}/{vn_idx} {v_bot2}/{vt_idx}/{vn_idx}")
        lines.append(f"f {v_top1}/{vt_idx}/{vn_idx} {v_bot2}/{vt_idx}/{vn_idx} {v_bot1}/{vt_idx}/{vn_idx}")
        
    return "\n".join(lines) + "\n"

for d in dirs:
    wheel_obj = os.path.join(d, "models", "sedan_steering_wheel.obj")
    if not os.path.exists(wheel_obj):
        continue
    
    with open(wheel_obj, "r", encoding="utf-8") as f:
        content = f.read()
    
    # If already has shaft, skip
    if "# Rotating Steering Column Shaft" in content:
        print(f"Already has shaft: {wheel_obj}")
        continue
    
    v_count = 0
    vt_count = 0
    vn_count = 0
    for line in content.splitlines():
        if line.startswith("v "):
            v_count += 1
        elif line.startswith("vt "):
            vt_count += 1
        elif line.startswith("vn "):
            vn_count += 1
            
    shaft_code = generate_shaft_obj(v_count, vt_count, vn_count)
    with open(wheel_obj, "a", encoding="utf-8") as f:
        f.write(shaft_code)
    print(f"Added rotating shaft to: {wheel_obj}")
