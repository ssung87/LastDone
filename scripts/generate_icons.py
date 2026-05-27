"""Generate Android mipmap icons from a 512x512 source PNG.

Outputs:
  - Legacy launcher PNGs: mipmap-{mdpi..xxxhdpi}/ic_launcher.png and ic_launcher_round.png
  - Adaptive foreground bitmaps: mipmap-{...}/ic_launcher_foreground.png
  - Prints sampled background color (hex) for use in ic_launcher_background.xml
"""

from __future__ import annotations
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "eonjehaesseo_app_icon_512.png"
RES = ROOT / "app" / "src" / "main" / "res"

LEGACY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Adaptive foreground canvas is 108dp; content safe zone is inner 66dp.
ADAPTIVE_SIZES = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}


def sample_bg_color(img: Image.Image) -> str:
    """Sample background color by walking inward from the top-middle until an
    opaque pixel is found. The top-middle is reliably background (above the
    calendar drawing) once past the rounded-corner curve.
    """
    rgba = img.convert("RGBA")
    w, h = rgba.size
    x = w // 2
    # Skip the rounded-square dark border by starting at 5% and skipping any
    # near-black pixels we hit on the way in.
    for pct in (0.05, 0.07, 0.10, 0.13, 0.15):
        y = int(h * pct)
        px = rgba.getpixel((x, y))
        if px[3] < 250:
            continue
        r, g, b = px[:3]
        if r + g + b < 60:  # skip dark border
            continue
        return f"#{r:02X}{g:02X}{b:02X}"
    raise RuntimeError("Could not sample an opaque background pixel")


def write_round(img: Image.Image, dst: Path) -> None:
    """Write a circle-masked version of img to dst (RGBA)."""
    from PIL import ImageDraw

    size = img.size[0]
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size, size), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    out.save(dst, format="PNG", optimize=True)


def main() -> None:
    src = Image.open(SRC).convert("RGBA")
    print(f"Loaded source: {SRC} ({src.size[0]}x{src.size[1]})")

    bg_hex = sample_bg_color(src)
    print(f"Sampled background color: {bg_hex}")

    # Legacy mipmap icons
    for density, size in LEGACY_SIZES.items():
        out_dir = RES / f"mipmap-{density}"
        out_dir.mkdir(parents=True, exist_ok=True)
        resized = src.resize((size, size), Image.LANCZOS)
        resized.save(out_dir / "ic_launcher.png", format="PNG", optimize=True)
        write_round(resized, out_dir / "ic_launcher_round.png")
        print(f"  wrote {out_dir.name}/ic_launcher{{,_round}}.png  {size}x{size}")

    # Adaptive foreground bitmaps. Use the source as-is on a 108dp canvas.
    # Source already has its own padding, so just resize to the canvas size.
    for density, size in ADAPTIVE_SIZES.items():
        out_dir = RES / f"mipmap-{density}"
        out_dir.mkdir(parents=True, exist_ok=True)
        fg = src.resize((size, size), Image.LANCZOS)
        fg.save(out_dir / "ic_launcher_foreground.png", format="PNG", optimize=True)
        print(f"  wrote {out_dir.name}/ic_launcher_foreground.png  {size}x{size}")

    print(f"\nNext: set ic_launcher_background.xml fillColor to {bg_hex}")


if __name__ == "__main__":
    main()
