#!/usr/bin/env python3
"""Generates the Auralis launcher icons (pure-python PNG writer, no deps)."""
import math, os, struct, zlib

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app/src/main/res")

# palette
TOP = (36, 18, 74)      # deep violet
BOT = (10, 8, 20)       # ink
ACC1 = (168, 124, 255)  # lilac
ACC2 = (255, 122, 190)  # pink
WHITE = (255, 255, 255)

def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def rounded_rect_sdf(x, y, w, h, r):
    # signed distance, negative inside
    dx = abs(x - w / 2) - (w / 2 - r)
    dy = abs(y - h / 2) - (h / 2 - r)
    ax, ay = max(dx, 0.0), max(dy, 0.0)
    return math.hypot(ax, ay) + min(max(dx, dy), 0.0) - r

def bars(cx, cy, unit):
    # (offset x, height, alpha) waveform
    return [
        (-2.0, 1.05, 0.92),
        (-1.0, 1.95, 1.0),
        (0.0, 2.85, 1.0),
        (1.0, 1.55, 1.0),
        (2.0, 0.85, 0.85),
    ]

def render(size, ss=3):
    n = size * ss
    px = bytearray(n * n * 4)
    r = n * 0.225
    cx, cy = n / 2, n / 2
    bw, gap = n * 0.045, n * 0.075
    for y in range(n):
        for x in range(n):
            d = rounded_rect_sdf(x + 0.5, y + 0.5, n, n, r)
            if d > 0.5:
                continue
            t = y / n
            base = mix(TOP, BOT, min(1.0, t * 1.25))
            # diagonal shimmer
            diag = (x / n) * 0.6 + (1 - y / n) * 0.4
            base = mix(base, ACC1, max(0.0, (diag - 0.52)) * 0.55)
            col = base
            # aura ring
            dist = math.hypot(x - cx, (y - cy) * 1.0)
            ring_r = n * 0.335
            ring_w = n * 0.012
            ring_d = abs(dist - ring_r)
            if ring_d < ring_w:
                a = (1 - ring_d / ring_w) ** 2
                col = mix(col, ACC2, a * 0.75)
            # waveform
            for i, (off, hgt, alpha) in enumerate(bars(0, 0, 0)):
                bx = cx + off * gap
                half_w = bw / 2
                bh = (n * 0.105) * hgt
                if abs(x - bx) <= half_w and abs(y - cy) <= bh / 2:
                    fade = 1 - abs(x - bx) / half_w
                    a = min(1.0, 0.75 + fade * 0.35) * alpha
                    col = mix(col, WHITE, a)
            # subtle inner top highlight
            if d > -n * 0.008:
                col = mix(col, WHITE, 0.10)
            i = (y * n + x) * 4
            px[i:i + 4] = bytes((col[0], col[1], col[2], 255))
    # downsample
    out = bytearray(size * size * 4)
    for y in range(size):
        for x in range(size):
            rs = gs = bs = as_ = 0
            for dy in range(ss):
                for dx in range(ss):
                    i = ((y * ss + dy) * n + (x * ss + dx)) * 4
                    rs += px[i]; gs += px[i + 1]; bs += px[i + 2]; as_ += px[i + 3]
            c = ss * ss
            o = (y * size + x) * 4
            out[o:o + 4] = bytes((rs // c, gs // c, bs // c, as_ // c))
    return out

def write_png(path, size, rgba):
    raw = b"".join(b"\x00" + bytes(rgba[y * size * 4:(y + 1) * size * 4]) for y in range(size))
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff)
    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "wb").write(png)

for folder, size in [("mipmap-mdpi", 48), ("mipmap-hdpi", 72), ("mipmap-xhdpi", 96),
                     ("mipmap-xxhdpi", 144), ("mipmap-xxxhdpi", 192)]:
    data = render(size)
    for name in ("ic_launcher.png", "ic_launcher_round.png"):
        write_png(os.path.join(OUT, folder, name), size, data)
    print("wrote", folder, size)
