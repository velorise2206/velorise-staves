import os
from PIL import Image

def recolor_blaze_to_breeze(input_path: str, output_path: str, is_netherite: bool = False):
    """
    Chuyển đổi texture gậy Blaze (Inferno Rod) thành gậy Breeze chuẩn phong cách Minecraft 1.21:
    - Viền: Tím thẫm / Slate violet huyền bí
    - Khuôn mặt & Trán: Gradient tím tro / Slate purple đặc trưng của Breeze
    - Mắt: Xanh ngọc phát sáng (Glowing Aqua/Cyan) với đồng tử phong lôi
    - Các thanh trôi nổi (Floating Rods): Lốc xoáy / Breeze Rod màu ngọc lam gió (Wind charge cyan)
    - Cán trượng: Lõi ngọc lam gió xoắn ốc (hoặc hợp kim Netherite nếu is_netherite=True)
    """
    if not os.path.exists(input_path):
        raise FileNotFoundError(f"Không tìm thấy file nguồn: {input_path}")
        
    im = Image.open(input_path).convert('RGBA')
    w, h = im.size
    breeze_im = Image.new('RGBA', (w, h), (0, 0, 0, 0))

    for y in range(h):
        for x in range(w):
            r, g, b, a = im.getpixel((x, y))
            if a == 0:
                continue

            # Phân vùng pixel:
            is_floating_rod = (y <= 1) or (x <= 4 and y <= 4) or (x >= 14 and y <= 4) or (x >= 10 and y >= 7 and y <= 10)
            is_handle = (y >= 7 and x <= 9)
            is_eye = (y == 4 and (x == 8 or x == 12))
            is_pupil = (y == 4 and (x == 9 or x == 11))

            if is_eye:
                # Mắt Breeze phát sáng (Cyan pastel sáng rực)
                breeze_im.putpixel((x, y), (200, 255, 248, 255))
            elif is_pupil:
                # Đồng tử mắt Breeze (Xanh ngọc lôi phong)
                breeze_im.putpixel((x, y), (45, 215, 195, 255))
            elif is_floating_rod:
                # Các thanh Breeze xoay quanh đầu (Breeze Rod / Wind charges)
                if (r, g, b) == (255, 238, 85): # Highlight vàng chói -> Trắng lục lam gió
                    breeze_im.putpixel((x, y), (185, 250, 242, 255))
                elif (r, g, b) == (255, 136, 0) or (r, g, b) == (204, 85, 0): # Cam -> Ngọc lam tươi
                    breeze_im.putpixel((x, y), (70, 205, 190, 255))
                else: # Đậm -> Ngọc lam đậm
                    breeze_im.putpixel((x, y), (35, 140, 135, 255))
            elif is_handle:
                if is_netherite:
                    # Giữ nguyên màu chuôi kim loại Netherite nếu là bản nâng cấp
                    breeze_im.putpixel((x, y), (r, g, b, a))
                else:
                    # Chuôi Breeze Rod xoắn gió
                    if (r, g, b) in ((51, 13, 0), (26, 13, 0)): # Viền
                        breeze_im.putpixel((x, y), (30, 22, 45, 255))
                    elif (r, g, b) == (128, 43, 0): # Bóng cán
                        breeze_im.putpixel((x, y), (42, 135, 132, 255))
                    elif (r, g, b) == (204, 85, 0): # Sáng cán
                        breeze_im.putpixel((x, y), (95, 220, 205, 255))
                    else:
                        breeze_im.putpixel((x, y), (60, 180, 170, 255))
            else:
                # Phần đầu & khuôn mặt Breeze
                if (r, g, b) in ((51, 13, 0), (26, 13, 0)): # Viền đầu
                    breeze_im.putpixel((x, y), (28, 20, 42, 255))
                elif (r, g, b) == (255, 238, 85): # Trán / Lông mày tím sáng
                    breeze_im.putpixel((x, y), (145, 120, 195, 255))
                elif (r, g, b) == (255, 136, 0): # Mặt trên
                    breeze_im.putpixel((x, y), (110, 85, 160, 255))
                elif (r, g, b) == (204, 85, 0): # Mặt dưới
                    breeze_im.putpixel((x, y), (82, 60, 125, 255))
                elif (r, g, b) == (128, 43, 0): # Cằm
                    breeze_im.putpixel((x, y), (55, 40, 90, 255))
                else:
                    breeze_im.putpixel((x, y), (75, 55, 115, 255))

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    breeze_im.save(output_path)
    print(f"Đã lưu texture mới tại: {output_path}")

if __name__ == '__main__':
    base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    tex_dir = os.path.join(base_dir, 'shared-resources', 'assets', 'velorise_staves', 'textures', 'item')
    
    inferno_path = os.path.join(tex_dir, 'inferno_rod.png')
    breeze_path = os.path.join(tex_dir, 'breeze_rod.png')
    recolor_blaze_to_breeze(inferno_path, breeze_path, is_netherite=False)
    
    neth_inferno_path = os.path.join(tex_dir, 'netherite_inferno_rod.png')
    neth_breeze_path = os.path.join(tex_dir, 'netherite_breeze_rod.png')
    recolor_blaze_to_breeze(neth_inferno_path, neth_breeze_path, is_netherite=True)
