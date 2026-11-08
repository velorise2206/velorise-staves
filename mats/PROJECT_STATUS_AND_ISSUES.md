# BÁO CÁO TỔNG KẾT TÌNH HÌNH & HỆ THỐNG PHẢN ỨNG NGUYÊN TỐ VELORISE STAVES

> **Ngày cập nhật:** 09/11/2026  
> **Phiên bản:** `1.0.0` (Triple-Loader: Forge 1.20.1 | NeoForge 1.21.1 | Fabric 1.21.1)  
> **Mục tiêu:** Mô phỏng hoàn chỉnh hệ thống **Elemental Gauge Theory (EGT)** và các phản ứng nguyên tố chuẩn Genshin Impact trong Minecraft.

---

## 1. TỔNG QUAN TÌNH TRẠNG HIỆN TẠI

Hệ thống phản ứng nguyên tố và hiển thị của mod đã được đại tu toàn diện và đồng bộ tuyệt đối trên cả 3 mod loader:

| Mod Loader | Phiên bản MC | Java Version | Trạng thái Build | Nơi triển khai tự động |
| :--- | :---: | :---: | :---: | :--- |
| **Forge** | 1.20.1 | Java 17 | `BUILD SUCCESSFUL` | `mods/velorise_staves/velorise_staves-1.20.1-forge-1.0.0.jar` |
| **NeoForge** | 1.21.1 | Java 21 | `BUILD SUCCESSFUL` | `mods/velorise_staves/velorise_staves-1.21.1-neoforge-1.0.0.jar` & **TLauncher mods** |
| **Fabric** | 1.21.1 | Java 21 | `BUILD SUCCESSFUL` | `mods/velorise_staves/velorise_staves-1.21.1-fabric-1.0.0.jar` |

---

## 2. CÁC VẤN ĐỀ ĐÃ PHÁT HIỆN VÀ GIẢI PHÁP KHẮC PHỤC TRIỆT ĐỂ

### 2.1. Lỗi hiển thị chữ phản ứng dưới thanh Actionbar
- **Hiện tượng:** Dòng chữ phản ứng như `🛡️ IMMUNE! (Blaze is immune to Pyro)` hiện ở đáy màn hình (Actionbar), che mất thanh trang bị/máu và không giống Genshin Impact.
- **Nguyên nhân:** Biến cấu hình `SHOW_ACTIONBAR_FEEDBACK` bật `true` theo mặc định.
- **Giải pháp:**
  - Chuyển `SHOW_ACTIONBAR_FEEDBACK = false` mặc định trong cấu hình [`StavesConfig.java`](file:///c:/Documents/Coder/Minecraft/myMod/velorise_staves/neoforge-1.21.1/src/main/java/com/velorise/staves/config/StavesConfig.java).
  - Đưa toàn bộ chữ phản ứng (`§lIMMUNE`, `§lVAPORIZE`, `§lMELT`,...) bay trực tiếp dưới dạng chữ nổi 3D in đậm phía trên đầu quái.

### 2.2. Lỗi icon nguyên tố và chữ 3D bị biến mất (Backface Culling)
- **Hiện tượng:** Khi camera nhìn từ một số góc hoặc khi tiếp cận quái, icon nguyên tố và chữ bị biến mất hoặc đảo chiều lộn ngược.
- **Nguyên nhân:** Trong bản vá trước, ma trận scale PoseStack dùng `0.025F` dương thay vì `-0.025F` âm. Trong pipeline render của Minecraft, camera billboard font bắt buộc dùng tọa độ X âm để quay mặt về phía ống kính camera; nếu để dương sẽ làm lật winding order của quad khiến OpenGL culling mặt trước/sau.
- **Giải pháp:** Sửa scale trong [`ClientAuraRenderer.java`](file:///c:/Documents/Coder/Minecraft/myMod/velorise_staves/neoforge-1.21.1/src/main/java/com/velorise/staves/client/ClientAuraRenderer.java) chuẩn:
  ```java
  poseStack.scale(-0.025F, -0.025F, 0.025F);
  ```
  Đồng thời gọi `bs.endBatch()` trước khi `poseStack.popPose()` để đảm bảo vertex buffer giữ đúng ma trận camera (tương thích 100% với Sodium, Embeddium, Iris Shaders).

### 2.3. Sát thương số và Màu sắc nguyên tố bay nổi (Floating Damage Numbers)
- **Tính năng mới:** Bổ sung hiển thị số sát thương nổi (Floating Damage Numbers) bay lên theo từng đòn đánh và phản ứng:
  - **Hỏa (Pyro):** Đỏ tươi (`#FF453A`)
  - **Thủy (Hydro):** Xanh dương (`#0A84FF`)
  - **Băng (Cryo):** Xanh lơ tuyết (`#64D2FF`)
  - **Lôi (Electro):** Tím điện (`#BF5AF2`)
  - **Thảo (Dendro):** Xanh lá thảo mộc (`#32D74B`)
  - **Phong (Anemo):** Lam ngọc (`#55EFC4`)
  - **Nham (Geo):** Vàng kim (`#FFD60A`)

### 2.4. Môi trường Lửa & Nước tự động gắn ấn (Environmental Ingestion)
- **Hiện tượng:** Quái vật hoặc người chơi bước vào lửa/nước không tự động có ấn nếu không bị gậy phép bắn.
- **Giải pháp:**
  - Lắng nghe sự kiện `DamageTypeTags.IS_FIRE` (cháy, đứng trên lửa, dung nham, lửa trại, sàn dung nham magma, bật lửa) và tự động gọi `handleEnvironmentalFireDamage(entity)`.
  - Nếu đối tượng đang có Thủy -> Kích hoạt **Bốc Hơi (Reverse Vaporize)** và dập tắt lửa.
  - Nếu đối tượng đang có Băng -> Kích hoạt **Tan Chảy (Forward Melt)** x2.0.
  - Nếu đối tượng đang có Lôi -> Kích hoạt **Quá Tải (Overloaded)** nổ văng xung quanh!
  - Nếu đối tượng đang có Thảo -> Kích hoạt **Cháy (Burning)** liên tục.
  - Nếu chưa có ấn -> Tự động nhận **1.0U ấn Hỏa**.
  - **Xuống nước:** Quái hoặc người chơi mang ấn Thảo nhảy xuống nước sẽ kích hoạt ngay **Nở Rộ (Bloom)** và rớt **Hạt Nhân Thảo 3D**!

### 2.5. Hạt Nhân Thảo 3D (Dendro Core Entity)
- **Hiện tượng:** Trước đây phản ứng Bloom chỉ lưu timestamp ẩn mà không có vật phẩm thực thể vật lý rơi ra thế giới.
- **Giải pháp:**
  - Đăng ký item `velorise_staves:dendro_core`.
  - Tạo texture 16x16 pixel art và model khối 3D Blockbench chuẩn Minecraft.
  - Quản lý qua [`DendroCoreManager.java`](file:///c:/Documents/Coder/Minecraft/myMod/velorise_staves/neoforge-1.21.1/src/main/java/com/velorise/staves/element/DendroCoreManager.java):
    - Tối đa 5 hạt tồn tại cùng lúc (hạt thứ 6 xuất hiện thì hạt cũ nhất nổ ngay).
    - Hạt rơi tự do, có lực cản nổi bập bềnh trên mặt nước (buoyancy), phát ánh sáng đom đóm xanh lá.
    - Sau 6 giây (120 ticks) tự nổ diện rộng 3m gây 8.0 DMG.
    - Nếu gặp đòn đánh **Lôi (Electro)** gần đó -> Kích hoạt **Hyperbloom (Sprawling Shot)**: phóng đạn tầm nhiệt truy đuổi kẻ địch gần nhất trong bán kính 16m gây 16.0 DMG!
    - Nếu gặp đòn đánh **Hỏa (Pyro)** gần đó -> Kích hoạt **Burgeon**: nổ tung cực mạnh bán kính 5m gây 17.5 DMG!

### 2.6. Tương tác Thảo với Nước & Bắn Thảo xuống nước (Water Bloom Ingestion)
- **Hiện tượng:** Trước đây, Iron Golem hoặc sinh vật đứng dưới nước bị bắn Thảo vẫn giữ nguyên ấn Thảo trên đầu mà không tạo Hạt Thảo; hoặc bắn trượng Thảo xuống sông/hồ không có hiệu ứng gì.
- **Giải pháp:**
  - Tự động kiểm tra `isEntityInWater`: Nếu thực thể mang ấn Thảo tiếp xúc với nước hoặc mưa, ấn Thảo bị tiêu hao ngay lập tức để kích hoạt **Sum Suê (Bloom)** và sinh ra Hạt Nhân Thảo 3D.
  - Khi bắn trượng Thảo (`Wither Staff`, `Web Staff`) chạm vào bất kỳ khối nước hoặc mặt nước nào (kể cả không trúng quái), hệ thống lập tức cho nở một **Hạt Nhân Thảo 3D** nổi bồng bềnh trên mặt nước chuẩn Genshin Impact (Nahida/Tighnari bắn vào nước).

### 2.7. Dập tắt triệt để thông báo Actionbar
- **Hiện tượng:** Khi đánh quái miễn nhiễm (Blaze trúng Hỏa), màn hình hiện dòng chữ `🛡️ IMMUNE! (Blaze is immune to Pyro)` ở đáy màn hình (Actionbar).
- **Giải pháp:** Vô hiệu hóa hoàn toàn phương thức `sendActionbar`, chuyển đổi 100% phản hồi sang chữ nổi 3D in đậm phía trên đầu quái (`§lIMMUNE`, `§lBLOOM`, `§lVAPORIZE`,...).

### 2.8. Lực đẩy định hướng phản ứng Quá Tải (Overloaded Directional Knockback)
- **Hiện tượng:** Mục tiêu trực tiếp kích hoạt Quá Tải trước đây chỉ bị nảy nhẹ lên trên tại chỗ do vector đẩy lùi từ tâm nổ về chính nó bằng 0.
- **Giải pháp:** Bổ sung vector đẩy lùi định hướng từ vị trí người chơi ra xa sau lưng quái, khiến mục tiêu bị hất văng lùi lại với lực nổ uy lực y hệt Genshin Impact.

---

## 3. PHÂN LOẠI QUÁI VẬT NGUYÊN TỐ TRONG MINECRAFT

Hệ thống phân chia toàn bộ sinh vật Minecraft thành 2 nhóm rõ rệt:

### Nhóm 1: Quái vật mang nguyên tố bẩm sinh (Innate Elemental Mobs)
*Đặc điểm: Luôn có ấn nguyên tố vĩnh viễn trên đầu, miễn nhiễm sát thương cùng hệ, và tự hồi lại ấn sau 0.8 giây nếu bị phản ứng tiêu hao sạch.*

| Nguyên tố | Sinh vật áp dụng | Cơ chế đặc biệt |
| :--- | :--- | :--- |
| **🔥 Hỏa (Pyro)** | Blaze, Magma Cube, Strider | Miễn nhiễm Hỏa; không thể bị dính ấn Thủy/Băng đè lên |
| **❄️ Băng (Cryo)** | Stray, Snow Golem | Luôn mang ấn Băng 1.0U |
| **💧 Thủy (Hydro)** | Guardian, Elder Guardian, Drowned | Luôn mang ấn Thủy 1.0U; không bị dập tắt |
| **⚡ Lôi (Electro)** | Charged Creeper (Creeper tích điện) | Mang ấn Lôi bẩm sinh |
| **🌿 Thảo (Dendro)** | Bogged (Bộ xương đầm lầy 1.21+) | Mang ấn Thảo bẩm sinh |
| **🌪️ Phong (Anemo)** | Breeze (1.21+) | Mang ấn Phong bẩm sinh |

### Nhóm 2: Sinh vật bình thường & Sinh vật biển môi trường
- **Cá, Mực, Cá heo, Rùa, Dơi:** Là các sinh vật biển tự nhiên/ambient, bình thường **không hiển thị icon ấn rác trên đầu** để giữ màn hình sạch đẹp; chỉ hiển thị khi trực tiếp tham gia chiến đấu/phản ứng nguyên tố.
- **Tất cả các mobs còn lại (Zombie, Skeleton, Spider, Enderman, Iron Golem, Player,...):** Không có ấn nguyên tố ban đầu, nhận ấn và phản ứng hoàn toàn linh hoạt theo đòn đánh hoặc môi trường.

---

## 4. BẢNG TRA CỨU PHẢN ỨNG NGUYÊN TỐ (GENSHIN COMPLIANT)

| Phản ứng | Nguyên tố 1 + Nguyên tố 2 | Hệ số sát thương / Hiệu ứng | Tiêu hao Gauge (Gauge Consumption) |
| :--- | :--- | :--- | :--- |
| **Bốc Hơi Thuận (Forward Vaporize)** | Hỏa (Aura) + Thủy (Trigger) | **x2.0 DMG** | 0.5x (Thủy tốn ít ấn Hỏa) |
| **Bốc Hơi Nghịch (Reverse Vaporize)** | Thủy (Aura) + Hỏa (Trigger) | **x1.5 DMG** | 2.0x (Hỏa tốn gấp đôi ấn Thủy) |
| **Tan Chảy Thuận (Forward Melt)** | Băng (Aura) + Hỏa (Trigger) | **x2.0 DMG** | 0.5x |
| **Tan Chảy Nghịch (Reverse Melt)** | Hỏa (Aura) + Băng (Trigger) | **x1.5 DMG** | 2.0x |
| **Quá Tải (Overloaded)** | Hỏa + Lôi | Nổ 3.5m AoE + Đẩy văng đối thủ | 1.0x |
| **Điện Cảm (Electro-Charged)** | Thủy + Lôi | Hai ấn cùng tồn tại, giật điện mỗi 1s | 0.4U mỗi nhịp |
| **Đóng Băng (Frozen)** | Thủy + Băng | Đóng băng bất động 3.5 giây | Hóa thành lớp băng bảo vệ |
| **Tan Vỡ (Shatter)** | Đóng Băng + Nham / Đập mạnh | Phá vỡ băng gây thêm 10.0 DMG | Xóa trạng thái đóng băng |
| **Khuếch Tán (Swirl)** | Hỏa / Thủy / Băng / Lôi + Phong | Lan tỏa ấn ra xung quanh 5.0m | Lan truyền nguyên tố gốc |
| **Kết Tinh (Crystallize)** | Hỏa / Thủy / Băng / Lôi + Nham | Tạo giáp bảo vệ (Absorption) | Tiêu hao 0.5x |
| **Sinh Trưởng (Quicken)** | Thảo + Lôi | Trạng thái Quicken trong 7.0 giây | Tạo nền kích hoạt Lan Tràn/Tăng Cường |
| **Tăng Cường (Aggravate)** | Quicken + Lôi | Tăng thêm +5.0 DMG trực tiếp | Không tiêu hao Quicken |
| **Lan Tràn (Spread)** | Quicken + Thảo | Tăng thêm +6.5 DMG trực tiếp | Không tiêu hao Quicken |
| **Nở Rộ (Bloom)** | Thủy + Thảo | Rớt **Hạt Nhân Thảo 3D** (tự nổ 6s) | Tiêu hao Thủy/Thảo |
| **Nở Rộ Siêu Đẳng (Hyperbloom)**| Hạt Nhân Thảo + Lôi | Đạn tầm nhiệt 16m gây 16.0 DMG | Kích nổ hạt thảo |
| **Bung Nở (Burgeon)** | Hạt Nhân Thảo + Hỏa | Nổ diện rộng 5m gây 17.5 DMG | Kích nổ hạt thảo |
| **Thiêu Đốt (Burning)** | Thảo + Hỏa | Cháy liên tục trong 5 giây | Đốt cạn dần ấn Thảo |

---

## 5. HƯỚNG DẪN BUILD & TRIỂN KHAI

Để build lại toàn bộ dự án cho cả 3 loader và tự động copy vào thư mục game:
```powershell
powershell -ExecutionPolicy Bypass -File .\build-all.ps1
```
Tập lệnh sẽ tự động:
1. Sử dụng JDK 17 cho Forge 1.20.1.
2. Sử dụng JDK 21 cho NeoForge 1.21.1 và Fabric 1.21.1.
3. Kích hoạt cờ `--no-daemon` để tránh xung đột tài nguyên giữa các JVM process.
4. Tự động sao chép các file `.jar` hoàn chỉnh vào thư mục `mods` của dự án và thư mục `mods` của TLauncher.
