"""
Blockbench .bbmodel to CarDecor Pack Converter
Converts a Blockbench model into a CarDecor vehicle pack:
- Extracts embedded PNG textures
- Converts meshes to OBJ with groups (o <group_name>)
- Generates vehicle definition JSON with animation definitions
"""

import json
import os
import base64
import math

def convert_bbmodel_to_pack(bbmodel_path, output_dir):
    with open(bbmodel_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    os.makedirs(output_dir, exist_ok=True)
    models_dir = os.path.join(output_dir, 'models')
    textures_dir = os.path.join(output_dir, 'textures')
    os.makedirs(models_dir, exist_ok=True)
    os.makedirs(textures_dir, exist_ok=True)

    print(f"Converting {bbmodel_path} -> {output_dir}")

    # 1. Extract Textures
    texture_files = {}
    for idx, tex in enumerate(data.get('textures', [])):
        name = tex.get('name', f'texture_{idx}')
        if not name.endswith('.png'):
            name += '.png'
        src = tex.get('source', '')
        if src.startswith('data:image/png;base64,'):
            b64_data = src.split(',', 1)[1]
            tex_path = os.path.join(textures_dir, name)
            with open(tex_path, 'wb') as img_out:
                img_out.write(base64.b64decode(b64_data))
            texture_files[idx] = os.path.relpath(tex_path, output_dir).replace('\\', '/')
            print(f"  Extracted texture: {name} ({tex.get('width')}x{tex.get('height')})")

    # Primary skin texture
    primary_tex = texture_files.get(0, 'textures/skin.png')

    # 2. Map Elements and Groups
    groups = {g.get('uuid'): g for g in data.get('groups', [])}
    uuid_to_group_name = {g.get('uuid'): g.get('name') for g in data.get('groups', [])}

    element_to_group = {}
    def trace_outliner(items, current_group='body'):
        for it in items:
            if isinstance(it, dict):
                gid = it.get('uuid')
                gname = uuid_to_group_name.get(gid, current_group)
                if 'children' in it:
                    trace_outliner(it['children'], gname)
            elif isinstance(it, str):
                element_to_group[it] = current_group

    trace_outliner(data.get('outliner', []))

    # Also resolve elements
    elements = data.get('elements', [])
    elements_by_uuid = {e.get('uuid'): e for e in elements}

    # Wheels vs Body vs Wheel separate obj
    wheel_groups = {'front-left-wheel', 'front-right-wheel', 'back-left-wheel', 'back-right-wheel', 'jeep-wheels'}

    # 3. Build OBJ meshes
    # Coordinate system in Blockbench:
    # BB: X is right, Y is up, Z is south (+Z forward/back depending on convention).
    # Typically 16 units = 1 meter in Minecraft / Blockbench!
    SCALE = 1.0 / 16.0 # Convert Blockbench units to meters

    def write_obj_file(filepath, element_filter, origin_offset=(0,0,0)):
        ox, oy, oz = origin_offset
        v_idx_offset = 1
        vt_idx_offset = 1

        with open(filepath, 'w', encoding='utf-8') as obj_file:
            obj_file.write(f"# Exported by CarDecor BBModel Converter\n")

            # Group elements by group name
            grouped_elements = {}
            for el in elements:
                el_uuid = el.get('uuid')
                gname = element_to_group.get(el_uuid, 'body')
                if element_filter(gname, el):
                    grouped_elements.setdefault(gname, []).append(el)

            for gname, el_list in grouped_elements.items():
                obj_file.write(f"\no {gname}\n")
                obj_file.write(f"g {gname}\n")

                for el in el_list:
                    el_origin = el.get('origin', [0, 0, 0])
                    el_rot = el.get('rotation', [0, 0, 0])
                    # Vertices
                    raw_verts = el.get('vertices', {})
                    if not raw_verts:
                        continue

                    # Local rotation matrix around el_origin if any
                    rx, ry, rz = [math.radians(a) for a in el_rot]
                    # Compute rotation matrix
                    # BB rotation order is typically X, Y, Z or Z, Y, X
                    # Compute transformed vertices
                    transformed_verts = {}
                    for vid, vpos in raw_verts.items():
                        # vertex pos relative to el_origin
                        # Note: in Blockbench mesh elements, vertices are usually already in element space
                        vx, vy, vz = vpos
                        # Rotate around el_origin if needed, or if mesh element coords are already global:
                        # In bbmodel freehand mesh, vertices are local to origin [x, y, z]
                        # Let's apply element origin:
                        world_x = (vx + el_origin[0] - ox) * SCALE
                        world_y = (vy + el_origin[1] - oy) * SCALE
                        world_z = (vz + el_origin[2] - oz) * SCALE
                        transformed_verts[vid] = (world_x, world_y, world_z)

                    vert_map = {}
                    for vid, pos in transformed_verts.items():
                        obj_file.write(f"v {pos[0]:.6f} {pos[1]:.6f} {pos[2]:.6f}\n")
                        vert_map[vid] = v_idx_offset
                        v_idx_offset += 1

                    faces = el.get('faces', {})
                    for fid, fdata in faces.items():
                        fverts = fdata.get('vertices', [])
                        fuv = fdata.get('uv', {})
                        if len(fverts) < 3:
                            continue

                        # Write texture coordinates
                        vt_map = {}
                        for vid in fverts:
                            uv = fuv.get(vid, [0, 0])
                            # In Blockbench, UV is in pixels (e.g. 0 to 256) or percentage
                            # Normalize UV to [0, 1]
                            res_w = 256.0
                            res_h = 256.0
                            u = uv[0] / res_w
                            v = 1.0 - (uv[1] / res_h) # OBJ V is inverted
                            obj_file.write(f"vt {u:.6f} {v:.6f}\n")
                            vt_map[vid] = vt_idx_offset
                            vt_idx_offset += 1

                        # Write face
                        face_tokens = [f"{vert_map[vid]}/{vt_map[vid]}" for vid in fverts if vid in vert_map and vid in vt_map]
                        if len(face_tokens) >= 3:
                            obj_file.write(f"f {' '.join(face_tokens)}\n")

    # Write Chassis OBJ (everything except wheels)
    chassis_obj_path = os.path.join(models_dir, 'chassis.obj')
    write_obj_file(chassis_obj_path, lambda gname, el: gname not in wheel_groups)
    print(f"  Generated chassis mesh: {chassis_obj_path}")

    # Write Wheel OBJ (using front-left-wheel geometry, centered at wheel origin)
    wheel_origin = None
    for g in data.get('groups', []):
        if g.get('name') == 'front-left-wheel':
            wheel_origin = [c * SCALE for c in g.get('origin', [0, 0, 0])]
            break
    if not wheel_origin:
        wheel_origin = [0, 0, 0]

    fl_group_origin = groups.get(next((g['uuid'] for g in data.get('groups',[]) if g.get('name')=='front-left-wheel'), ''), {}).get('origin', [0,0,0])

    wheel_obj_path = os.path.join(models_dir, 'wheel.obj')
    write_obj_file(wheel_obj_path, lambda gname, el: gname == 'front-left-wheel', origin_offset=fl_group_origin)
    print(f"  Generated wheel mesh: {wheel_obj_path}")

    # 4. Generate Vehicle Definition JSON
    # Door origins and rotation limits from animations
    anim_defs = [
        {
            "part": "jeep-left-door",
            "type": "rotation",
            "variable": "door_left",
            "axis": [0.0, 1.0, 0.0],
            "center": [-7.75 * SCALE, 9.25 * SCALE, -4.75 * SCALE],
            "factor": -62.5,
            "min": -65.0,
            "max": 0.0
        },
        {
            "part": "jeep-right-door",
            "type": "rotation",
            "variable": "door_right",
            "axis": [0.0, 1.0, 0.0],
            "center": [7.75 * SCALE, 9.25 * SCALE, -4.75 * SCALE],
            "factor": 62.5,
            "min": 0.0,
            "max": 65.0
        },
        {
            "part": "jeep-back-door",
            "type": "rotation",
            "variable": "trunk",
            "axis": [0.0, 1.0, 0.0],
            "center": [-5.0 * SCALE, 8.75 * SCALE, 16.25 * SCALE],
            "factor": -62.5,
            "min": -65.0,
            "max": 0.0
        },
        {
            "part": "jeep-steering-wheel-head",
            "type": "rotation",
            "variable": "steer",
            "axis": [0.0, 0.5, 0.866], # Tilted steering column
            "center": [-4.25 * SCALE, 12.45 * SCALE, -2.0 * SCALE],
            "factor": 15.0,
            "min": -540.0,
            "max": 540.0
        },
        {
            "part": "jeep-e-break",
            "type": "rotation",
            "variable": "handbrake",
            "axis": [1.0, 0.0, 0.0],
            "center": [0.9 * SCALE, 6.75 * SCALE, -2.0 * SCALE],
            "factor": 27.5,
            "min": 0.0,
            "max": 27.5
        }
    ]

    veh_json = {
        "id": "jurassic_jeep",
        "name": "Jurassic Park Jeep",
        "category": "car",
        "scale": 1.0,
        "groundOffsetY": 0.0,
        "models": {
            "chassis": "models/chassis.obj",
            "wheel": "models/wheel.obj",
            "steeringWheel": "models/chassis.obj" # Steering wheel is part of chassis groups!
        },
        "textures": {
            "chassis": primary_tex
        },
        "wheels": {
            "radius": 0.38,
            "wheelHp": 180.0,
            "frontLeft": [-4.25 * SCALE, 4.0 * SCALE, -14.0 * SCALE],
            "frontRight": [4.25 * SCALE, 4.0 * SCALE, -14.0 * SCALE],
            "rearLeft": [-4.25 * SCALE, 4.0 * SCALE, 11.0 * SCALE],
            "rearRight": [4.25 * SCALE, 4.0 * SCALE, 11.0 * SCALE]
        },
        "seats": [
            {
                "seatIndex": 0,
                "name": "Водитель",
                "isDriver": True,
                "pos": [-0.35, 0.45, 0.25],
                "dismountOffset": [-1.2, 0.0, 0.25]
            },
            {
                "seatIndex": 1,
                "name": "Пассажир",
                "isDriver": False,
                "pos": [0.35, 0.45, 0.25],
                "dismountOffset": [1.2, 0.0, 0.25]
            }
        ],
        "physics": {
            "mass": 1550.0,
            "horsepower": 190.0,
            "torqueNm": 310.0,
            "maxSpeed": 95.0,
            "drivetrain": "awd",
            "engineType": "inline6",
            "offroadCapability": 1.45,
            "wheelbase": 2.50,
            "trackWidth": 1.65,
            "suspensionStiffness": 1.15
        },
        "animations": anim_defs
    }

    json_path = os.path.join(output_dir, "jurassic_jeep.json")
    with open(json_path, 'w', encoding='utf-8') as jf:
        json.dump(veh_json, jf, indent=2, ensure_ascii=False)

    print(f"  Generated vehicle json: {json_path}")
    print("Done converting pack!")

if __name__ == '__main__':
    bb_file = r"C:\Users\evgen\Downloads\jurassic-park-jeep.bbmodel"
    out_dir = r"B:\Developing\Battleverse\projects\car_decorations_mod\run\cardecor_packs\jurassic_jeep"
    convert_bbmodel_to_pack(bb_file, out_dir)
