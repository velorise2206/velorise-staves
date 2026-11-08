# Báo Cáo Phân Tích Kỹ Thuật & Bản Vá Lỗi — Velorise's Staves

Tài liệu này tổng hợp toàn bộ kiến trúc, bảng thông số chi tiết của 26 vật phẩm (11 gậy chiến đấu + 2 bảo vật tiện ích ở 2 bậc Thường & Netherite), cùng danh sách 12 lỗi tiềm ẩn đã được phát hiện và khắc phục triệt để trên cả 3 nền tảng (`forge-1.20.1`, `neoforge-1.21.1`, `fabric-1.21.1`).

---

## 1. Tổng Quan Kiến Trúc Dự Án

- **Mod ID**: `velorise_staves`
- **Phiên bản**: `1.0.1` (Bản vá lỗi & đồng bộ đa nền tảng)
- **Các Loader hỗ trợ**:
  - **Forge 1.20.1**: Java 17, Forge `47.3.0`, công thức chế tạo định dạng 1.20.1 (`data/velorise_staves/recipes/`).
  - **NeoForge 1.21.1**: Java 21, NeoForge `21.1.65`, dùng tài nguyên chung tại `shared-resources`.
  - **Fabric 1.21.1**: Java 21, Fabric Loader `0.16.14` + Fabric API `0.116.14+1.21.1`, dùng Mixin (`ProjectileMixin`) và `shared-resources`.
- **Tài nguyên dùng chung (`shared-resources`)**:
  - 26 textures item (`textures/item/*.png`) và 27 models item (`models/item/*.json`).
  - 1 hiệu ứng âm thanh riêng (`sounds/magic_mirror_use.ogg`).
  - 26 công thức chế tạo & nâng cấp Bàn Rèn chuẩn 1.21.1 (`data/velorise_staves/recipe/*.json`).
  - 2 file Item Tags chuẩn 1.21.1 (`data/minecraft/tags/item/enchantable/durability.json` và `vanishing.json`).
  - Bản dịch đầy đủ Tiếng Anh (`en_us.json`) và Tiếng Việt (`vi_vn.json`).

---

## 2. Bảng Thông Số Hiện Tại (Sau Khi Chuẩn Hóa)

### 2.1. Nhóm 11 Gậy Chiến Đấu (`ArcaneStaffItem`)

| Vật phẩm | Cơ chế hoạt động | Độ bền (Thường / Netherite) | Hồi chiêu | Tầm bắn / Bán kính (Thường → Netherite) | Sát thương & Hiệu ứng (Thường → Netherite) | Nguyên liệu chế tạo / Sửa chữa trên Đe |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Dragonfire Scepter** | Bắn 1 cầu lửa rồng tạo vùng mây `DRAGON_BREATH` (tồn tại 6s / 120 ticks) | 130 / 500 | 8.0s (160t) | Tầm 32m, bán kính mây: 3.5m → 4.5m (nở thêm +1m) | Sát thương tức thời (`HARM`): Cấp I → Cấp II | Chế: 1 Dragon Head + 2 Blaze Rod<br>Sửa: Blaze Rod / Netherite Ingot |
| **Resonant Echo** | Giữ chuột phải 1.5s (30 ticks) tụ sóng âm xuyên thấu mọi mục tiêu trên đường thẳng | 130 / 500 | 8.0s (160t) | Tầm: 28m → 32m, độ rộng tia: 2.0m → 2.5m | Sát thương phép (`magic`): 22.0 → 28.0 | Chế: 1 Echo Shard + 2 Blaze Rod<br>Sửa: Blaze Rod / Netherite Ingot |
| **Inferno Rod** | Chuỗi 3 phát cầu lửa nhỏ (cách nhau 0.2s / 4 ticks, tốn 1 độ bền/chuỗi) | 150 / 550 | 4.0s (80t) | Tầm: 30m (đơn mục tiêu) | Sát thương phép: 6.0 → 8.0/phát; Cháy: 4s → 6s | Chế: 1 Fire Charge + 2 Blaze Rod<br>Sửa: Blaze Rod / Netherite Ingot |
| **Ghastfire Staff** | Bắn 1 cầu lửa Ghast nổ diện rộng kèm vụ nổ Vanilla của `LargeFireball` | 120 / 450 | 5.0s (100t) | Tầm: 36m, bán kính nổ phụ: 3.5m → 4.5m | Sát thương phép: 12.0 → 16.0; Cháy: 5s → 6s (+ nổ Vanilla) | Chế: 1 Ghast Tear + 2 Blaze Rod<br>Sửa: Blaze Rod / Netherite Ingot |
| **Wither Scepter** | Chuỗi 3 phát đầu lâu Wither (cách nhau 0.2s / 4 ticks, tốn 1 độ bền/chuỗi) | 140 / 500 | 6.0s (120t) | Tầm: 32m, bán kính nổ: 3.0m → 4.0m | Sát thương phép: 8.0 → 10.0/phát; Wither 6s: Cấp II → Cấp III | Chế: 1 Wither Skeleton Skull + 2 Blaze Rod<br>Sửa: Blaze Rod / Netherite Ingot |
| **Shulker Seeker** | Bắn 2 viên đạn Shulker cùng lúc (góc lệch 9°, tự khóa mục tiêu trong góc 1.5m) | 130 / 500 | 6.0s (120t) | Tầm: 30m (2 viên/lần bắn) | Sát thương phép: 8.0 → 10.0/viên; Levitation I: 5s → 6s | Chế: 1 Shulker Shell + 2 End Rod<br>Sửa: End Rod, Shulker Shell / Netherite Ingot |
| **Guardian Lens** | Giữ chuột phải tối đa 2.5s (50 ticks), bắn 10 nhịp tia nhiệt (mỗi 5 ticks) | 130 / 500 | 5.0s (100t) | Tầm: 24m → 28m, bán kính khóa tia: 1.2m | Sát thương phép: 2.5 → 3.5 mỗi 5 ticks (tổng 10 nhịp = **25.0 → 35.0**) | Chế: 1 Heart of the Sea + 2 Blaze Rod<br>Sửa: Blaze Rod / Netherite Ingot |
| **Evoker Fang Staff** | Triệu hồi hàng nanh Evoker dọc mặt đất cách nhau 1.5m (độ trễ `i * 2` ticks) | 120 / 450 | 5.0s (100t) | Số lượng: 9 nanh (13.5m) → 13 nanh (19.5m) | Sát thương Vanilla `EvokerFangs` (6.0 sát thương phép/nanh) | Chế: 1 Totem of Undying + 2 Stick<br>Sửa: Stick, Blaze Rod / Netherite Ingot |
| **Stormcaller** | Giáng sét trực tiếp vào mục tiêu hoặc điểm nhìn (`VisualOnly` + sát thương tùy chỉnh) | 130 / 500 | 6.0s (120t) | Tầm: 32m → 36m, bán kính nổ sét: 3.5m → 4.5m | Sát thương sét (`lightningBolt`): 10.0 → 14.0; Cháy: 4s → 5s | Chế: 1 Lightning Rod + 2 Stick<br>Sửa: Stick, Blaze Rod / Netherite Ingot |
| **Frostbolt Wand** | Chuỗi 3 phát cầu tuyết băng (cách nhau 0.2s / 4 ticks, tốn 1 độ bền/chuỗi) | 150 / 500 | 4.0s (80t) | Tầm: 28m; Netherite có thêm nổ lan 2.0m | Trực tiếp: 6.0 → 8.0 (`freeze`), Slowness II (4s → 5s), Freeze (140t → 180t). Nổ lan Netherite: 4.0 sát thương, Slowness I (3s), Freeze (100t) | Chế: 1 Blue Ice + 2 Stick<br>Sửa: Stick, Blaze Rod / Netherite Ingot |
| **Webcaster** | Chuỗi 3 phát cầu tơ nhện (cách nhau 0.2s / 4 ticks), đặt `COBWEB` tạm thời và phát xung độc mỗi 1s | 140 / 500 | 5.0s (100t) | Tầm: 28m, đặt 1 block tơ nhện/phát | Tơ tồn tại: 5s (100t) → 7s (140t); Độc (3s mỗi nhịp, miễn nhiễm cho chủ nhân): Poison I → Poison II | Chế: 1 Cobweb + 2 Stick<br>Sửa: Stick, Blaze Rod / Netherite Ingot |

### 2.2. Nhóm 2 Bảo Vật Tiện Ích & Cơ Động

| Vật phẩm | Chức năng & Cơ chế | Độ bền (Thường / Netherite) | Thời gian niệm / Hồi chiêu | Thông số Thường → Netherite | Nguyên liệu chế tạo / Sửa chữa trên Đe |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Void Translocator** | Dịch chuyển tức thời tới vị trí nhìn (ưu tiên trên đỉnh block, cạnh block, hoặc lùi 1.2m trước vật cản; xóa sát thương rơi) | 80 / 250 | Tức thời / 5.0s (100t) | Khoảng cách: 24 blocks → 36 blocks | Chế: 1 Ender Pearl + 2 Stick<br>Sửa: Ender Pearl / Netherite Ingot |
| **Magic Mirror** | Giữ chuột phải niệm chú dịch chuyển về giường/neo hồi sinh hoặc spawn thế giới | 40 / 150 | Niệm 2.5s (50t) / Hồi 15.0s (300t) | Bản Thường chỉ dùng cùng chiều không gian; bản Netherite xuyên chiều không gian (Nether/End về Overworld) | Chế: 1 Diamond Block + 4 Diamond + 2 Glass<br>Sửa: Diamond / Netherite Ingot |

---

## 3. Danh Sách 12 Lỗi Đã Được Khắc Phục Trong Bản Vá

1. **Bổ sung Item Tags Phù Phép cho Minecraft 1.21.1 (`durability.json` & `vanishing.json`)**:
   - **Vấn đề cũ**: Minecraft 1.21+ kiểm tra quyền ép sách `Unbreaking`, `Mending` và `Curse of Vanishing` thông qua tag `minecraft:enchantable/durability` và `minecraft:enchantable/vanishing`. Do thiếu 2 file tag này, người chơi trên NeoForge 1.21.1 và Fabric 1.21.1 không thể ép sách `Unbreaking` hoặc `Mending` vào gậy trong chế độ Sinh tồn.
   - **Cách khắc phục**: Tạo đầy đủ `shared-resources/data/minecraft/tags/item/enchantable/durability.json` và `vanishing.json` chứa cả 26 vật phẩm, đồng thời thêm loại trừ `exclude 'data/minecraft/tags/item/**'` vào `forge-1.20.1/build.gradle` để không ảnh hưởng tới cấu trúc tag của 1.20.1.

2. **Bổ sung chỉ số Enchantability và sửa chữa bằng nguyên liệu trên Đe (`getEnchantmentValue` & `isValidRepairItem`)**:
   - **Vấn đề cũ**: Cả 3 class vật phẩm chưa ghi đè `getEnchantmentValue()` (mặc định bằng 0) và `isValidRepairItem(...)`, khiến vật phẩm không thể phù phép trên Bàn Phù Phép và không thể sửa bằng nguyên liệu trên Đe như mô tả trong `README.md`.
   - **Cách khắc phục**: Ghi đè `getEnchantmentValue()` (trả về `15` cho bản Thường, `20` cho bản Netherite) và `isValidRepairItem()` cho phép sửa bằng nguyên liệu gốc (`Blaze Rod`, `End Rod`, `Stick`, `Ender Pearl`, `Diamond`) hoặc `Netherite Ingot` đối với bản Netherite.

3. **Miễn nhiễm sát thương độc của `Webcaster` cho chủ sở hữu (`TemporaryWebManager.java`)**:
   - **Vấn đề cũ**: `TemporaryWebManager` lưu `ownerId` của người bắn nhưng không kiểm tra khi phát xung độc mỗi 20 ticks, khiến người chơi tự trúng độc của chính mình khi đứng gần mạng nhện.
   - **Cách khắc phục**: Thêm điều kiện `if (entry.ownerId != null && entry.ownerId.equals(target.getUUID())) continue;` trước khi áp hiệu ứng `MobEffects.POISON`.

4. **Tự động dọn dẹp mạng nhện tạm thời khi tắt máy chủ (`TemporaryWebManager.clearAll`)**:
   - **Vấn đề cũ**: Nếu server tắt hoặc khởi động lại khi mạng nhện của `Webcaster` chưa hết thời gian tồn tại (5–7 giây), các block `COBWEB` sẽ bị kẹt vĩnh viễn trên thế giới.
   - **Cách khắc phục**: Bổ sung phương thức `TemporaryWebManager.clearAll(MinecraftServer)` và đăng ký vào sự kiện `ServerStoppingEvent` (Forge/NeoForge) cũng như `ServerLifecycleEvents.SERVER_STOPPING` (Fabric) để hoàn trả toàn bộ block gốc trước khi lưu thế giới.

5. **Sửa lỗi kẹt nhịp click và hao tốn gấp 3 độ bền của nhóm gậy bắn chuỗi 3 phát (`ArcaneStaffItem.java`)**:
   - **Vấn đề cũ**: Khi bắn 1–2 phát bằng `Inferno Rod`, `Wither Scepter`, `Frostbolt Wand`, hoặc `Webcaster` rồi dừng quá 1 giây (`20 ticks`), lần click tiếp theo bị trả về `false` (không bắn ra đạn) nhưng lại lập tức kích hoạt hồi chiêu 4–6 giây. Ngoài ra, mỗi phát trong chuỗi 3 viên đều trừ 1 độ bền (tốn 3 độ bền/chuỗi).
   - **Cách khắc phục**:
     - Thêm `inventoryTick()` tự động kiểm tra và đóng chuỗi bắn ngay khi quá 20 ticks không thao tác để kích hoạt hồi chiêu tức thì; nếu người chơi click lại sau khi hết thời gian chờ, gậy bắn ngay lập tức mà không bị kẹt.
     - Chỉ trừ 1 điểm độ bền ở viên đạn đầu tiên của mỗi chuỗi 3 phát (`isFirstBurstShot`).

6. **Sửa lỗi mất khả năng tự dẫn đường (Homing) của `Shulker Seeker` (`ArcaneStaffItem.shootShulker`)**:
   - **Vấn đề cũ**: Khi tìm thấy mục tiêu (`target != null`), code khởi tạo `new ShulkerBullet(level, player, target, Direction.Axis.Y)` nhưng sau đó lại gọi `prepareProjectile()` ghi đè vận tốc bay thẳng tốc độ cao (`1.25F`) và đặt 2 viên đạn trùng khít tọa độ, làm hỏng AI dẫn đường của `ShulkerBullet`.
   - **Cách khắc phục**: Khi `target != null`, tách vị trí xuất phát của 2 viên đạn theo góc rẻ quạt `9.0°`, gán vận tốc khởi đầu nhẹ (`0.45D`) và gắn tag/tầm bắn trực tiếp mà không gọi `projectile.shoot()`, giúp 2 viên đạn tách quỹ đạo mượt mà và tự động lao vào mục tiêu.

7. **Xử lý tự hủy đạn khi bay quá tầm tối đa (`maxRange`) trên không trung (Cả 3 Loader)**:
   - **Vấn đề cũ**: Kiểm tra `isBeyondRange()` chỉ chạy khi đạn va chạm (`onProjectileImpact` / `onProjectileHit`). Nếu bắn lên trời hoặc khoảng không, đạn cầu lửa và đầu lâu Wither bay mãi mãi không tự hủy.
   - **Cách khắc phục**:
     - Trên **Fabric**: Inject thêm `velorise$onTick` vào `ProjectileMixin` gọi `ArcaneStaffEvents.onProjectileTick(projectile)` để tự hủy đạn kèm hiệu ứng khói ngay khi vượt quá `maxRange`.
     - Trên **Forge & NeoForge**: Theo dõi các đạn đang bay trong `TRACKED_PROJECTILES` (`WeakHashMap`) và kiểm tra khoảng cách mỗi `onServerTick` để tự hủy đạn khi vượt quá `maxRange`.

8. **Chuẩn hóa đủ 10 nhịp sát thương cho `Guardian Lens` (`ArcaneStaffItem.onUseTick`)**:
   - **Vấn đề cũ**: Điều kiện `elapsed <= 0` bỏ qua nhịp đầu tiên (`elapsed == 0`), khiến 50 ticks niệm chú chỉ gây sát thương 9 lần (`22.5` thường / `31.5` Netherite) thay vì 10 lần (`25.0` thường / `35.0` Netherite như mô tả trong `vi_vn.json`).
   - **Cách khắc phục**: Đổi điều kiện thành `elapsed < 0 || elapsed % 5 != 0`, đảm bảo tia nhiệt bắn đủ 10 nhịp (`elapsed = 0, 5, 10, ..., 45`), đạt chuẩn **25.0** sát thương (Thường) và **35.0** sát thương (Netherite).

9. **Sửa lỗi truyền cứng `EquipmentSlot.MAINHAND` khi dùng gậy ở tay phụ (`OFFHAND`)**:
   - **Vấn đề cũ**: `Guardian Lens`, `Resonant Echo` và `MagicMirrorItem` (bản Forge 1.20.1) truyền cứng `EquipmentSlot.MAINHAND` khi trừ độ bền, gây lỗi hiển thị khi người chơi cầm vật phẩm ở tay phụ (`OFFHAND`).
   - **Cách khắc phục**: Sử dụng `player.getUsedItemHand()` và `LivingEntity.getSlotForHand(player.getUsedItemHand())` đồng bộ trên cả 3 loader.

10. **Đồng bộ hiệu ứng lấp lánh (`isFoil`) cho `MagicMirrorItem` và `VoidTranslocatorItem` trên 1.21.1**:
    - **Vấn đề cũ**: Bản Forge 1.20.1 có hiệu ứng lấp lánh (`isFoil = true`) cho Gương Ma Thuật và Gậy Dịch Chuyển, nhưng bản NeoForge 1.21.1 và Fabric 1.21.1 bị thiếu.
    - **Cách khắc phục**: Bổ sung `@Override public boolean isFoil(ItemStack stack) { return true; }` vào `MagicMirrorItem` và `VoidTranslocatorItem` trên NeoForge 1.21.1 và Fabric 1.21.1.

11. **Loại bỏ ký tự UTF-8 BOM (`ï»¿`) khỏi 41 file JSON**:
    - **Vấn đề cũ**: 41 file `.json` trong `shared-resources` và `forge-1.20.1` chứa byte BOM ở đầu file.
    - **Cách khắc phục**: Chuyển đổi toàn bộ 41 file sang chuẩn `UTF-8` không BOM.

12. **Dọn dẹp tên dự án cũ trong các file script `.bat` / `gradlew`**:
    - **Vấn đề cũ**: `fabric-1.21.1/build.bat` và `run-client.bat` hiển thị `[Vein Farming Fabric 1.21.1]`, còn các file `gradlew`/`gradlew.bat` trỏ cache về `.gradle/camera-lockon-distributions`.
    - **Cách khắc phục**: Đồng bộ nhãn hiển thị thành `[Velorise's Staves Fabric 1.21.1]` và thư mục cache thành `.gradle/velorise-staves-distributions`.
