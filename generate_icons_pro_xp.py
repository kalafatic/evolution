import zlib
import struct
import os

def make_png(width, height, data):
    def chunk(type, data):
        return struct.pack('>I', len(data)) + type + data + struct.pack('>I', zlib.crc32(type + data) & 0xffffffff)

    ihdr = struct.pack('>2I5B', width, height, 8, 2, 0, 0, 0)

    idat_data = b''
    for y in range(height):
        row = b'\x00' # No filter
        for x in range(width):
            pixel = data[y * width + x]
            row += struct.pack('BBB', int(pixel[0]), int(pixel[1]), int(pixel[2]))
        idat_data += row

    idat = zlib.compress(idat_data)
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')
    return png

def save_png(filename, width, height, data):
    with open(filename, 'wb') as f:
        f.write(make_png(width, height, data))

def lerp(c1, c2, t):
    return [max(0, min(255, c1[i] + (c2[i] - c1[i]) * t)) for i in range(3)]

def draw_rect(data, w, h, x, y, rw, rh, color):
    for i in range(int(y), int(y+rh)):
        for j in range(int(x), int(x+rw)):
            if 0 <= i < h and 0 <= j < w:
                data[i * w + j] = color

def generate_pro_xp_app_icon(size):
    # 3D Metallic Isometric Block
    bg = [45, 62, 80]
    data = [bg] * (size * size)

    # Simple isometric box
    c_mid = size // 2
    c_top = size // 4
    c_bot = 3 * size // 4

    silver_light = [230, 230, 230]
    silver_mid = [160, 160, 160]
    silver_dark = [80, 80, 80]
    blue_accent = [0, 85, 229]

    # Isometric Top
    for y in range(size // 4, size // 2):
        for x in range(size // 4, 3 * size // 4):
            dx = abs(x - size // 2)
            dy = (y - size // 4)
            if dy >= dx / 2:
                data[y * size + x] = silver_light

    # Isometric Front-Left
    for y in range(size // 2, 3 * size // 4):
        for x in range(size // 4, size // 2):
            if (y - size // 2) < (x - size // 4) * 2:
                data[y * size + x] = silver_mid

    # Isometric Front-Right
    for y in range(size // 2, 3 * size // 4):
        for x in range(size // 2, 3 * size // 4):
            if (y - size // 2) < (3 * size // 4 - x) * 2:
                data[y * size + x] = silver_dark

    # Blue core highlight in center
    draw_rect(data, size, size, size//2 - size//10, size//2 - size//10, size//5, size//5, blue_accent)

    return data

def generate_pro_xp_folder(size=16):
    # Pro Blue Folder (Technical)
    bg = [240, 240, 240]
    data = [bg] * 256
    blue_light = [100, 150, 255]
    blue_mid = [0, 100, 200]
    blue_dark = [0, 50, 150]

    for y in range(3, 14):
        for x in range(1, 15):
            if y < 5 and x > 8: continue
            t = (y - 3) / 11
            data[y * 16 + x] = lerp(blue_light, blue_dark, t)

    # Add a small "wrench" overlay (white pixels)
    for i in range(7, 11):
        data[i * 16 + 5] = [255, 255, 255]
        data[9 * 16 + i] = [255, 255, 255]

    return data

def generate_pro_xp_orchestrator(size=16):
    # 3D Gear Hub
    bg = [240, 240, 240]
    data = [bg] * 256
    grey_light = [200, 200, 200]
    grey_dark = [80, 80, 80]
    blue = [0, 120, 255]

    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            dist = ((x-cx)**2 + (y-cy)**2)**0.5
            if 3 < dist < 7:
                # Teeth
                angle = (y-cy) / (x-cx) if x != cx else 100
                if int(dist * 2) % 2 == 0:
                    data[y * 16 + x] = lerp(grey_light, grey_dark, y/16)
            elif dist <= 3:
                data[y * 16 + x] = blue
    return data

def generate_pro_xp_agent(size=16):
    # Professional Silver Robot Head (Technical)
    bg = [240, 240, 240]
    data = [bg] * 256
    silver = [220, 220, 220]
    dark = [60, 60, 60]

    for y in range(2, 14):
        for x in range(4, 12):
            # Curved head
            dx = abs(x - 7.5)
            dy = abs(y - 7.5)
            if (dx*dx + dy*dy)**0.5 < 6:
                data[y * 16 + x] = lerp(silver, dark, y/14)

    # Glowing blue technical eyes
    data[5 * 16 + 6] = [0, 200, 255]
    data[5 * 16 + 9] = [0, 200, 255]

    # Circuit lines
    for i in range(8, 12):
        data[i * 16 + 7] = [100, 100, 100]

    return data

icon_dir = 'eu.kalafatic.evolution.view/icons'
os.makedirs(icon_dir, exist_ok=True)

for size in [16, 32, 48, 64, 128, 256, 512]:
    save_png(os.path.join(icon_dir, f'eclipse{size}.png'), size, size, generate_pro_xp_app_icon(size))

save_png(os.path.join(icon_dir, 'sample.png'), 16, 16, generate_pro_xp_app_icon(16))
save_png(os.path.join(icon_dir, 'sample@2x.png'), 32, 32, generate_pro_xp_app_icon(32))

save_png(os.path.join(icon_dir, 'evo_project.png'), 16, 16, generate_pro_xp_folder())
save_png(os.path.join(icon_dir, 'orchestrator.png'), 16, 16, generate_pro_xp_orchestrator())
save_png(os.path.join(icon_dir, 'agent.png'), 16, 16, generate_pro_xp_agent())
