"""Author numeric check for the v6 grip mount. Replays the runtime tracks and the vanilla display chain."""
import json
import math
from pathlib import Path

ROOT = Path(r"D:/alianximod")
TRACKS = ROOT / "src/main/resources/assets/xueximod/actions/space_cut_grip_fast_v6.json"
PIXEL = 0.0625
GRIP_Q = (3.5 / 16.0, 3.5 / 16.0, 0.5)
TIP_Q = (15.5 / 16.0, 15.5 / 16.0, 0.5)
CENTER_Q = (0.5, 0.5, 0.5)
MESH_BLADE = (0.7071067811865476, 0.7071067811865476, 0.0)
MESH_EDGE = (0.7071067811865476, -0.7071067811865476, 0.0)
MESH_FLAT = (0.0, 0.0, 1.0)
GRIP_CENTER = (3.5 / 16.0 - 0.5, 3.5 / 16.0 - 0.5, 0.0)
THIRD_EDGE = (0.0, 0.1736481777, 0.9848077530)
THIRD_BLADE = (0.0, 0.9848077530, -0.1736481777)
FIRST_EDGE = (0.0, -0.3420201433, 0.9396926208)
FIRST_BLADE = (0.0, 0.9396926208, 0.3420201433)
THIRD_SHIFT = (0.0, 4.0 / 16.0, 0.5 / 16.0)
FIRST_SHIFT = (1.13 / 16.0, 3.2 / 16.0, 1.13 / 16.0)
PARENTS = {
    "Head": "Body",
    "ArmRight": "Body",
    "ArmLeft": "Body",
    "ForearmRight": "ArmRight",
    "SwordHand": "ForearmRight",
    "ForearmLeft": "ArmLeft",
}
PIVOTS = {
    "Body": (0.0, 12.0, 0.0),
    "Head": (0.0, 12.0, 0.0),
    "ArmRight": (-6.0, 12.0, 0.0),
    "ForearmRight": (0.0, -6.0, 0.0),
    "SwordHand": (0.0, -6.0, 0.0),
    "ArmLeft": (6.0, 12.0, 0.0),
    "ForearmLeft": (0.0, -6.0, 0.0),
    "LegRight": (-2.0, 12.0, 0.0),
    "LegLeft": (2.0, 12.0, 0.0),
}


def add(left, right):
    return (left[0] + right[0], left[1] + right[1], left[2] + right[2])


def sub(left, right):
    return (left[0] - right[0], left[1] - right[1], left[2] - right[2])


def scale(vector, factor):
    return (vector[0] * factor, vector[1] * factor, vector[2] * factor)


def dot(left, right):
    return left[0] * right[0] + left[1] * right[1] + left[2] * right[2]


def cross(left, right):
    return (
        left[1] * right[2] - left[2] * right[1],
        left[2] * right[0] - left[0] * right[2],
        left[0] * right[1] - left[1] * right[0],
    )


def length(vector):
    return math.sqrt(dot(vector, vector))


def unit(vector):
    magnitude = length(vector)
    return scale(vector, 1.0 / magnitude)


def ry(vector, degrees):
    theta = math.radians(degrees)
    cosine = math.cos(theta)
    sine = math.sin(theta)
    return (
        cosine * vector[0] + sine * vector[2],
        vector[1],
        -sine * vector[0] + cosine * vector[2],
    )


def rz(vector, degrees):
    theta = math.radians(degrees)
    cosine = math.cos(theta)
    sine = math.sin(theta)
    return (
        cosine * vector[0] - sine * vector[1],
        sine * vector[0] + cosine * vector[1],
        vector[2],
    )


def display_rotate(vector, z_degrees):
    return ry(rz(vector, z_degrees), -90.0)


def blockbench_zyx(pitch, yaw, roll):
    half_pitch = math.radians(pitch) * 0.5
    half_yaw = math.radians(yaw) * 0.5
    half_roll = math.radians(roll) * 0.5
    cos_pitch = math.cos(half_pitch)
    sin_pitch = math.sin(half_pitch)
    cos_yaw = math.cos(half_yaw)
    sin_yaw = math.sin(half_yaw)
    cos_roll = math.cos(half_roll)
    sin_roll = math.sin(half_roll)
    return (
        sin_pitch * cos_yaw * cos_roll - cos_pitch * sin_yaw * sin_roll,
        cos_pitch * sin_yaw * cos_roll + sin_pitch * cos_yaw * sin_roll,
        cos_pitch * cos_yaw * sin_roll - sin_pitch * sin_yaw * cos_roll,
        cos_pitch * cos_yaw * cos_roll + sin_pitch * sin_yaw * sin_roll,
    )


def product(left, right):
    lx, ly, lz, lw = left
    rx, ry_q, rz_q, rw = right
    return (
        lw * rx + lx * rw + ly * rz_q - lz * ry_q,
        lw * ry_q - lx * rz_q + ly * rw + lz * rx,
        lw * rz_q + lx * ry_q - ly * rx + lz * rw,
        lw * rw - lx * rx - ly * ry_q - lz * rz_q,
    )


def rotate(quaternion, vector):
    qx, qy, qz, qw = quaternion
    temp_x = 2.0 * (qy * vector[2] - qz * vector[1])
    temp_y = 2.0 * (qz * vector[0] - qx * vector[2])
    temp_z = 2.0 * (qx * vector[1] - qy * vector[0])
    return (
        vector[0] + qw * temp_x + (qy * temp_z - qz * temp_y),
        vector[1] + qw * temp_y + (qz * temp_x - qx * temp_z),
        vector[2] + qw * temp_z + (qx * temp_y - qy * temp_x),
    )


def matrix_from_quaternion(quaternion):
    qx, qy, qz, qw = quaternion
    flipped = (qx, -qy, qz, qw)
    square = dot(flipped[:3], flipped[:3]) + flipped[3] * flipped[3]
    inverse = 1.0 / math.sqrt(square)
    qx, qy, qz, qw = (component * inverse for component in flipped)
    product_xy = qx * qy
    product_xz = qx * qz
    product_xw = qx * qw
    product_yz = qy * qz
    product_yw = qy * qw
    product_zw = qz * qw
    doubled_x = 2.0 * qx * qx
    doubled_y = 2.0 * qy * qy
    doubled_z = 2.0 * qz * qz
    return {
        "m00": 1.0 - doubled_y - doubled_z,
        "m01": 2.0 * (product_xy - product_zw),
        "m02": 2.0 * (product_xz + product_yw),
        "m10": 2.0 * (product_xy + product_zw),
        "m11": 1.0 - doubled_x - doubled_z,
        "m12": 2.0 * (product_yz - product_xw),
        "m20": 2.0 * (product_xz - product_yw),
        "m21": 2.0 * (product_yz + product_xw),
        "m22": 1.0 - doubled_x - doubled_y,
        "m30": 0.0,
        "m31": 0.0,
        "m32": 0.0,
    }


def transform_point(matrix, point):
    px, py, pz = point
    return (
        matrix["m00"] * px + matrix["m10"] * py + matrix["m20"] * pz + matrix["m30"],
        matrix["m01"] * px + matrix["m11"] * py + matrix["m21"] * pz + matrix["m31"],
        matrix["m02"] * px + matrix["m12"] * py + matrix["m22"] * pz + matrix["m32"],
    )


def sample(values, times, time):
    if time <= times[0]:
        return values[0]
    last = len(times) - 1
    if time >= times[last]:
        return values[last]
    slot = 0
    while slot < last and time > times[slot + 1]:
        slot += 1
    span = times[slot + 1] - times[slot]
    weight = (time - times[slot]) / span if span > 0.0 else 0.0
    start = values[slot]
    toward = values[slot + 1]
    return [start[c] + (toward[c] - start[c]) * weight for c in range(3)]


def solve(tracks, bone_name, time, solved):
    if bone_name in solved:
        return solved[bone_name]
    track = tracks[bone_name]
    rotation = sample(track["rotation"], track["times"], time)
    position = sample(track["position"], track["times"], time) if "position" in track else [0.0, 0.0, 0.0]
    pivot = PIVOTS[bone_name]
    local_rotation = blockbench_zyx(rotation[0], rotation[1], rotation[2])
    local = (pivot[0] + position[0], pivot[1] + position[1], pivot[2] + position[2])
    parent_name = PARENTS.get(bone_name)
    if parent_name is None:
        solved_bone = (local, local_rotation)
    else:
        parent_point, parent_rotation = solve(tracks, parent_name, time, solved)
        turned = rotate(parent_rotation, local)
        solved_bone = (add(parent_point, turned), product(parent_rotation, local_rotation))
    solved[bone_name] = solved_bone
    return solved_bone


def write_mapped(mount, row, target_side, target_blade, target_third, source_side, source_blade, source_third):
    column_x = target_side[row] * source_side[0] + target_blade[row] * source_blade[0] + target_third[row] * source_third[0]
    column_y = target_side[row] * source_side[1] + target_blade[row] * source_blade[1] + target_third[row] * source_third[1]
    column_z = target_side[row] * source_side[2] + target_blade[row] * source_blade[2] + target_third[row] * source_third[2]
    if row == 0:
        mount["m00"], mount["m10"], mount["m20"] = column_x, column_y, column_z
    elif row == 1:
        mount["m01"], mount["m11"], mount["m21"] = column_x, column_y, column_z
    else:
        mount["m02"], mount["m12"], mount["m22"] = column_x, column_y, column_z


def mount_matrix(hand, camera_space, first_person):
    matrix = matrix_from_quaternion(hand[1])
    blade = (-matrix["m10"], -matrix["m11"], -matrix["m12"])
    side = (matrix["m00"], matrix["m01"], matrix["m02"])
    pivot = (hand[0][0], 24.0 - hand[0][1], hand[0][2])
    if camera_space:
        blade = (-blade[0], -blade[1], blade[2])
        side = (-side[0], -side[1], side[2])
        relative = (pivot[0] - 0.0, pivot[1] - (-1.92), pivot[2] - (-4.0))
        palm = (-relative[0] * PIXEL, -relative[1] * PIXEL, relative[2] * PIXEL)
    else:
        palm = scale(pivot, PIXEL)
    blade = unit(blade)
    along = dot(side, blade)
    side = sub(side, scale(blade, along))
    side = unit(side)
    third = cross(side, blade)
    source_side = FIRST_EDGE if first_person else THIRD_EDGE
    source_blade = FIRST_BLADE if first_person else THIRD_BLADE
    source_third = cross(source_side, source_blade)
    mount = {"m30": 0.0, "m31": 0.0, "m32": 0.0}
    for row in range(3):
        write_mapped(mount, row, side, blade, third, source_side, source_blade, source_third)
    display_shift = FIRST_SHIFT if first_person else THIRD_SHIFT
    display_scale = 0.68 if first_person else 0.85
    grip_on_edge = dot(GRIP_CENTER, MESH_EDGE)
    grip_on_blade = dot(GRIP_CENTER, MESH_BLADE)
    grip_on_flat = dot(GRIP_CENTER, MESH_FLAT)
    carried = transform_point(mount, display_shift)
    carried = add(carried, scale(side, display_scale * grip_on_edge))
    carried = add(carried, scale(blade, display_scale * grip_on_blade))
    carried = add(carried, scale(third, display_scale * grip_on_flat))
    mount["m30"] = palm[0] - carried[0]
    mount["m31"] = palm[1] - carried[1]
    mount["m32"] = palm[2] - carried[2]
    return mount, palm, blade


def vanilla_point(mount, point, z_degrees, shift, display_scale):
    centered = sub(point, (0.5, 0.5, 0.5))
    rotated = display_rotate(scale(centered, display_scale), z_degrees)
    return transform_point(mount, add(shift, rotated))


def main():
    tracks = json.loads(TRACKS.read_text(encoding="utf-8"))["bones"]
    lines = []
    worst_grip = 0.0
    worst_angle = 0.0
    for z_degrees, first_person, label in ((55.0, False, "third"), (25.0, True, "first")):
        lines.append(
            label
            + " constant-vs-rotation edge "
            + str(length(sub(display_rotate(MESH_EDGE, z_degrees), FIRST_EDGE if first_person else THIRD_EDGE)))
            + " blade "
            + str(length(sub(display_rotate(MESH_BLADE, z_degrees), FIRST_BLADE if first_person else THIRD_BLADE)))
            + " flat "
            + str(length(sub(display_rotate(MESH_FLAT, z_degrees), cross(FIRST_EDGE if first_person else THIRD_EDGE, FIRST_BLADE if first_person else THIRD_BLADE))))
        )
    for time in (0.0, 0.15, 0.1875, 0.5):
        hand = solve(tracks, "SwordHand", time, {})
        palm_model = (hand[0][0], 24.0 - hand[0][1], hand[0][2])
        lines.append(f"t={time:.4f} palm_model {palm_model[0]:.6f} {palm_model[1]:.6f} {palm_model[2]:.6f}")
        for z_degrees, first_person, label, shift, display_scale in (
            (55.0, False, "third", THIRD_SHIFT, 0.85),
            (25.0, True, "first", FIRST_SHIFT, 0.68),
        ):
            mount, palm, blade = mount_matrix(hand, first_person, first_person)
            grip = vanilla_point(mount, GRIP_Q, z_degrees, shift, display_scale)
            tip = vanilla_point(mount, TIP_Q, z_degrees, shift, display_scale)
            center = vanilla_point(mount, CENTER_Q, z_degrees, shift, display_scale)
            grip_error = length(sub(grip, palm))
            center_error = length(sub(center, palm))
            traveled = sub(tip, grip)
            cosine = max(-1.0, min(1.0, dot(unit(traveled), blade)))
            angle = math.degrees(math.acos(cosine))
            old_axis = unit(sub(
                vanilla_point(mount, (0.5, 1.0, 0.5), z_degrees, shift, display_scale),
                center,
            ))
            old_angle = math.degrees(math.acos(max(-1.0, min(1.0, dot(old_axis, blade)))))
            worst_grip = max(worst_grip, grip_error)
            worst_angle = max(worst_angle, angle)
            lines.append(
                f"  {label} grip {grip_error:.3e} center {center_error:.6f} "
                f"blade_deg {angle:.3e} old_plus_y_deg {old_angle:.3f} length {length(traveled):.6f}"
            )
    start = solve(tracks, "SwordHand", 0.0, {})
    start_error = length(sub(start[0], (5.3, 15.5, -3.6)))
    lines.append(f"start_palm_error {start_error:.6e}")
    lines.append(f"worst_grip {worst_grip:.3e} worst_blade_deg {worst_angle:.3e}")
    text = "\n".join(lines) + "\n"
    output = Path(__file__).with_name("check_grip.txt")
    output.write_text(text, encoding="utf-8")
    print(text)
    if start_error > 0.05 or worst_grip > 1.0e-5 or worst_angle > 1.0e-3:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
