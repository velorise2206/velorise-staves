#!/usr/bin/env python3
"""
Genshin Impact to Minecraft Pixel Art Texture Converter
-------------------------------------------------------
Tự động chuyển đổi các asset 2D/HD độ phân giải cao của Genshin Impact
(Thánh di vật, Nguyên liệu đột phá, Biểu tượng nguyên tố, Vũ khí...)
thành texture Pixel Art 16x16 hoặc 32x32 chuẩn Minecraft.

Sử dụng:
    python scripts/convert_genshin_assets.py --input input_folder --output output_folder --size 16
    python scripts/convert_genshin_assets.py --file path/to/image.png --size 16 --outline
"""

import os
import sys
import argparse
from PIL import Image, ImageEnhance

def convert_image_to_pixel_art(
    img: Image.Image,
    target_size: int = 16,
    alpha_threshold: int = 80,
    contrast_boost: float = 1.25,
    color_boost: float = 1.35,
    add_outline: bool = False,
    outline_color: tuple = (25, 20, 25, 255)
) -> Image.Image:
    """
    Chuyển đổi 1 ảnh RGBA thành pixel art chuẩn Minecraft:
    1. Cắt bớt khoảng trống trong suốt thừa (Crop bounding box)
    2. Căn giữa vào khung vuông tỉ lệ 1:1
    3. Co nhỏ (Resample) bằng Lanczos với lề an toàn 1px
    4. Tăng cường độ tương phản (Contrast) và bão hòa màu (Saturation)
    5. Cắt ngưỡng Alpha (Binarize) loại bỏ viền mờ đục
    6. (Tùy chọn) Thêm viền tối 1px nổi bật
    """
    img = img.convert("RGBA")
    
    # 1. Crop bounding box
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
        w, h = img.size
        dim = max(w, h)
        padded = Image.new("RGBA", (dim, dim), (0, 0, 0, 0))
        padded.paste(img, ((dim - w) // 2, (dim - h) // 2))
        img = padded
        
    # 2. Resample
    inner_margin = 2 if not add_outline else 4
    inner_size = max(4, target_size - inner_margin)
    small = img.resize((inner_size, inner_size), Image.Resampling.LANCZOS)
    
    # 3. Enhance color & contrast
    rgb = small.convert("RGB")
    rgb = ImageEnhance.Contrast(rgb).enhance(contrast_boost)
    rgb = ImageEnhance.Color(rgb).enhance(color_boost)
    
    # 4. Binarize alpha
    r, g, b = rgb.split()
    a = small.split()[3]
    a = a.point(lambda p: 255 if p > alpha_threshold else 0)
    small_clean = Image.merge("RGBA", (r, g, b, a))
    
    # 5. Canvas paste
    canvas = Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0))
    offset = (target_size - inner_size) // 2
    canvas.paste(small_clean, (offset, offset))
    
    # 6. Add outline if requested
    if add_outline:
        w, h = canvas.size
        outlined = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        cur_alpha = canvas.split()[3]
        for y in range(h):
            for x in range(w):
                if cur_alpha.getpixel((x, y)) == 0:
                    has_solid_neighbor = False
                    for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < w and 0 <= ny < h:
                            if cur_alpha.getpixel((nx, ny)) > 0:
                                has_solid_neighbor = True
                                break
                    if has_solid_neighbor:
                        outlined.putpixel((x, y), outline_color)
        outlined.paste(canvas, (0, 0), canvas)
        return outlined

    return canvas

def main():
    parser = argparse.ArgumentParser(description="Convert Genshin assets to Minecraft pixel art textures")
    parser.add_argument("--input", "-i", type=str, help="Input directory containing PNG images")
    parser.add_argument("--file", "-f", type=str, help="Single PNG file to convert")
    parser.add_argument("--output", "-o", type=str, default="output_textures", help="Output directory")
    parser.add_argument("--size", "-s", type=int, default=16, help="Target texture size (16 or 32)")
    parser.add_argument("--threshold", "-t", type=int, default=80, help="Alpha threshold (0-255)")
    parser.add_argument("--outline", action="store_true", help="Add 1px dark outline")
    
    args = parser.parse_args()
    os.makedirs(args.output, exist_ok=True)
    
    files = []
    if args.file:
        files.append(args.file)
    elif args.input:
        for fname in os.listdir(args.input):
            if fname.lower().endswith((".png", ".webp", ".jpg", ".jpeg")):
                files.append(os.path.join(args.input, fname))
    else:
        print("Please provide --input directory or --file path.")
        return
        
    print(f"Converting {len(files)} file(s) to {args.size}x{args.size} pixel art...")
    for fpath in files:
        base_name = os.path.splitext(os.path.basename(fpath))[0]
        try:
            with Image.open(fpath) as img:
                result = convert_image_to_pixel_art(
                    img,
                    target_size=args.size,
                    alpha_threshold=args.threshold,
                    add_outline=args.outline
                )
                out_path = os.path.join(args.output, f"{base_name}.png")
                result.save(out_path)
                print(f"  [OK] {base_name} -> {out_path}")
        except Exception as e:
            print(f"  [FAIL] {fpath}: {e}")

if __name__ == "__main__":
    main()
