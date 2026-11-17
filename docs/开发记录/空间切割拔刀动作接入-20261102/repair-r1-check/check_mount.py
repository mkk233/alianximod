# 对照 1.20.1 renderItem 的完整手持变换，检查挂点而不是只套平移常数。
# ItemTransform.apply：平移，再 Quaternionf.rotationXYZ，再缩放。
# ItemRenderer.render 随后 translate(-0.5, -0.5, -0.5)。模型中心因此落到展示平移上。
# 右手、leftHand=false，不镜像。缩放保留，只改变长度，不改变轴和中心。
import json
import math
from pathlib import Path

ROOT = Path(r"D:/alianximod")
TRACKS = ROOT / "src/main/resources/assets/xueximod/actions/space_cut_left_waist_v5.json"
MOUNT = ROOT / "src/main/java/com/bslx/XueXiMod/client/action/spacecut/SpaceCutItemMount.java"
TIMES = (0.0, 0.48, 0.60, 1.60)
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


def mul(left, right):
    return (
        left[3] * right[0] + left[0] * right[3] + left[1] * right[2] - left[2] * right[1],
        left[3] * right[1] - left[0] * right[2] + left[1] * right[3] + left[2] * right[0],
        left[3] * right[2] + left[0] * right[1] - left[1] * right[0] + left[2] * right[3],
        left[3] * right[3] - left[0] * right[0] - left[1] * right[1] - left[2] * right[2],
    )


def rotate(quaternion, vector):
    quat_x, quat_y, quat_z, quat_w = quaternion
    coordinate_x, coordinate_y, coordinate_z = vector
    temp_x = 2.0 * (quat_y * coordinate_z - quat_z * coordinate_y)
    temp_y = 2.0 * (quat_z * coordinate_x - quat_x * coordinate_z)
    temp_z = 2.0 * (quat_x * coordinate_y - quat_y * coordinate_x)
    return (
        coordinate_x + quat_w * temp_x + (quat_y * temp_z - quat_z * temp_y),
        coordinate_y + quat_w * temp_y + (quat_z * temp_x - quat_x * temp_z),
        coordinate_z + quat_w * temp_z + (quat_x * temp_y - quat_y * temp_x),
    )


def axis_quat(axis, degrees):
    half = math.radians(degrees) * 0.5
    sine = math.sin(half)
    return (axis[0] * sine, axis[1] * sine, axis[2] * sine, math.cos(half))


def rotation_xyz(pitch, yaw, roll):
    # JOML rotationXYZ：先 Rz，再 Ry，再 Rx。
    return mul(mul(axis_quat((1, 0, 0), pitch), axis_quat((0, 1, 0), yaw)), axis_quat((0, 0, 1), roll))


def zyx(pitch, yaw, roll):
    half_pitch = math.radians(pitch) * 0.5
    half_yaw = math.radians(yaw) * 0.5
    half_roll = math.radians(roll) * 0.5
    cos_pitch, sin_pitch = math.cos(half_pitch), math.sin(half_pitch)
    cos_yaw, sin_yaw = math.cos(half_yaw), math.sin(half_yaw)
    cos_roll, sin_roll = math.cos(half_roll), math.sin(half_roll)
    return (
        sin_pitch * cos_yaw * cos_roll - cos_pitch * sin_yaw * sin_roll,
        cos_pitch * sin_yaw * cos_roll + sin_pitch * cos_yaw * sin_roll,
        cos_pitch * cos_yaw * sin_roll - sin_pitch * sin_yaw * cos_roll,
        cos_pitch * cos_yaw * cos_roll + sin_pitch * sin_yaw * sin_roll,
    )


def sample(track, time, channel):
    times = track["times"]
    values = track[channel]
    if time <= times[0]:
        return values[0]
    if time >= times[-1]:
        return values[-1]
    slot = 0
    while slot < len(times) - 1 and time > times[slot + 1]:
        slot += 1
    span = times[slot + 1] - times[slot]
    weight = (time - times[slot]) / span if span else 0.0
    start = values[slot]
    toward = values[slot + 1]
    return [start[axis] + (toward[axis] - start[axis]) * weight for axis in range(3)]


def solve(bones, name, time, cache):
    if name in cache:
        return cache[name]
    track = bones[name]
    rotation = sample(track, time, "rotation")
    shift = sample(track, time, "position") if "position" in track else [0.0, 0.0, 0.0]
    pivot = PIVOTS[name]
    local = zyx(*rotation)
    local_point = (pivot[0] + shift[0], pivot[1] + shift[1], pivot[2] + shift[2])
    parent = PARENTS.get(name)
    if parent is None:
        solved = (local_point, local)
    else:
        parent_point, parent_rotation = solve(bones, parent, time, cache)
        turned = rotate(parent_rotation, local_point)
        solved = (
            (parent_point[0] + turned[0], parent_point[1] + turned[1], parent_point[2] + turned[2]),
            mul(parent_rotation, local),
        )
    cache[name] = solved
    return solved


def model_matrix(quaternion):
    quat_x, quat_y, quat_z, quat_w = quaternion[0], -quaternion[1], quaternion[2], quaternion[3]
    length = math.sqrt(quat_x * quat_x + quat_y * quat_y + quat_z * quat_z + quat_w * quat_w)
    quat_x, quat_y, quat_z, quat_w = quat_x / length, quat_y / length, quat_z / length, quat_w / length
    product_xy = quat_x * quat_y
    product_xz = quat_x * quat_z
    product_xw = quat_x * quat_w
    product_yz = quat_y * quat_z
    product_yw = quat_y * quat_w
    product_zw = quat_z * quat_w
    doubled_x = 2.0 * quat_x * quat_x
    doubled_y = 2.0 * quat_y * quat_y
    doubled_z = 2.0 * quat_z * quat_z
    element00 = 1.0 - doubled_y - doubled_z
    element01 = 2.0 * (product_xy - product_zw)
    element02 = 2.0 * (product_xz + product_yw)
    element10 = 2.0 * (product_xy + product_zw)
    element11 = 1.0 - doubled_x - doubled_z
    element12 = 2.0 * (product_yz - product_xw)
    element20 = 2.0 * (product_xz - product_yw)
    element21 = 2.0 * (product_yz + product_xw)
    element22 = 1.0 - doubled_x - doubled_y
    return (
        (element00, element10, element20),
        (element01, element11, element21),
        (element02, element12, element22),
    )


def columns_from_quat(quaternion):
    return tuple(rotate(quaternion, axis) for axis in ((1, 0, 0), (0, 1, 0), (0, 0, 1)))


def mat_from_columns(columns):
    return tuple(tuple(columns[col][row] for col in range(3)) for row in range(3))


def mul_mat(left, right):
    return tuple(
        tuple(sum(left[row][k] * right[k][col] for k in range(3)) for col in range(3))
        for row in range(3)
    )


def apply_mat(matrix, vector):
    return tuple(sum(matrix[row][col] * vector[col] for col in range(3)) for row in range(3))


def transpose(matrix):
    return tuple(tuple(matrix[row][col] for row in range(3)) for col in range(3))


def add(left, right):
    return tuple(left[axis] + right[axis] for axis in range(3))


def sub(left, right):
    return tuple(left[axis] - right[axis] for axis in range(3))


def scale(vector, factor):
    return tuple(component * factor for component in vector)


def length(vector):
    return math.sqrt(sum(component * component for component in vector))


def angle(left, right):
    denom = length(left) * length(right)
    if denom < 1.0e-12:
        return 180.0
    cosine = max(-1.0, min(1.0, sum(left[axis] * right[axis] for axis in range(3)) / denom))
    return math.degrees(math.acos(cosine))


def flip_camera(vector):
    return (-vector[0], -vector[1], vector[2])


def camera_point(point):
    relative_x = point[0]
    relative_y = point[1] - (-1.92)
    relative_z = point[2] - (-4.0)
    pixel = 0.0625
    return (-relative_x * pixel, -relative_y * pixel, relative_z * pixel)


def display(kind):
    if kind == "third":
        rotation = rotation_xyz(0.0, -90.0, 55.0)
        shift = (0.0 / 16.0, 4.0 / 16.0, 0.5 / 16.0)
        item_scale = 0.85
    else:
        rotation = rotation_xyz(0.0, -90.0, 25.0)
        shift = (1.13 / 16.0, 3.2 / 16.0, 1.13 / 16.0)
        item_scale = 0.68
    return rotation, shift, item_scale


def mount_linear(target, source, inverse):
    # inverse True：T·R⁻¹。False：旧的 T·R。
    right = transpose(source) if inverse else source
    return mul_mat(target, right)


def rx(degrees):
    cosine = math.cos(math.radians(degrees))
    sine = math.sin(math.radians(degrees))
    return ((1.0, 0.0, 0.0), (0.0, cosine, -sine), (0.0, sine, cosine))


def ry(degrees):
    cosine = math.cos(math.radians(degrees))
    sine = math.sin(math.radians(degrees))
    return ((cosine, 0.0, sine), (0.0, 1.0, 0.0), (-sine, 0.0, cosine))


def main():
    bones = json.loads(TRACKS.read_text(encoding="utf-8"))["bones"]
    source_text = MOUNT.read_text(encoding="utf-8")
    lines = []
    branch_marks = [
        "isSword(player.getMainHandItem())",
        "isSword(player.getOffhandItem())",
        "sourceHand != hand",
        "sourceArm != arm",
        "rightSide && sourceHand != null",
        "ItemDisplayContext.FIRST_PERSON_RIGHT_HAND",
        "ItemDisplayContext.THIRD_PERSON_RIGHT_HAND",
        "player.getMainArm().getOpposite()",
    ]
    for mark in branch_marks:
        lines.append(f"branch {mark}: {'kept' if mark in source_text else 'MISSING'}")
    lines.append("")
    for kind in ("third", "first"):
        rotation, shift, item_scale = display(kind)
        source_columns = columns_from_quat(rotation)
        lines.append(
            f"{kind} display X {tuple(round(v, 8) for v in source_columns[0])} "
            f"Y {tuple(round(v, 8) for v in source_columns[1])} scale {item_scale}"
        )
        for time in TIMES:
            point, quaternion = solve(bones, "SwordHand", time, {})
            model = model_matrix(quaternion)
            side = (model[0][0], model[1][0], model[2][0])
            blade = (-model[0][1], -model[1][1], -model[2][1])
            if kind == "first":
                side = flip_camera(side)
                blade = flip_camera(blade)
                palm = camera_point((point[0], 24.0 - point[1], point[2]))
            else:
                palm = scale((point[0], 24.0 - point[1], point[2]), 0.0625)
            third = (
                side[1] * blade[2] - side[2] * blade[1],
                side[2] * blade[0] - side[0] * blade[2],
                side[0] * blade[1] - side[1] * blade[0],
            )
            target = mat_from_columns((side, blade, third))
            source = mat_from_columns(source_columns)
            fixed = mount_linear(target, source, True)
            broken = mount_linear(target, source, False)
            for label, linear, cancel_shift in (("fixed", fixed, True), ("open", fixed, False), ("old", broken, False)):
                final = mul_mat(linear, source)
                final_side = apply_mat(final, (item_scale, 0.0, 0.0))
                final_blade = apply_mat(final, (0.0, item_scale, 0.0))
                centered = (0.0, 0.0, 0.0)
                displayed = add(apply_mat(source, scale(centered, item_scale)), shift)
                carried = apply_mat(linear, shift)
                anchor = sub(palm, carried) if cancel_shift else palm
                grip = add(anchor, apply_mat(linear, displayed))
                offset = sub(grip, palm)
                blade_length = length(final_blade)
                blade_unit = scale(final_blade, 1.0 / blade_length)
                along = sum(offset[axis] * blade_unit[axis] for axis in range(3))
                perpendicular = length(sub(offset, scale(blade_unit, along))) * 16.0
                lines.append(
                    f"{kind} t={time:.2f} {label} blade {angle(final_blade, blade):.6f} "
                    f"normal {angle(final_side, side):.6f} "
                    f"center {length(offset) * 16:.6f}px axis {perpendicular:.6f}px"
                )
    lines.append("")
    cases = [
        (60.0, 0.0, 0.0, 0.0, 0.0),
        (-30.0, 120.0, 10.0, -20.0, 40.0),
        (0.0, 0.0, 0.0, 0.0, 0.0),
        (85.0, -170.0, 40.0, 15.0, 90.0),
    ]
    for look_pitch, look_yaw, pitch_bob, yaw_bob, body_yaw in cases:
        pitch_residual = (look_pitch - pitch_bob) * 0.1
        yaw_residual = (look_yaw - yaw_bob) * 0.1
        body_delta = (body_yaw - look_yaw + 180.0) % 360.0 - 180.0
        parent = mul_mat(rx(pitch_residual), ry(yaw_residual))
        intended = mul_mat(rx(look_pitch), ry(body_delta))
        cancel = mul_mat(ry(-yaw_residual), rx(-pitch_residual))
        ours = mul_mat(mul_mat(cancel, rx(look_pitch)), ry(body_delta))
        total = mul_mat(parent, ours)
        wrong = mul_mat(mul_mat(rx(-pitch_residual), ry(-yaw_residual)), intended)
        wrong_total = mul_mat(parent, wrong)
        probe = (0.2, 0.7, -0.4)
        lines.append(
            f"view pitch {look_pitch} yaw {look_yaw} bob {pitch_bob},{yaw_bob} body {body_yaw} "
            f"cancel {angle(apply_mat(total, probe), apply_mat(intended, probe)):.6f} "
            f"reversed {angle(apply_mat(wrong_total, probe), apply_mat(intended, probe)):.6f}"
        )
    text = "\n".join(lines) + "\n"
    out = Path(__file__).with_name("check_mount.txt")
    out.write_text(text, encoding="utf-8")
    print(text)


if __name__ == "__main__":
    main()
