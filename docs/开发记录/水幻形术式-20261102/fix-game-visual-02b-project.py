# Offline diagnostic only. Not a playtest.
# Replays SteveWaterLineMesh texel expansion and alignVanillaSteve, then a front camera.
import math
import struct
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(r"D:\alianximod")
BIN_PATH = ROOT / "src/main/resources/assets/xueximod/models/effect/water_phantom_steve_v21/lines.bin"
SKIN_PATH = ROOT / "src/main/resources/assets/xueximod/textures/entity/xueximod/steve_water_lines.png"
OUT_PATH = Path(__file__).with_name("fix-game-visual-02b-offline-front.png")


def load_pieces(blob):
    offset = 6
    count = struct.unpack_from(">H", blob, 4)[0]
    pieces = []
    for _piece_index in range(count):
        texture = blob[offset]
        offset += 1
        pivot = struct.unpack_from(">fff", blob, offset)
        offset += 12
        quad_count = struct.unpack_from(">H", blob, offset)[0]
        offset += 2
        faces = []
        for _quad_index in range(quad_count):
            offset += 12
            corners = []
            for _vertex_index in range(4):
                corners.append(struct.unpack_from(">fffff", blob, offset))
                offset += 20
            faces.append(corners)
        def read_keys():
            nonlocal offset
            key_count = struct.unpack_from(">H", blob, offset)[0]
            offset += 2
            offset += key_count * 17
        read_keys()
        read_keys()
        read_keys()
        pieces.append((texture, pivot, faces))
    return pieces


def expand_skin(pivot, faces, image):
    width, height = image.size
    pixels = image.load()
    points = []
    for face in faces:
        origin_u, origin_v = face[0][3], face[0][4]
        across_u = face[1][3] - origin_u
        across_v = face[1][4] - origin_v
        down_u = face[3][3] - origin_u
        down_v = face[3][4] - origin_v
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


def align_vanilla(point, body_yaw):
    local_x, local_y, local_z = point
    local_y -= 1.501
    local_x = -local_x
    local_y = -local_y
    theta = math.radians(180.0 - body_yaw)
    cosine = math.cos(theta)
    sine = math.sin(theta)
    return (
        local_x * cosine + local_z * sine,
        local_y,
        -local_x * sine + local_z * cosine,
    )


def project_front(point):
    aligned = align_vanilla(point, 180.0)
    camera = (0.0, 1.45, -4.2)
    relative = (aligned[0] - camera[0], aligned[1] - camera[1], aligned[2] - camera[2])
    depth = relative[2]
    return (-relative[0] / depth, -relative[1] / depth)


def main():
    pieces = load_pieces(BIN_PATH.read_bytes())
    skin = Image.open(SKIN_PATH).convert("RGBA")
    clouds = [expand_skin(pivot, faces, skin) if texture == 0 else [] for texture, pivot, faces in pieces[:6]]
    colors = [(255, 80, 80, 255), (80, 160, 255, 255), (180, 220, 255, 255), (255, 255, 255, 255), (140, 140, 140, 255), (90, 90, 90, 255)]
    projected = [[project_front(point) for point in cloud] for cloud in clouds]
    flat = [sample for cloud in projected for sample in cloud]
    min_x = min(sample[0] for sample in flat)
    max_x = max(sample[0] for sample in flat)
    min_y = min(sample[1] for sample in flat)
    max_y = max(sample[1] for sample in flat)
    canvas = Image.new("RGB", (640, 800), (12, 16, 24))
    draw = ImageDraw.Draw(canvas)
    from PIL import ImageFont
    label_font = ImageFont.truetype(r"C:\Windows\Fonts\msyh.ttc", 18)
    draw.text((16, 16), "离线诊断  不是游戏画面", font=label_font, fill=(240, 220, 120))
    draw.text((16, 40), "正面：已提交纹素。红=画面左侧 arm_nx，蓝=arm_px", font=label_font, fill=(200, 200, 200))

    def place(sample):
        screen_x = 40 + (sample[0] - min_x) / (max_x - min_x) * 560
        screen_y = 110 + (sample[1] - min_y) / (max_y - min_y) * 640
        return screen_x, screen_y

    for cloud, color in zip(projected, colors):
        for sample in cloud:
            screen_x, screen_y = place(sample)
            draw.rectangle((screen_x, screen_y, screen_x + 2, screen_y + 2), fill=color[:3])
    canvas.save(OUT_PATH)
    print(OUT_PATH)


if __name__ == "__main__":
    main()
