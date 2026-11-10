# Hướng Dẫn Chèn Ảnh Minh Họa Vào Sách Elemental Codex (Cẩm Nang Nguyên Tố)

Giao diện `ElementalCodexScreen` được thiết kế với **Khung Minh Họa Thông Minh (Auto-Detect Screenshot Banner)** ở đầu mỗi chương.
- **Mặc định (khi chưa có file ảnh PNG)**: Khung minh họa tự động vẽ **Sơ đồ trực quan bằng Icon Nguyên Tố & Vật Phẩm** ngay trong game.
- **Khi bạn chụp ảnh trong game (F2) và muốn đưa vào sách**: Bạn chỉ cần cắt ảnh (tỉ lệ khuyên dùng `256x128` hoặc `512x256` pixel, định dạng `.png`) và đặt vào đúng thư mục này (`shared-resources/assets/velorise_staves/textures/gui/codex/`) với các tên file tương ứng dưới đây. Hệ thống sẽ **tự động nhận diện và hiển thị ảnh chụp của bạn** mà không cần sửa code!

## Danh sách tên file ảnh cho 7 Chương:
1. `tab_basics.png` — Minh họa Chương 1: Nhập Môn (VD: Ảnh quái vật đang có biểu tượng Ấn Nguyên Tố trên đầu).
2. `tab_elements.png` — Minh họa Chương 2: 7 Nguyên Tố (VD: Ảnh các quái vật mang ấn bẩm sinh hoặc 7 hiệu ứng nguyên tố).
3. `tab_reactions_amp.png` — Minh họa Chương 3: Phản ứng Khuếch Đại & Biến Đổi (VD: Ảnh hiệu ứng chữ nổi Vaporize / Overloaded / Frozen).
4. `tab_reactions_dendro.png` — Minh họa Chương 4: Hệ Thảo & Kết Tinh (VD: Ảnh Hạt Thảo Dendro Core và Tinh Thể Kết Tinh 3D trên mặt đất).
5. `tab_environment.png` — Minh họa Chương 5: Tương tác Môi trường (VD: Ảnh phép Hỏa làm tan băng hoặc Nước Tích Điện phóng sét).
6. `tab_staves.png` — Minh họa Chương 6: Kho Vũ Khí & Pháp Khí (VD: Ảnh nhân vật cầm các cây gậy Netherite).
7. `tab_mastery.png` — Minh họa Chương 7: Sách phép Elemental Mastery & Bàn Phù Phép.
