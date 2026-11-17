import math
import struct
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(r"D:\alianximod")
BIN_PATH = ROOT / "src/main/resources/assets/xueximod/models/effect/water_phantom_steve_v21/lines.bin"
SKIN_PATH = ROOT / "src/main/resources/assets/xueximod/textures/entity/xueximod/steve_water_lines.png"
OUT_PATH = Path(__file__).with_name("fix-game-visual-03-offline-overlap.png")


def load_skin_pieces():
    blob = BIN_PATH.read_bytes()
    offset = 6
    pieces = []
    for _index in range(6):
        offset += 1
        pivot = struct.unpack_from(">fff", blob, offset)
        offset += 12
        quad_count = struct.unpack_from(">H", blob, offset)[0]
        offset += 2
        faces = []
        for _quad in range(quad_count):
            offset += 12
            corners = []
            for _vertex in range(4):
                corners.append(struct.unpack_from(">fffff", blob, offset))
                offset += 20
            faces.append(corners)
        for _track in range(3):
            key_count = struct.unpack_from(">H", blob, offset)[0]
            offset += 2 + key_count * 17
        pieces.append((pivot, faces))
    return pieces


def expand(pivot, faces, image):
    width, height = image.size
    pixels = image.load()
    points = []
    for face in faces:
        origin_u, origin_v = face[0][3], face[0][4]
        across_u, across_v = face[1][3] - origin_u, face[1][4] - origin_v
        down_u, down_v = face[3][3] - origin_u, face[3][4] - origin_v
        determinant = across_u * down_v - across_v * down_u
        if abs(determinant) < 1.0e-6:
            continue
        corner_u = [corner[3] for corner in face]
        corner_v = [corner[4] for corner in face]
        start_column = max(0, int(math.floor(min(corner_u) * width + 0.0001)))
        end_column = min(width, int(math.floor(max(corner_u) * width - 0.0001)) + 1)
        start_row = max(0, int(math.floor(min(corner_v) * height + 0.0001)))
        end_row = min(height, int(math.floor(max(corner_v) * height - 0.0001)) + 1)
        origin_x, origin_y, origin_z = face[0][0], face[0][1], face[0][2]
        across_x = face[1][0] - origin_x
        across_y = face[1][1] - origin_y
        across_z = face[1][2] - origin_z
        down_x = face[3][0] - origin_x
        down_y = face[3][1] - origin_y
        down_z = face[3][2] - origin_z
        for row in range(start_row, end_row):
            for column in range(start_column, end_column):
                if pixels[column, row][3] == 0:
                    continue
                center_u = (column + 0.5) / width
                center_v = (row + 0.5) / height
                relative_u = center_u - origin_u
                relative_v = center_v - origin_v
                along_across = (relative_u * down_v - relative_v * down_u) / determinant
                along_down = (across_u * relative_v - across_v * relative_u) / determinant
                points.append((
                    pivot[0] + origin_x + across_x * along_across + down_x * along_down,
                    pivot[1] + origin_y + across_y * along_across + down_y * along_down,
                    pivot[2] + origin_z + across_z * along_across + down_z * along_down,
                ))
    return points


def align(point):
    local_x, local_y, local_z = point[0], point[1] - 1.501, point[2]
    local_x, local_y = -local_x, -local_y
    theta = math.radians(180.0)
    cosine, sine = math.cos(theta), math.sin(theta)
    return (
        local_x * cosine + local_z * sine,
        local_y,
        -local_x * sine + local_z * cosine,
    )


def project(points, orbit_degrees, pitch_degrees, distance):
    pitch = math.radians(pitch_degrees)
    orbit = math.radians(orbit_degrees)
    camera = (
        distance * math.sin(orbit) * math.cos(pitch),
        1.15 + distance * math.sin(pitch),
        -distance * math.cos(orbit) * math.cos(pitch),
    )
    target = (0.0, 0.9, 0.0)
    forward = tuple(target[axis] - camera[axis] for axis in range(3))
    length = math.sqrt(sum(value * value for value in forward))
    forward = tuple(value / length for value in forward)
    world_up = (0.0, 1.0, 0.0)
    right = (
        forward[1] * world_up[2] - forward[2] * world_up[1],
        forward[2] * world_up[0] - forward[0] * world_up[2],
        forward[0] * world_up[1] - forward[1] * world_up[0],
    )
    right_length = math.sqrt(sum(value * value for value in right)) or 1.0
    right = tuple(value / right_length for value in right)
    up = (
        right[1] * forward[2] - right[2] * forward[1],
        right[2] * forward[0] - right[0] * forward[2],
        right[0] * forward[1] - right[1] * forward[0],
    )
    projected = []
    for point in points:
        placed = align(point)
        relative = tuple(placed[axis] - camera[axis] for axis in range(3))
        depth = sum(relative[axis] * forward[axis] for axis in range(3))
        depth = max(depth, 0.2)
        screen_x = sum(relative[axis] * right[axis] for axis in range(3)) / depth
        screen_y = -sum(relative[axis] * up[axis] for axis in range(3)) / depth
        projected.append((screen_x, screen_y))
    return projected


def outside_fraction(arm_points, body_points):
    body_min = min(point[0] for point in body_points)
    body_max = max(point[0] for point in body_points)
    outside = 0
    for point in arm_points:
        if point[0] < body_min or point[0] > body_max:
            outside += 1
    return outside / len(arm_points)


def inside_fraction(arm_points, leg_points):
    min_x = min(point[0] for point in leg_points)
    max_x = max(point[0] for point in leg_points)
    min_y = min(point[1] for point in leg_points)
    max_y = max(point[1] for point in leg_points)
    inside = 0
    for point in arm_points:
        if min_x <= point[0] <= max_x and min_y <= point[1] <= max_y:
            inside += 1
    return inside / len(arm_points)


def main():
    image = Image.open(SKIN_PATH).convert("RGBA")
    clouds = [expand(pivot, faces, image) for pivot, faces in load_skin_pieces()]
    print("best views where one arm leaves the body and the other falls on a leg")
    best = None
    for orbit in range(-80, 81, 10):
        for pitch in (20, 40, 60, 75):
            for distance in (2.2, 3.5, 5.0):
                projected = [project(clouds[index], orbit, pitch, distance) for index in range(6)]
                left_out = outside_fraction(projected[0], projected[2])
                right_out = outside_fraction(projected[1], projected[2])
                left_on_leg = max(inside_fraction(projected[0], projected[4]), inside_fraction(projected[0], projected[5]))
                right_on_leg = max(inside_fraction(projected[1], projected[4]), inside_fraction(projected[1], projected[5]))
                score = abs(left_out - right_out) + max(left_on_leg, right_on_leg)
                if best is None or score > best[0]:
                    best = (score, orbit, pitch, distance, left_out, right_out, left_on_leg, right_on_leg)
    print("best", best)
    for orbit, pitch, distance in ((0, 25, 4.0), (0, 60, 3.0), (best[1], best[2], best[3]), (-60, 65, 2.4), (60, 65, 2.4)):
        projected = [project(clouds[index], orbit, pitch, distance) for index in range(6)]
        print(
            "orbit", orbit, "pitch", pitch, "dist", distance,
            "Lout", round(outside_fraction(projected[0], projected[2]), 3),
            "Rout", round(outside_fraction(projected[1], projected[2]), 3),
            "LonLeg", round(max(inside_fraction(projected[0], projected[4]), inside_fraction(projected[0], projected[5])), 3),
            "RonLeg", round(max(inside_fraction(projected[1], projected[4]), inside_fraction(projected[1], projected[5])), 3),
        )
    orbit, pitch, distance = best[1], best[2], best[3]
    projected = [project(clouds[index], orbit, pitch, distance) for index in range(6)]
    colors = [(255, 70, 70), (70, 140, 255), (190, 230, 255), (255, 255, 255), (150, 150, 150), (90, 90, 90)]
    flat = [point for cloud in projected for point in cloud]
    min_x = min(point[0] for point in flat)
    max_x = max(point[0] for point in flat)
    min_y = min(point[1] for point in flat)
    max_y = max(point[1] for point in flat)
    canvas = Image.new("RGB", (640, 800), (12, 16, 24))
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.truetype(r"C:\Windows\Fonts\msyh.ttc", 18)
    draw.text((16, 12), "离线诊断  不是游戏画面", font=font, fill=(240, 220, 120))
    draw.text((16, 36), f"orbit {orbit} pitch {pitch} dist {distance}", font=font, fill=(200, 200, 200))
    for cloud, color in zip(projected, colors):
        for point in cloud:
            screen_x = 40 + (point[0] - min_x) / (max_x - min_x) * 560
            screen_y = 80 + (point[1] - min_y) / (max_y - min_y) * 680
            draw.rectangle((screen_x, screen_y, screen_x + 2, screen_y + 2), fill=color)
    canvas.save(OUT_PATH)
    print(OUT_PATH)


if __name__ == "__main__":
    main()
