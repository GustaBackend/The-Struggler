#!/usr/bin/env python3
"""Canonical cube-first Midland Guard GeckoLib asset generator.

The visual source of truth is this file.  It emits the Bedrock/GeckoLib
geometry, the native animation JSON, an editable Blockbench Bedrock project,
and the shared 128px texture atlas.  Coordinates are Minecraft Bedrock Y-up
world coordinates: the feet rest at Y=0, the head is at Y=24..32, and front is
negative Z.  No Java Y flip or low-poly mesh conversion is involved.

Only Pillow is required for the texture atlas.  The generated model is a
restrained 52-cube medieval common guard: a closed steel greathelm with a
narrow black slit, layered steel armor over a dark tunic, brown equipment,
a one-hand sword, and a compact stepped heater-like shield.
"""

from __future__ import annotations

import base64
import hashlib
import json
import math
import uuid
from dataclasses import dataclass
from io import BytesIO
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART_DIR = ROOT / "art" / "midland_guard"
RESOURCE_DIR = ROOT / "src" / "main" / "resources" / "assets" / "struggler"
GEO_PATH = RESOURCE_DIR / "geo" / "midland_guard.geo.json"
ANIMATION_PATH = RESOURCE_DIR / "animations" / "midland_guard.animation.json"
RUNTIME_TEXTURE_PATH = RESOURCE_DIR / "textures" / "entity" / "midland_guard.png"
ART_TEXTURE_PATH = ART_DIR / "midland_guard.png"
BBMODEL_PATH = ART_DIR / "midland_guard.bbmodel"
ATLAS_SIZE = 128
SEED = "midland-guard-geckolib-closed-helm-v1"


@dataclass(frozen=True)
class Cube:
    name: str
    bone: str
    origin: tuple[float, float, float]
    size: tuple[float, float, float]
    material: str
    rotation: tuple[float, float, float] = (0.0, 0.0, 0.0)
    pivot: tuple[float, float, float] | None = None


# Bedrock bones use global pivots even when parented.  Cube origins below are
# intentionally global too, matching the user's requested GeckoLib contract.
BONES: list[dict[str, object]] = [
    {"name": "root", "parent": None, "pivot": [0, 0, 0]},
    {"name": "body", "parent": "root", "pivot": [0, 12, 0]},
    {"name": "head", "parent": "body", "pivot": [0, 24, 0]},
    {"name": "right_arm", "parent": "body", "pivot": [-5, 22, 0]},
    {"name": "left_arm", "parent": "body", "pivot": [5, 22, 0]},
    {"name": "right_leg", "parent": "root", "pivot": [-1.9, 12, 0]},
    {"name": "left_leg", "parent": "root", "pivot": [1.9, 12, 0]},
    {"name": "right_sword", "parent": "right_arm", "pivot": [-6, 13, 0]},
    {"name": "left_shield", "parent": "left_arm", "pivot": [6, 14, 0]},
]
BONE_PIVOTS = {str(bone["name"]): tuple(float(value) for value in bone["pivot"]) for bone in BONES}
BONE_NAMES = {str(bone["name"]) for bone in BONES}


# Each material has a clean atlas tile.  Face UV sizes are derived from the
# cube dimensions at exactly two texture pixels per world unit.
MATERIAL_REGIONS: dict[str, tuple[int, int, int, int]] = {
    "steel": (0, 0, 32, 32),
    "steel_dark": (32, 0, 64, 32),
    "steel_edge": (64, 0, 96, 32),
    "mail": (96, 0, 128, 32),
    "tunic": (0, 32, 32, 64),
    "burgundy": (32, 32, 64, 64),
    "leather": (64, 32, 96, 64),
    "boot": (96, 32, 128, 64),
    "wood": (0, 64, 32, 96),
    "wood_dark": (32, 64, 64, 96),
    "steel_bright": (64, 64, 96, 96),
    "slit": (96, 64, 112, 80),
}
MATERIAL_COLORS: dict[str, tuple[int, int, int]] = {
    "steel": (86, 91, 91),
    "steel_dark": (57, 62, 63),
    "steel_edge": (126, 128, 120),
    "mail": (48, 53, 53),
    "tunic": (32, 33, 37),
    "burgundy": (105, 31, 42),
    "leather": (91, 56, 36),
    "boot": (62, 40, 31),
    "wood": (86, 51, 38),
    "wood_dark": (55, 37, 32),
    "steel_bright": (157, 158, 145),
    "slit": (7, 8, 9),
}


def stable_uuid(kind: str, name: str) -> str:
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"https://struggler.invalid/{SEED}/{kind}/{name}"))


def fnum(value: float) -> int | float:
    value = float(value)
    return int(value) if value.is_integer() else round(value, 4)


def vector(values: Iterable[float]) -> list[int | float]:
    return [fnum(value) for value in values]


def make_cubes() -> list[Cube]:
    cubes: list[Cube] = []

    def add(
        name: str,
        bone: str,
        origin: tuple[float, float, float],
        size: tuple[float, float, float],
        material: str,
        rotation: tuple[float, float, float] = (0.0, 0.0, 0.0),
        pivot: tuple[float, float, float] | None = None,
    ) -> None:
        # Equipment was sketched top-to-bottom; convert that placement into
        # Bedrock Y-up while leaving the exact vanilla core and neck intact.
        if bone == "body" and name not in {"body_dark_tunic", "mail_collar"}:
            origin = (origin[0], 35.0 - origin[1] - size[1], origin[2])
        elif bone in {"right_arm", "left_arm"} and not name.endswith("_vanilla"):
            origin = (origin[0], 36.0 - origin[1] - size[1], origin[2])
            if pivot is not None:
                pivot = (pivot[0], 36.0 - pivot[1], pivot[2])
        elif bone == "right_sword":
            origin = (origin[0], 36.0 - origin[1] - size[1], origin[2])
        elif bone == "left_shield":
            origin = (origin[0] - 1.8, 34.0 - origin[1] - size[1], origin[2])
        cubes.append(Cube(name, bone, origin, size, material, rotation, pivot))

    # Exact human vanilla core volumes requested by the renderer contract.
    add("body_dark_tunic", "body", (-4, 12, -2), (8, 12, 4), "tunic")
    add("head_closed_helm_shell", "head", (-4, 24, -4), (8, 8, 8), "steel_dark")
    add("right_arm_vanilla", "right_arm", (-8, 12, -2), (4, 12, 4), "tunic")
    add("left_arm_vanilla", "left_arm", (4, 12, -2), (4, 12, 4), "tunic")
    add("right_leg_vanilla", "right_leg", (-3.9, 0, -2), (4, 12, 4), "tunic")
    add("left_leg_vanilla", "left_leg", (-0.1, 0, -2), (4, 12, 4), "tunic")

    # Closed steel greathelm.  The shell is opaque; only the separate narrow
    # black slit reads through the front armor.  Small rotated bevels and the
    # stepped crown keep it modeled rather than a flat face decal.
    add("helmet_top_step", "head", (-3.25, 31.4, -3.25), (6.5, 0.6, 6.5), "steel_edge")
    add("helmet_brow_visor", "head", (-3.6, 26.0, -4.35), (7.2, 1.2, 0.55), "steel")
    add("helmet_vision_slit", "head", (-2.7, 27.05, -4.58), (5.4, 0.38, 0.18), "slit")
    add("helmet_nose_ridge", "head", (-0.42, 25.35, -4.62), (0.84, 3.0, 0.5), "steel_edge")
    add("helmet_cheekguard_left", "head", (-4.35, 25.7, -3.45), (0.6, 4.7, 2.4), "steel")
    add("helmet_cheekguard_right", "head", (3.75, 25.7, -3.45), (0.6, 4.7, 2.4), "steel")
    add(
        "helmet_visor_bevel_left",
        "head",
        (-3.7, 27.0, -4.4),
        (3.4, 0.65, 0.45),
        "steel_edge",
        rotation=(0.0, 0.0, -7.0),
        pivot=(-0.3, 27.3, -4.35),
    )
    add(
        "helmet_visor_bevel_right",
        "head",
        (0.3, 27.0, -4.4),
        (3.4, 0.65, 0.45),
        "steel_edge",
        rotation=(0.0, 0.0, 7.0),
        pivot=(0.3, 27.3, -4.35),
    )

    # Volumetric breastplates on both sides, mail collar, dark tabard, belt,
    # and readable leather pouches.
    add("mail_collar", "body", (-3.5, 22.7, -2.35), (7.0, 1.3, 4.7), "mail")
    add("breastplate_front_upper", "body", (-3.5, 13.0, -2.7), (7.0, 4.2, 0.7), "steel")
    add("breastplate_front_lower", "body", (-3.0, 17.0, -2.6), (6.0, 3.2, 0.6), "steel_edge")
    add("breastplate_back_upper", "body", (-3.5, 13.0, 2.2), (7.0, 5.0, 0.6), "steel_dark")
    add("breastplate_back_lower", "body", (-3.0, 18.0, 2.1), (6.0, 2.4, 0.6), "steel_edge")
    add("burgundy_tabard_panel", "body", (-2.0, 19.3, -2.5), (4.0, 3.8, 0.5), "burgundy")
    add("belt_band", "body", (-4.3, 21.1, -2.5), (8.6, 1.0, 0.7), "leather")
    add("belt_buckle", "body", (-0.8, 20.9, -2.9), (1.6, 1.2, 0.35), "steel_edge")
    add("belt_pouch_right", "body", (3.7, 20.0, -2.4), (1.5, 2.2, 1.2), "leather")
    add("belt_pouch_left", "body", (-5.2, 20.0, -2.4), (1.5, 2.2, 1.2), "leather")

    # Mirrored arm armor: shoulders, mail at the elbow, broad bracers, and
    # articulated-looking gloves.  The arm core remains the vanilla 4x12x4.
    for side, x, sign in (("right", -8.6, -1.0), ("left", 3.4, 1.0)):
        add(
            f"{side}_shoulder_plate",
            f"{side}_arm",
            (x, 11.7, -2.6),
            (5.2, 2.3, 5.2),
            "steel",
            rotation=(0.0, 0.0, sign * 5.0),
            pivot=(-6.0 if side == "right" else 6.0, 13.0, 0.0),
        )
        add(f"{side}_elbow_mail", f"{side}_arm", (x + 0.35 * sign, 16.0, -2.35), (4.5, 3.0, 4.7), "mail")
        add(f"{side}_bracer", f"{side}_arm", (x + 0.15 * sign, 19.0, -2.65), (4.9, 4.2, 5.2), "steel")
        add(f"{side}_glove", f"{side}_arm", (x + 0.35 * sign, 22.6, -2.45), (4.5, 2.2, 4.8), "leather")

    # Six leg pieces: raised knee armor, long shins, and square-toed brown
    # boots.  The base leg cubes preserve the vanilla 4x12x4 proportions.
    for side, x in (("right", -4.15), ("left", -0.35)):
        add(f"{side}_knee_guard", f"{side}_leg", (x + 0.35, 6.2, -2.45), (3.8, 1.4, 4.9), "steel_edge")
        add(f"{side}_shin_plate", f"{side}_leg", (x + 0.35, 3.1, -2.4), (3.8, 3.5, 4.8), "steel")
        add(f"{side}_brown_boot", f"{side}_leg", (x + 0.15, 0.0, -2.9), (4.2, 3.2, 5.5), "boot")

    # One-hand sword lowered beside the right hand; converted by add() into Y-up.
    # The tip remains above the ground in the rest pose.
    add("sword_grip", "right_sword", (-6.45, 21.3, -2.65), (0.9, 2.4, 0.9), "leather")
    add("sword_pommel", "right_sword", (-6.55, 20.4, -2.72), (1.1, 0.9, 1.1), "steel_edge")
    add("sword_crossguard", "right_sword", (-7.9, 23.45, -2.82), (3.8, 0.65, 1.25), "steel_edge")
    add("sword_blade", "right_sword", (-6.42, 24.05, -2.69), (0.84, 6.6, 0.7), "steel_bright")
    add("sword_tip", "right_sword", (-6.42, 30.65, -2.68), (0.84, 0.9, 0.7), "steel_edge")

    # Small stepped heater-like shield on the left, front faces at negative Z.
    # Grip and strap are real positive-Z rear volumes, not painted markings.
    add("shield_board", "left_shield", (5.2, 15.0, -3.1), (6.0, 8.8, 0.9), "wood")
    add("shield_top_step", "left_shield", (5.9, 14.0, -3.05), (4.6, 1.2, 0.85), "wood")
    add("shield_lower_tip", "left_shield", (6.4, 23.8, -3.05), (3.2, 1.8, 0.85), "wood_dark")
    add("shield_iron_rim_left", "left_shield", (4.9, 15.0, -3.35), (0.45, 8.8, 1.0), "steel_edge")
    add("shield_iron_rim_right", "left_shield", (11.05, 15.0, -3.35), (0.45, 8.8, 1.0), "steel_edge")
    add("shield_burgundy_stripe", "left_shield", (7.75, 15.6, -3.58), (0.9, 7.0, 0.2), "burgundy")
    add("shield_front_boss", "left_shield", (7.85, 18.3, -3.8), (0.7, 1.4, 0.3), "steel_edge")
    add("shield_back_grip", "left_shield", (6.5, 18.3, -1.8), (3.4, 1.1, 1.0), "leather")
    add("shield_back_strap", "left_shield", (5.9, 20.3, -1.75), (1.0, 2.0, 0.8), "leather")

    assert len(cubes) == 52, f"expected 52 authored cubes, got {len(cubes)}"
    assert all(cube.bone in BONE_NAMES for cube in cubes)
    return cubes


CUBES = make_cubes()


def cube_face_uv(cube: Cube) -> dict[str, dict[str, list[int | float]]]:
    """Return custom per-face UVs at exactly two atlas pixels per unit."""
    left, top, right, bottom = MATERIAL_REGIONS[cube.material]
    region_w = right - left
    region_h = bottom - top
    w, h, d = cube.size
    dimensions = {
        "north": (w * 2.0, h * 2.0),
        "south": (w * 2.0, h * 2.0),
        "east": (d * 2.0, h * 2.0),
        "west": (d * 2.0, h * 2.0),
        "up": (w * 2.0, d * 2.0),
        "down": (w * 2.0, d * 2.0),
    }
    result: dict[str, dict[str, list[int | float]]] = {}
    for face, (uv_w, uv_h) in dimensions.items():
        if uv_w > region_w or uv_h > region_h:
            raise ValueError(f"{cube.name} {face} does not fit its material UV tile")
        uv_x = left + (region_w - uv_w) / 2.0
        uv_y = top + (region_h - uv_h) / 2.0
        result[face] = {"uv": vector((uv_x, uv_y)), "uv_size": vector((uv_w, uv_h))}
    return result


def geo_cube(cube: Cube) -> dict[str, object]:
    data: dict[str, object] = {
        "name": cube.name,
        "origin": vector(cube.origin),
        "size": vector(cube.size),
        "uv": cube_face_uv(cube),
        "inflate": 0.0,
    }
    if any(abs(value) > 1e-7 for value in cube.rotation):
        data["rotation"] = vector(cube.rotation)
        data["pivot"] = vector(cube.pivot or BONE_PIVOTS[cube.bone])
    return data


def geo_json() -> dict[str, object]:
    geometry_bones: list[dict[str, object]] = []
    for bone in BONES:
        name = str(bone["name"])
        data: dict[str, object] = {
            "name": name,
            "pivot": bone["pivot"],
            "cubes": [geo_cube(cube) for cube in CUBES if cube.bone == name],
        }
        if bone["parent"] is not None:
            data["parent"] = bone["parent"]
        geometry_bones.append(data)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.midland_guard",
                    "texture_width": ATLAS_SIZE,
                    "texture_height": ATLAS_SIZE,
                    "visible_bounds_width": 3.0,
                    "visible_bounds_height": 3.0,
                    "visible_bounds_offset": [0, 1, 0],
                },
                "bones": geometry_bones,
            }
        ],
    }


# Animation source data is shared by the native GeckoLib JSON and Blockbench's
# editable animation tracks.  Values are degrees, seconds, and local bone
# offsets, matching Bedrock/GeckoLib conventions.
CLIPS: dict[str, dict[str, object]] = {
    "idle": {
        "duration": 2.0,
        "loop": True,
        "bones": {
            "body": {
                "rotation": [(0.0, (0, 0, 0)), (0.5, (1.2, 0, 0)), (1.0, (0, 0, 0)), (1.5, (-1.2, 0, 0)), (2.0, (0, 0, 0))],
                "position": [(0.0, (0, 0, 0)), (0.5, (0, 0.12, 0)), (1.0, (0, 0, 0)), (1.5, (0, -0.12, 0)), (2.0, (0, 0, 0))],
            },
            "head": {
                "rotation": [(0.0, (0, 0, 0)), (0.5, (-0.8, 0, 0)), (1.0, (0, 0, 0)), (1.5, (0.8, 0, 0)), (2.0, (0, 0, 0))],
            },
            "right_arm": {"rotation": [(0.0, (0, 0, -2)), (0.5, (0, 0, -2.4)), (1.0, (0, 0, -2)), (1.5, (0, 0, -1.6)), (2.0, (0, 0, -2))]},
            "left_arm": {"rotation": [(0.0, (0, 0, 2)), (0.5, (0, 0, 2.4)), (1.0, (0, 0, 2)), (1.5, (0, 0, 1.6)), (2.0, (0, 0, 2))]},
        },
    },
    "walk": {
        "duration": 1.0,
        "loop": True,
        "bones": {
            "body": {"rotation": [(0.0, (2.5, 0, 0)), (0.25, (0, 0, 0)), (0.5, (-2.5, 0, 0)), (0.75, (0, 0, 0)), (1.0, (2.5, 0, 0))]},
            "head": {"rotation": [(0.0, (-1.0, 0, 0)), (0.5, (1.0, 0, 0)), (1.0, (-1.0, 0, 0))]},
            "right_leg": {"rotation": [(0.0, (32, 0, 0)), (0.25, (0, 0, 0)), (0.5, (-32, 0, 0)), (0.75, (0, 0, 0)), (1.0, (32, 0, 0))]},
            "left_leg": {"rotation": [(0.0, (-32, 0, 0)), (0.25, (0, 0, 0)), (0.5, (32, 0, 0)), (0.75, (0, 0, 0)), (1.0, (-32, 0, 0))]},
            "right_arm": {"rotation": [(0.0, (-22, 0, -2)), (0.25, (0, 0, 0)), (0.5, (22, 0, -2)), (0.75, (0, 0, 0)), (1.0, (-22, 0, -2))]},
            "left_arm": {"rotation": [(0.0, (22, 0, 2)), (0.25, (0, 0, 0)), (0.5, (-22, 0, 2)), (0.75, (0, 0, 0)), (1.0, (22, 0, 2))]},
        },
    },
    "run": {
        "duration": 0.8,
        "loop": True,
        "bones": {
            "body": {"rotation": [(0.0, (7, 0, 0)), (0.2, (5, 0, 0)), (0.4, (7, 0, 0)), (0.6, (5, 0, 0)), (0.8, (7, 0, 0))]},
            "head": {"rotation": [(0.0, (-3, 0, 0)), (0.4, (-2, 0, 0)), (0.8, (-3, 0, 0))]},
            "right_leg": {"rotation": [(0.0, (46, 0, 0)), (0.2, (0, 0, 0)), (0.4, (-46, 0, 0)), (0.6, (0, 0, 0)), (0.8, (46, 0, 0))]},
            "left_leg": {"rotation": [(0.0, (-46, 0, 0)), (0.2, (0, 0, 0)), (0.4, (46, 0, 0)), (0.6, (0, 0, 0)), (0.8, (-46, 0, 0))]},
            "right_arm": {"rotation": [(0.0, (-34, 0, -4)), (0.2, (0, 0, 0)), (0.4, (34, 0, -4)), (0.6, (0, 0, 0)), (0.8, (-34, 0, -4))]},
            "left_arm": {"rotation": [(0.0, (34, 0, 4)), (0.2, (0, 0, 0)), (0.4, (-34, 0, 4)), (0.6, (0, 0, 0)), (0.8, (34, 0, 4))]},
        },
    },
    "attack": {
        "duration": 1.6,
        "loop": False,
        "bones": {
            "body": {"rotation": [(0.0, (-8, -4, 0)), (0.35, (-12, -8, 0)), (0.75, (-2, -3, 0)), (1.1, (18, 8, 0)), (1.6, (4, 2, 0))]},
            "head": {"rotation": [(0.0, (6, 0, 0)), (0.35, (10, 0, 0)), (0.75, (2, 0, 0)), (1.1, (-8, 0, 0)), (1.6, (0, 0, 0))]},
            "right_arm": {"rotation": [(0.0, (-72, -8, -8)), (0.35, (-108, -12, -10)), (0.75, (-30, -4, -3)), (1.1, (52, 10, 5)), (1.6, (12, 3, 1))]},
            "right_sword": {"rotation": [(0.0, (0, 0, 0)), (0.35, (-8, 0, 0)), (0.75, (8, 0, 0)), (1.1, (16, 0, 0)), (1.6, (3, 0, 0))]},
            "left_arm": {"rotation": [(0.0, (-5, 0, 2)), (0.35, (-8, 0, 3)), (0.75, (-4, 0, 2)), (1.1, (4, 0, 0)), (1.6, (0, 0, 0))]},
        },
    },
    "defend": {
        "duration": 2.0,
        "loop": False,
        "bones": {
            "body": {"rotation": [(0.0, (0, 0, 0)), (0.4, (0, 4, 0)), (1.2, (0, 5, 0)), (2.0, (0, 0, 0))]},
            "left_arm": {"rotation": [(0.0, (0, 0, 0)), (0.4, (-32, 0, 4)), (1.2, (-38, 0, 5)), (2.0, (0, 0, 0))]},
            "left_shield": {
                "rotation": [(0.0, (0, 0, 0)), (0.4, (32, 0, -4)), (1.2, (38, 0, -5)), (2.0, (0, 0, 0))],
                "position": [(0.0, (0, 0, 0)), (0.4, (0, -0.8, -1.1)), (1.2, (0, -0.9, -1.2)), (2.0, (0, 0, 0))],
            },
            "right_arm": {"rotation": [(0.0, (0, 0, 0)), (0.4, (-10, 0, -2)), (1.2, (-12, 0, -3)), (2.0, (0, 0, 0))]},
        },
    },
    "hurt": {
        "duration": 0.5,
        "loop": False,
        "bones": {
            "body": {"rotation": [(0.0, (0, 0, 0)), (0.08, (-8, 0, 7)), (0.22, (-13, 0, 10)), (0.5, (0, 0, 0))]},
            "head": {"rotation": [(0.0, (0, 0, 0)), (0.08, (5, 0, -4)), (0.22, (8, 0, -7)), (0.5, (0, 0, 0))]},
            "right_arm": {"rotation": [(0.0, (0, 0, 0)), (0.08, (10, 0, -4)), (0.22, (16, 0, -6)), (0.5, (0, 0, 0))]},
            "left_arm": {"rotation": [(0.0, (0, 0, 0)), (0.08, (10, 0, 4)), (0.22, (16, 0, 6)), (0.5, (0, 0, 0))]},
        },
    },
    "death": {
        "duration": 1.0,
        "loop": False,
        "bones": {
            "root": {"rotation": [(0.0, (0, 0, 0)), (0.4, (0, 0, -28)), (1.0, (0, 0, -72))], "position": [(0.0, (0, 0, 0)), (0.4, (0, -0.4, 0)), (1.0, (0, -1.0, 0))]},
            "body": {"rotation": [(0.0, (0, 0, 0)), (0.4, (8, 0, -4)), (1.0, (22, 0, -10))]},
            "head": {"rotation": [(0.0, (0, 0, 0)), (0.4, (10, 0, -8)), (1.0, (24, 0, -18))]},
            "right_arm": {"rotation": [(0.0, (0, 0, 0)), (0.4, (18, 0, -12)), (1.0, (38, 0, -32))]},
            "left_arm": {"rotation": [(0.0, (0, 0, 0)), (0.4, (18, 0, 12)), (1.0, (38, 0, 32))]},
            "right_leg": {"rotation": [(0.0, (0, 0, 0)), (0.4, (10, 0, 0)), (1.0, (22, 0, 0))]},
            "left_leg": {"rotation": [(0.0, (0, 0, 0)), (0.4, (-10, 0, 0)), (1.0, (-22, 0, 0))]},
        },
    },
}


def native_animation_json() -> dict[str, object]:
    animations: dict[str, object] = {}
    for clip_name, clip in CLIPS.items():
        bones: dict[str, object] = {}
        for bone_name, tracks in dict(clip["bones"]).items():  # type: ignore[arg-type]
            bone_tracks: dict[str, object] = {}
            for channel in ("rotation", "position"):
                if channel in tracks:
                    bone_tracks[channel] = {str(time): vector(values) for time, values in tracks[channel]}
            bones[bone_name] = bone_tracks
        animations[f"animation.midland_guard.{clip_name}"] = {
            "loop": bool(clip["loop"]),
            "animation_length": float(clip["duration"]),
            "bones": bones,
        }
    return {"format_version": "1.8.0", "animations": animations}


def bb_keyframe(clip_name: str, bone_name: str, channel: str, time: float, values: tuple[float, float, float]) -> dict[str, object]:
    return {
        "channel": channel,
        "time": fnum(time),
        "uuid": stable_uuid("keyframe", f"{clip_name}/{bone_name}/{channel}/{time}"),
        "interpolation": "linear",
        "data_points": [{"x": str(fnum(values[0])), "y": str(fnum(values[1])), "z": str(fnum(values[2]))}],
    }


def bb_animations() -> list[dict[str, object]]:
    animations: list[dict[str, object]] = []
    for clip_name, clip in CLIPS.items():
        full_name = f"animation.midland_guard.{clip_name}"
        animators: dict[str, object] = {}
        for bone_name, tracks in dict(clip["bones"]).items():  # type: ignore[arg-type]
            if bone_name not in BONE_NAMES:
                raise ValueError(f"animation references unknown bone {bone_name}")
            keyframes = []
            for channel in ("rotation", "position"):
                keyframes.extend(bb_keyframe(full_name, bone_name, channel, time, values) for time, values in tracks.get(channel, []))
            keyframes.sort(key=lambda key: (float(key["time"]), str(key["channel"])))
            animators[stable_uuid("bone", bone_name)] = {"name": bone_name, "type": "bone", "keyframes": keyframes}
        animations.append({
            "name": full_name,
            "uuid": stable_uuid("animation", full_name),
            "loop": "loop" if bool(clip["loop"]) else "once",
            "override": False,
            "length": float(clip["duration"]),
            "snapping": 20,
            "animators": animators,
        })
    return animations


def paint_texture() -> Image.Image:
    image = Image.new("RGBA", (ATLAS_SIZE, ATLAS_SIZE), (15, 16, 17, 255))
    draw = ImageDraw.Draw(image)
    for material, (left, top, right, bottom) in MATERIAL_REGIONS.items():
        base = MATERIAL_COLORS[material]
        # Clean two-tone vertical gradient: readable in-game without random
        # checkerboards or per-pixel camouflage.
        for y in range(top, bottom):
            t = (y - top) / max(1, bottom - top - 1)
            color = tuple(round(base[i] * (1.0 - 0.16 * t) + min(255, base[i] + 20) * (0.16 * t)) for i in range(3))
            draw.line((left, y, right - 1, y), fill=(*color, 255))
        if material in {"steel", "steel_dark", "steel_edge", "steel_bright"}:
            draw.line((left + 11, top + 12, left + 14, top + 11), fill=(*tuple(min(255, c + 14) for c in base), 255), width=1)
            draw.line((left + 18, top + 22, left + 20, top + 22), fill=(*tuple(max(0, c - 12) for c in base), 255), width=1)
            draw.line((left + 2, top + 3, right - 4, top + 3), fill=(*tuple(min(255, c + 22) for c in base), 255), width=1)
        elif material == "mail":
            for row in range(top + 5, bottom - 2, 8):
                for col in range(left + 4 + ((row - top) // 8 % 2) * 4, right - 2, 8):
                    draw.arc((col - 3, row - 3, col + 3, row + 3), 25, 205, fill=(119, 121, 111, 220), width=1)
                    draw.arc((col - 2, row - 2, col + 2, row + 2), 210, 350, fill=(24, 27, 27, 230), width=1)
        elif material in {"tunic", "burgundy"}:
            draw.polygon([(left + 2, top + 4), (left + 13, top + 1), (right - 4, bottom - 4), (left + 8, bottom - 1)], fill=(*tuple(min(255, channel + 10) for channel in base), 255))
            draw.line((left + 3, bottom - 5, right - 2, bottom - 3), fill=(*tuple(max(0, channel - 18) for channel in base), 255), width=2)
        elif material in {"leather", "boot"}:
            draw.line((left + 3, top + 6, right - 3, top + 3), fill=(*tuple(min(255, channel + 25) for channel in base), 210), width=2)
            draw.line((left + 3, bottom - 7, right - 5, bottom - 3), fill=(*tuple(max(0, channel - 22) for channel in base), 220), width=2)
            draw.rectangle((left + 12, top + 13, left + 19, top + 16), outline=(*tuple(min(255, channel + 34) for channel in base), 210), width=1)
        elif material.startswith("wood"):
            draw.line((left + 6, top + 1, left + 3, bottom - 2), fill=(*tuple(max(0, channel - 20) for channel in base), 220), width=2)
            draw.line((left + 17, top + 2, left + 14, bottom - 1), fill=(*tuple(min(255, channel + 20) for channel in base), 180), width=1)
            draw.line((left + 25, top + 1, left + 22, bottom - 2), fill=(*tuple(max(0, channel - 15) for channel in base), 180), width=1)
        elif material == "slit":
            draw.rectangle((left, top, right - 1, bottom - 1), fill=(*base, 255))
            draw.line((left + 1, top + 2, right - 2, top + 2), fill=(32, 34, 34, 255), width=1)
    return image


def bb_texture_source(image: Image.Image) -> str:
    buffer = BytesIO()
    image.save(buffer, format="PNG", optimize=False, compress_level=9)
    return "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode("ascii")


def bb_face_uv(cube: Cube, face: str) -> list[int | float]:
    data = cube_face_uv(cube)[face]
    x, y = data["uv"]
    w, h = data["uv_size"]
    return [fnum(x), fnum(y), fnum(float(x) + float(w)), fnum(float(y) + float(h))]


def bb_element(cube: Cube) -> dict[str, object]:
    x, y, z = cube.origin
    w, h, d = cube.size
    return {
        "name": cube.name,
        "type": "cube",
        "uuid": stable_uuid("cube", cube.name),
        "from": vector((x, y, z)),
        "to": vector((x + w, y + h, z + d)),
        "origin": vector(cube.pivot or BONE_PIVOTS[cube.bone]),
        "rotation": vector(cube.rotation),
        "faces": {face: {"uv": bb_face_uv(cube, face), "texture": 0, "rotation": 0} for face in ("north", "south", "east", "west", "up", "down")},
        "autouv": 0,
        "color": 0,
        "shade": True,
    }


def bb_bone_node(name: str, children_by_bone: dict[str, list[str]], child_bones: dict[str, list[str]]) -> dict[str, object]:
    children: list[object] = list(children_by_bone.get(name, []))
    children.extend(bb_bone_node(child, children_by_bone, child_bones) for child in child_bones.get(name, []))
    pivot = BONE_PIVOTS[name]
    return {
        "name": name,
        "uuid": stable_uuid("bone", name),
        "origin": vector(pivot),
        "color": 0,
        "export": True,
        "isOpen": True,
        "children": children,
    }


def bbmodel_json(image: Image.Image) -> dict[str, object]:
    children_by_bone: dict[str, list[str]] = {name: [] for name in BONE_NAMES}
    for cube in CUBES:
        children_by_bone[cube.bone].append(stable_uuid("cube", cube.name))
    child_bones: dict[str, list[str]] = {name: [] for name in BONE_NAMES}
    for bone in BONES:
        parent = bone["parent"]
        if parent is not None:
            child_bones[str(parent)].append(str(bone["name"]))
    root_children = [bb_bone_node(str(bone["name"]), children_by_bone, child_bones) for bone in BONES if bone["parent"] is None]
    return {
        "meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": False},
        "name": "midland_guard_closed_helm",
        "resolution": {"width": ATLAS_SIZE, "height": ATLAS_SIZE},
        "elements": [bb_element(cube) for cube in CUBES],
        "outliner": root_children,
        "textures": [{
            "name": "midland_guard.png",
            "path": str(RUNTIME_TEXTURE_PATH),
            "relative_path": "../../src/main/resources/assets/struggler/textures/entity/midland_guard.png",
            "id": "0",
            "uuid": stable_uuid("texture", "midland_guard.png"),
            "width": ATLAS_SIZE,
            "height": ATLAS_SIZE,
            "uv_width": ATLAS_SIZE,
            "uv_height": ATLAS_SIZE,
            "file_name": "midland_guard.png",
            "file_format": "png",
            "source": bb_texture_source(image),
        }],
        "animations": bb_animations(),
    }


def validate_outputs(geo: dict[str, object], animation: dict[str, object], bbmodel: dict[str, object]) -> tuple[tuple[float, float, float], tuple[float, float, float]]:
    assert len(CUBES) == 52
    assert geo["format_version"] == "1.12.0"
    geometry = geo["minecraft:geometry"][0]  # type: ignore[index]
    description = geometry["description"]  # type: ignore[index]
    assert description["identifier"] == "geometry.midland_guard"
    assert description["texture_width"] == ATLAS_SIZE and description["texture_height"] == ATLAS_SIZE
    assert description["visible_bounds_width"] == 3.0
    assert description["visible_bounds_height"] == 3.0
    assert description["visible_bounds_offset"] == [0, 1, 0]
    assert animation["format_version"] == "1.8.0"
    assert set(animation["animations"]) == {f"animation.midland_guard.{name}" for name in CLIPS}
    assert len(bbmodel["animations"]) == 7
    assert bbmodel["meta"]["model_format"] == "bedrock"

    global_min = [float("inf")] * 3
    global_max = [float("-inf")] * 3
    cubes_by_name = {cube.name: cube for cube in CUBES}
    for cube in CUBES:
        assert min(cube.size) > 0
        assert cube.material in MATERIAL_REGIONS
        for face, uv_data in cube_face_uv(cube).items():
            uv = uv_data["uv"]
            uv_size = uv_data["uv_size"]
            assert uv[0] >= 0 and uv[1] >= 0
            assert float(uv[0]) + float(uv_size[0]) <= ATLAS_SIZE
            assert float(uv[1]) + float(uv_size[1]) <= ATLAS_SIZE
            expected = (cube.size[0] * 2, cube.size[1] * 2) if face in {"north", "south"} else (cube.size[2] * 2, cube.size[1] * 2) if face in {"east", "west"} else (cube.size[0] * 2, cube.size[2] * 2)
            assert math.isclose(float(uv_size[0]), expected[0], abs_tol=0.001)
            assert math.isclose(float(uv_size[1]), expected[1], abs_tol=0.001)
        for axis in range(3):
            global_min[axis] = min(global_min[axis], cube.origin[axis])
            global_max[axis] = max(global_max[axis], cube.origin[axis][0] if False else cube.origin[axis] + cube.size[axis])
        assert cube.origin[1] >= 0, f"{cube.name} sinks below feet"
    assert cubes_by_name["sword_tip"].origin[1] >= 0
    assert cubes_by_name["head_closed_helm_shell"].origin == (-4, 24, -4)
    assert cubes_by_name["body_dark_tunic"].origin == (-4, 12, -2)
    assert cubes_by_name["right_arm_vanilla"].origin == (-8, 12, -2)
    assert cubes_by_name["left_arm_vanilla"].origin == (4, 12, -2)
    assert cubes_by_name["right_leg_vanilla"].origin == (-3.9, 0, -2)
    assert cubes_by_name["left_leg_vanilla"].origin == (-0.1, 0, -2)
    return tuple(global_min), tuple(global_max)


def write_outputs() -> None:
    ART_DIR.mkdir(parents=True, exist_ok=True)
    GEO_PATH.parent.mkdir(parents=True, exist_ok=True)
    ANIMATION_PATH.parent.mkdir(parents=True, exist_ok=True)
    RUNTIME_TEXTURE_PATH.parent.mkdir(parents=True, exist_ok=True)

    image = paint_texture()
    image.save(ART_TEXTURE_PATH, format="PNG", optimize=False, compress_level=9)
    image.save(RUNTIME_TEXTURE_PATH, format="PNG", optimize=False, compress_level=9)

    geometry = geo_json()
    animation = native_animation_json()
    bbmodel = bbmodel_json(image)
    bounds = validate_outputs(geometry, animation, bbmodel)

    GEO_PATH.write_text(json.dumps(geometry, indent=2) + "\n", encoding="utf-8")
    ANIMATION_PATH.write_text(json.dumps(animation, indent=2) + "\n", encoding="utf-8")
    BBMODEL_PATH.write_text(json.dumps(bbmodel, indent=2) + "\n", encoding="utf-8")

    # Re-read the files and validate the serialized artifacts too.
    serialized_geo = json.loads(GEO_PATH.read_text(encoding="utf-8"))
    serialized_animation = json.loads(ANIMATION_PATH.read_text(encoding="utf-8"))
    serialized_bbmodel = json.loads(BBMODEL_PATH.read_text(encoding="utf-8"))
    validate_outputs(serialized_geo, serialized_animation, serialized_bbmodel)

    print(f"generated {len(CUBES)} cubes, {len(BONES)} bones, {ATLAS_SIZE}x{ATLAS_SIZE} atlas")
    print(f"bounds min={list(bounds[0])} max={list(bounds[1])}")
    print("rest pose: closed opaque greathelm; slit is the only black face opening; sword tip remains above Y=0")
    print("shield: compact stepped heater silhouette; negative-Z front has wood, iron rim, burgundy stripe, and boss; grip/strap are rear volumes")
    print(f"geo: {GEO_PATH}")
    print(f"animations: {ANIMATION_PATH}")
    print(f"bbmodel: {BBMODEL_PATH}")
    print(f"texture: {RUNTIME_TEXTURE_PATH}")


if __name__ == "__main__":
    write_outputs()
