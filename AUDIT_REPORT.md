# BÁO CÁO KIỂM TOÁN KỸ THUẬT TOÀN DIỆN (FULL TECHNICAL AUDIT REPORT)
**Dự án:** `velorise_staves` (Velorise's Staves — Genshin Impact Elemental Combat & Archon Arsenal)  
**Phạm vi kiểm toán:** `forge-1.20.1` (Java 17), `neoforge-1.21.1` (Java 21), `fabric-1.21.1` (Java 21), `shared-resources/`  
**Vai trò thực hiện:** Senior Minecraft Modding Architect (NeoForge / Fabric / Forge / Mixin / JVM Performance)

## 1. Tổng Quan Kiến Trúc & Đánh Giá Thiết Kế

### 1.1. Điểm mạnh kiến trúc (Architectural Strengths)
- **Kiến trúc Multi-Loader & Parity kỷ luật cao:**
  - Toàn bộ tài nguyên (`assets/velorise_staves`, `data/velorise_staves`, `pack.mcmeta`) được gom tập trung tại `shared-resources/` và nạp trực tiếp qua cấu hình Gradle, loại bỏ hoàn toàn tình trạng lệch texture, model, sound, hoặc bảng dịch giữa 3 loader.
  - **54 trên 74 class Java** (bao gồm toàn bộ **20 Shared Classes** bắt buộc trong `build-all.ps1`) đạt độ đồng nhất **100% SHA-256 byte-for-byte** giữa `neoforge-1.21.1` và `fabric-1.21.1`.
  - Hệ thống `ElementalGaugeManager` đóng vai trò **Thin Facade** điều phối sang các domain manager chuyên biệt (`MavuikaManager`, `RaidenManager`, `ZhongliManager`, `VentiManager`, `FurinaManager`, `NahidaManager`, `TsaritsaManager`, `XilonenManager`, `NeuvilletteManager`, `ArchonUltiManager`, `PlungeAndGliderManager`, `ElectrifiedWaterManager`, `EnvironmentalBlockManager`, `ElementalResonanceManager`, `CrystallizeShardManager`, `DendroCoreManager`).
- **Phân tách rõ ràng giữa `Omni Staff` và Vũ khí 7 Thần (`isArchon()`):**
  - Toàn bộ 7 vị Thần (`THOUSAND_BLAZING_SUNS`, `SPLENDOR`, `DAYBREAK`, `ENGULFING_LIGHTNING`, `THOUSAND_FLOATING_DREAMS`, `TSARITSA_BOW`/`TSARITSA_SPEAR`, `VORTEX`) được đánh dấu `isArchon() == true` và bị chặn hợp lệ khỏi công thức ghép cũng như cơ chế hấp thụ đe (Anvil) của `Omni Staff`. Chỉ các vũ khí không phải Thần (bao gồm 3 nhân vật Genshin: Neuvillette `GUARDIAN`, Xilonen `PEAK_PATROL_SONG`, Layla `NIGHTS_FOCUS`) mới có thể ghép vào `Omni Staff`.
- **Quản lý trạng thái tập trung với `PlayerGenshinState`:**
  - Sử dụng `FieldMapView<V>` bọc trên một `ConcurrentHashMap<UUID, PlayerGenshinState>` duy nhất cho 47 trường trạng thái chiến đấu, có cơ chế `pruneInactive(MinecraftServer)` định kỳ.
- **Đồng bộ Đa ngôn ngữ (i18n) hoàn chỉnh:**
  - Tất cả **9 ngôn ngữ** (`en_us`, `vi_vn`, `zh_cn`, `ja_jp`, `ko_kr`, `ru_ru`, `fr_fr`, `es_es`, `pt_br`) đều đạt **780/780 keys** đồng bộ tuyệt đối (0 key thiếu, 0 key dư, 100% valid JSON).

### 1.2. Điểm yếu & Rủi ro kỹ thuật phát hiện trước khi sửa (Key Weaknesses Identified)
- **Chi phí truy vấn không gian (`getEntitiesOfClass`) trong vòng lặp nóng (`tick()`):**
  - `HydroSalonSummonEntity.hasActiveSummon(Player)` được gọi 4 lần mỗi tick (cả Client và Server), quét hình hộp `128×128×128` (`inflate(64.0D)`) tới **80 lần/giây** khi người chơi cầm `Splendor of Tranquil Waters` nhưng chưa triệu hồi Salon Member (do thiếu negative cache).
  - `ElectrifiedWaterManager.tick(ServerLevel)` gọi `getEntitiesOfClass` trên mỗi vùng nước ở mọi tick ngay cả khi tick đó không vẽ tia điện (`now % 3`, `now % 4`) và không gây sát thương (`now % 20`), đồng thời gọi lặp lại `getEntitiesOfClass` lần thứ 2 bên trong `shockEntities()`.
- **Nguy cơ đệ quy sát thương (Re-entrancy / Recursive Damage Loops):**
  - `ElectrifiedWaterManager.shockEntities()` thiếu cờ `ThreadLocal<Boolean> IS_SHOCKING` và `RaidenManager` gọi `EnvironmentalBlockManager.applyElectroEnvironmentEffects()` bên ngoài khối `try/finally` của `IS_PROCESSING_RAIDEN_PROC`.
  - Trên `fabric-1.21.1`, `ZhongliManager.modifyFabricIncomingDamage()` (được gọi từ `@ModifyVariable` trong `LivingEntity.actuallyHurt` và `Player.actuallyHurt`) kích hoạt `applyImbuedSwordHit()` và `applyRaidenMusouIsshinHit()` vốn có thể gây phản ứng nguyên tố nổ (`Overloaded`, `Superconduct`, `Stellar Conduct`) gọi ngược vào `hurt() -> actuallyHurt()` khi chưa có cờ chặn đệ quy.
- **Thiếu/Sai Item ID trong Datapack (`shared-resources/data/`):**
  - `advancement/starry_night.json` tham chiếu sai ID `"velorise_staves:nights_focus"` thay vì `"velorise_staves:nights_focus_staff"`.
  - Các tag `staves.json`, `enchantable/elemental_mastery.json`, `enchantable/durability.json`, `enchantable/vanishing.json` và `advancement/netherite_upgrade.json` bị thiếu vũ khí của Venti, Raiden, Nahida, Tsaritsa (và một số bản Netherite).

## 2. Danh Sách Chi Tiết Các Lỗi Phát Hiện & Cách Đã Sửa

| Mã | Mức độ | Thành phần / File & Dòng | Nguyên nhân gốc rễ (Root Cause) | Giải pháp kỹ thuật đã áp dụng (Fix Applied) |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-01** | **CRITICAL** | `HydroSalonSummonEntity.java`<br>Dòng `63-65`, `140-156`<br>*(Cả 3 loader)* | `hasActiveSummon(Player)` được gọi 4 lần/tick (Client + Server Pre/Post tick). Khi người chơi mang trượng Furina nhưng không có summon, `ACTIVE_OWNER_SUMMON_UNTIL` rỗng nên hàm quét 40 ô túi đồ + gọi `getEntitiesOfClass(..., inflate(64.0D))` (vùng 128×128×128m) **80 lần/giây**. Ngoài ra, truy vấn trên Server không lọc `ownerUuid` và map tĩnh không được dọn khi player thoát game. | 1. Kiểm tra `ACTIVE_OWNER_SUMMON_UNTIL` trước khi quét túi đồ.<br>2. Bổ sung `NEGATIVE_SUMMON_SCAN_UNTIL` cache kết quả âm trong 10 tick (giảm **97.5%** số lần gọi `getEntitiesOfClass`).<br>3. Lọc `pid.equals(e.getOwnerUUID())` trên Server và thêm `clearPlayerState(UUID)` + `clearAll()`. |
| **BUG-02** | **CRITICAL** | `shared-resources/data/.../advancement/starry_night.json`<br>& `netherite_upgrade.json`<br>& `tags/item/*.json` | 1. `starry_night.json` ghi sai ID item thành `"velorise_staves:nights_focus"` và `"velorise_staves:netherite_nights_focus"` (ID đăng ký thực tế là `nights_focus_staff`), khiến datapack báo lỗi không parse được advancement.<br>2. `tags/item/staves.json` thiếu vũ khí của Venti & Raiden.<br>3. Các tag `enchantable/elemental_mastery.json`, `durability.json`, `vanishing.json` thiếu vũ khí của Venti, Raiden, Nahida, Tsaritsa.<br>4. `netherite_upgrade.json` thiếu 8 vũ khí Netherite mới. | 1. Sửa ID trong `starry_night.json` thành `nights_focus_staff` / `netherite_nights_focus_staff`.<br>2. Bổ sung đầy đủ vũ khí của Venti, Raiden, Nahida, Tsaritsa vào `staves.json`, `elemental_mastery.json`, `durability.json`, `vanishing.json` và `netherite_upgrade.json`. |
| **BUG-03** | **HIGH** | `ElectrifiedWaterManager.java`<br>Dòng `46-48`, `186-238`<br>& `RaidenManager.java`<br>Dòng `712`, `803`, `884`<br>*(Cả 3 loader)* | 1. `shockEntities()` thiếu `ThreadLocal<Boolean> IS_SHOCKING`, có thể gây vòng lặp đệ quy khi sát thương Điện Cảm kích hoạt hiệu ứng lan truyền.<br>2. `RaidenManager` gọi `EnvironmentalBlockManager.applyElectroEnvironmentEffects()` ngoài khối `IS_PROCESSING_RAIDEN_PROC`.<br>3. `ElectrifiedWaterManager.tick()` gọi `getEntitiesOfClass` mỗi tick kể cả ở các tick không dùng kết quả, và gọi lặp lại lần 2 trong `shockEntities()`. | 1. Thêm `ThreadLocal<Boolean> IS_SHOCKING` bảo vệ `shockEntities()` bằng `try/finally` và dùng `Set<UUID> shockedThisTick` để chống giật kép khi 2 vùng nước giao nhau.<br>2. Chỉ gọi `getEntitiesOfClass` khi `now % 4 == 0 \|\| now % 3 == 0 \|\| now % 20 == 0` và tái sử dụng danh sách `inWaterMobs` cho `shockEntities()`.<br>3. Đưa `applyElectroEnvironmentEffects` vào trong khối `try` của `IS_PROCESSING_RAIDEN_PROC`. |
| **BUG-04** | **HIGH** | `ZhongliManager.java`<br>Dòng `1395-1421`<br>*(`neoforge-1.21.1`, `fabric-1.21.1`)* | Trên Fabric, cả `LivingEntityDamageMixin` và `PlayerAttackMixin` đều hook `@ModifyVariable` vào `actuallyHurt` gọi `modifyFabricIncomingDamage()`. Bên trong hàm này, `applyImbuedSwordHit()` và `applyRaidenMusouIsshinHit()` có thể kích hoạt phản ứng nguyên tố gọi đệ quy `target.hurt() -> actuallyHurt()`, khiến sát thương phản ứng bị tính nhầm thành đòn chém thường một lần nữa. | Thêm `ThreadLocal<Boolean> IS_MODIFYING_FABRIC_DAMAGE` bọc quanh `applyImbuedSwordHit()` và `applyRaidenMusouIsshinHit()` bằng `try/finally` để chặn xử lý đệ quy. |
| **BUG-05** | **HIGH** | `XilonenManager.java`<br>Dòng `230-245` (1.20.1) / `246-261` (1.21.1)<br>*(Cả 3 loader)* | Trong `handleSkateDash()`, ngay trước lệnh `st.dashChain++;`, code gán cứng `st.dashChain = 0; st.dashWindowUntil = 0L; st.dashCooldownUntil = now + 10L;`. Do đó `st.dashChain` luôn bằng `1` và nhánh `if (st.dashChain >= 2)` (cooldown 30 tick sau 2 lần lướt liên tiếp) trở thành **dead code**, cho phép spam lướt mỗi 5 tick vô hạn. | Xóa 3 dòng gán đè `st.dashChain = 0` thừa, khôi phục đúng cơ chế chuỗi 2 lần lướt (5 tick giữa 2 lần lướt, 30 tick hồi chiêu sau lần lướt thứ 2). |
| **BUG-06** | **HIGH** | `FlamestriderBikeEntity.java`<br>Dòng `873`, `910`, `946`, `1003`, `1072`, `1241`, `1286`, `1608`, `1833`<br>*(Cả 3 loader)* | 1. **Desync Dash (`CD CD CC`):** Server đặt cooldown lướt lần 2 là `15` tick nhưng Client đặt `30` tick. Khi xoáy Donut (`CC`) >= 8 tick, Server reset `serverDashCooldownUntil` nhưng Client (`tickLocalControl`) không reset `clientDashCooldownTicks`, gây kẹt nút Dash ở Client.<br>2. **Mất Owner UUID:** `readAdditionalSaveData` gọi `setOwner(level.getPlayerByUUID(...))`, làm mất `DATA_OWNER_UUID` nếu chunk được nạp trước khi người chơi vào thế giới.<br>3. **Thiếu lọc đồng đội/Spectator:** 6 đòn đánh AoE của xe không lọc `!e.isSpectator()`, `!sp.isAlliedTo(e)`, và `!isRaidenProtectedAlly(sp, e)`. | 1. Đồng bộ cooldown chuỗi lướt thứ 2 ở Server/Client (`30` tick) và reset `clientDashCooldownTicks = 0` khi `clientContinuousSpinTicks >= 8`.<br>2. Lưu trực tiếp `DATA_OWNER_UUID` trong `readAdditionalSaveData`.<br>3. Thêm hàm `isValidBikeCombatTarget(sp, e)` cho cả 6 đòn AoE của xe. |
| **BUG-07** | **MEDIUM** | `TsaritsaManager.java`<br>Dòng `776`, `809-815`<br>& `RaidenManager.java` (`745`)<br>*(Cả 3 loader)* | 1. Nhánh Support Burst của Tsaritsa phát trùng lặp âm thanh `BEACON_POWER_SELECT` và `105` hạt particle ở tick 0 (ngoài âm thanh ở dòng 787 và particle khi nổ ở tick 38).<br>2. Chiêu cuối của Tsaritsa và Raiden chưa áp dụng giảm 10% hồi chiêu từ Cộng Hưởng Phong (`ResonanceType.ANEMO`). | 1. Xóa khối âm thanh/particle bị nhân đôi ở nhánh Support Burst.<br>2. Áp dụng giảm 10% cooldown Burst khi có `ResonanceType.ANEMO` cho cả Tsaritsa và Raiden. |
| **BUG-08** | **MEDIUM** | `EnvironmentalBlockManager.java`<br>Dòng `49-67`, `325`, `357`, `539`<br>*(Cả 3 loader)* | 1. `LAST_SENSOR_ZAP.keySet().retainAll(currentActiveSensors)` nằm bên trong vòng lặp `for (ServerLevel level : server.getAllLevels())`, khiến các chiều không gian khác (Nether/End) xóa sạch lịch sử Sculk Sensor của Overworld mỗi tick.<br>2. Khóa `LAST_OBSERVER_ZAP` dùng `(p.asLong() & 0xFFFFFFFFL)` làm mất 26 bit tọa độ X của `BlockPos`.<br>3. `PLAYER_TESLA_SCAN_CACHE` và `LAST_AMETHYST_ZAP` không được dọn ngay khi player thoát game hoặc đổi chiều không gian. | 1. Đưa `currentActiveSensors` và `retainAll()` ra ngoài vòng lặp `getAllLevels()`.<br>2. Đổi sang `(p.hashCode() & 0xFFFFFFFFL)` để giữ đủ thông tin X/Y/Z.<br>3. Thêm `clearPlayerState(UUID)` và gọi khi thoát game / đổi dimension. |
| **BUG-09** | **MEDIUM** | `VeloriseStavesMod.java` *(Fabric)*<br>`MavuikaManager.java`<br>`ElementalResonanceManager.java`<br>`ClientAuraRenderer.java` | 1. `fabric-1.21.1` chưa đăng ký `ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD` để gọi `PlayerGenshinState.onPlayerChangedDimension`.<br>2. `ElementalResonanceManager.clearPlayerState()` thiếu `GEO_RES_SHRED_UNTIL.remove(playerId)`.<br>3. `MavuikaManager.clearEntityStateInternal()` thiếu dọn `ICD_TRACKER`, `REACTION_ICD_TRACKER`, `LAST_PROXIMITY_SHOCK`, `LAST_THORNS_REFLECT`, `HydroSalonSummonEntity`, và `EnvironmentalBlockManager`.<br>4. `ClientAuraRenderer.onClientLogout()` trên NeoForge/Forge thiếu `OmniWheelOverlay.reset()`. | Bổ sung đầy đủ các lệnh dọn dẹp bộ nhớ khi thoát game, chết, đổi chiều không gian, và dừng máy chủ trên cả 3 loader. |
| **BUG-10** | **MEDIUM** | `ArchonUltiManager.java`<br>Dòng `237-246`<br>*(Cả 3 loader)* | Khi kích hoạt Venti Burst (`[R]`) bằng trượng `Daybreak` cầm ở **tay phụ (offhand)**, hàm quên ghi `VENTI_BURST_CD_UNTIL.put(id, now + cdTicks)`, cho phép xả chiêu cuối Venti liên tục 0 giây hồi chiêu. | Bổ sung tính toán `cdTicks` (kèm Anemo Resonance) và ghi `VENTI_BURST_CD_UNTIL.put(id, now + cdTicks)` cho nhánh offhand. |
| **BUG-11** | **MEDIUM** | `NeuvilletteManager.java`<br>Dòng `386-426`<br>*(Cả 3 loader)* | 1. `Guardian Lens` (Neuvillette — không phải Thần nên ghép được vào `Omni Staff` và cầm được ở offhand) dùng `stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND)` gán cứng `MAINHAND` và bỏ qua `ArcaneStaffItem.hurtStaffAndCheckBreak`.<br>2. `fireEquitableJudgmentTick()` tạo Stream pipeline (`.stream().filter().sorted().toList()`) mỗi 8 tick trong vòng lặp bắn tia nước. | 1. Xác định đúng tay cầm (`usedHand`) và gọi `ArcaneStaffItem.hurtStaffAndCheckBreak(stack, 1, player, usedHand)`.<br>2. Thay Stream bằng vòng lặp `ArrayList` + `distanceToSqr` + `targets.sort(...)` và lọc thêm `isRaidenProtectedAlly`. |
| **BUG-12** | **MEDIUM** | `RaidenVisualRenderer.java`<br>Dòng `41-44`<br>& `ClientInputHandler.java`<br>Dòng `26-36`, `389`, `420` | 1. `RaidenVisualRenderer.tickClientAttackHold()` gán cứng `ClientAuraTracker.localRaidenAttackHeld = false` mỗi tick, ghi đè trạng thái giữ chuột trái của `ClientInputHandler` khi đang trong trạng thái Musou Isshin.<br>2. `ClientInputHandler` (NeoForge/Forge) thiếu `stopDestroyBlock()` và reset `swinging` khi kéo cung Tsaritsa/Venti (không đồng bộ với Fabric), và `neoforge-1.21.1` chưa có fallback tên trường SRG cho `missTime`. | 1. Chỉ reset `localRaidenAttackHeld = false` trong `RaidenVisualRenderer` khi người chơi không ở trạng thái Musou Isshin.<br>2. Bổ sung `stopDestroyBlock()` + reset `swinging` khi kéo cung và chuỗi fallback `{"missTime", "f_91084_", "f_91078_"}`. |
| **BUG-13** | **LOW** | `VentiManager.java` (`789`)<br>& `TemporaryWebManager.java` (`84-104`)<br>*(Cả 3 loader)* | 1. `VentiManager` còn sót 1 lệnh `level.sendParticles(ParticleTypes.ELECTRIC_SPARK, ...)` ở nhánh `ELECTRO_CHARGED`.<br>2. `TemporaryWebManager` bị nhân bản 3 lần liên tiếp đoạn code kiểm tra Advancement `EVENT_WEB_DEVELOPER` trong cùng vòng lặp. | 1. Xóa `ParticleTypes.ELECTRIC_SPARK` còn sót trong `VentiManager`.<br>2. Thu gọn 3 khối kiểm tra trùng lặp trong `TemporaryWebManager` về 1 khối duy nhất. |

## 3. Kiểm Tra Độ Đồng Nhất (SHA-256 Parity) & Tương Thích Đa Phiên Bản

### 3.1. Kết quả kiểm tra Parity 20 Shared Classes (`neoforge-1.21.1` vs `fabric-1.21.1`)
Toàn bộ **20/20 Shared Classes** bắt buộc trong `build-all.ps1` (cùng **34 class dùng chung khác**, tổng cộng **54/54 class**) đạt **100% SHA-256 Parity** sau khi áp dụng toàn bộ bản vá:
- `element/Element.java` — **MATCH**
- `element/ElementalReaction.java` — **MATCH**
- `element/GaugeState.java` — **MATCH**
- `element/ElementalGaugeManager.java` — **MATCH**
- `element/PlayerGenshinState.java` — **MATCH**
- `element/RaidenManager.java` — **MATCH**
- `element/ZhongliManager.java` — **MATCH**
- `element/VentiManager.java` — **MATCH**
- `element/MavuikaManager.java` — **MATCH**
- `client/ClientAuraTracker.java` — **MATCH**
- `client/ElementalCodexScreen.java` — **MATCH**
- `client/glider/WindGliderRenderer.java` — **MATCH**
- `client/character/XilonenVisualRenderer.java` — **MATCH**
- `client/character/NahidaVisualRenderer.java` — **MATCH**
- `client/character/RaidenVisualRenderer.java` — **MATCH**
- `client/character/ZhongliVisualRenderer.java` — **MATCH**
- `client/character/MavuikaVisualRenderer.java` — **MATCH**
- `client/character/NeuvilletteVisualRenderer.java` — **MATCH**
- `client/character/TsaritsaVisualRenderer.java` — **MATCH**
- `client/character/VentiVisualRenderer.java` — **MATCH**

### 3.2. Kiểm tra tương thích Java 17 (`forge-1.20.1`) vs Java 21 (`1.21.1`)
- Không sử dụng API chỉ có trên Java 21 (`List.getFirst()`, `List.getLast()`, `Math.clamp()`, SequencedCollection, hoặc Unnamed Variables `_`) trong bất kỳ file nào của `forge-1.20.1`.
- Các khác biệt API giữa Minecraft 1.20.1 và 1.21.1 (`CustomData` / `DataComponents` vs `stack.getTag()`, `SavedData.Factory` vs `DimensionDataStorage.computeIfAbsent`, `CustomPacketPayload` vs `SimpleChannel`, `GuiGraphics` render layers) được giữ tách biệt hoàn toàn.

## 4. Các Tối Ưu Hiệu Năng (TPS & FPS) Đã Thực Hiện & Đề Xuất Refactor OOP

### 4.1. Các tối ưu TPS/FPS đã áp dụng trực tiếp
1. **Giảm 97.5% truy vấn `getEntitiesOfClass` bán kính 64m của Furina (`HydroSalonSummonEntity.hasActiveSummon`):**
   - Đảo thứ tự kiểm tra cache `ACTIVE_OWNER_SUMMON_UNTIL` lên trước `carriesSplendorStaff(player)` (tránh quét 40 ô inventory mỗi frame khi đã có cache) và bổ sung `NEGATIVE_SUMMON_SCAN_UNTIL` (10 ticks TTL).
2. **Giảm 50%+ truy vấn `getEntitiesOfClass` trong `ElectrifiedWaterManager.tick()`:**
   - Chỉ truy vấn thực thể trong vùng nước ở các tick thực sự cần xử lý (`now % 4 == 0 || now % 3 == 0 || now % 20 == 0`) và truyền thẳng danh sách `inWaterMobs` vào `shockEntities()` thay vì gọi `getEntitiesOfClass` lần thứ 2.
3. **Loại bỏ cấp phát Stream và căn bậc hai (`Math.sqrt`) trong tia nước của Neuvillette (`NeuvilletteManager.fireEquitableJudgmentTick`):**
   - Thay chuỗi `.stream().filter(...).sorted(...).toList()` và `.distanceTo(c)` bằng vòng lặp `ArrayList` trực tiếp kết hợp `.distanceToSqr(c)`.

### 4.2. Lộ trình Refactor OOP cho các phiên bản tiếp theo (Không phá vỡ 20 Shared Classes)
Để giảm trùng lặp code hơn nữa giữa 9 Archon/Character Managers trong các bản cập nhật tới mà vẫn giữ nguyên danh sách 20 Shared Classes của `build-all.ps1`, khuyến nghị áp dụng 3 bước sau:
1. **Chuẩn hóa `DashChainController` (Value Object nội bộ trong `PlayerGenshinState`):**
   - Hiện tại logic lướt 2 nhịp (`dashChain`, `dashWindowUntil`, `dashCooldownUntil`, 5t gap -> 30t cooldown) đang được viết riêng trong `XilonenManager`, `FlamestriderBikeEntity`, và `TsaritsaManager`. Có thể đóng gói thành một static helper hoặc nested class `PlayerGenshinState.DashChainState` với phương thức `tryConsumeDash(long now, int gapTicks, int chainCooldownTicks, int windowTicks)`.
2. **Chuẩn hóa `CombatTargetPredicates` trong `ElementalGaugeManager`:**
   - Gom các biểu thức lambda lọc mục tiêu (`e -> e.isAlive() && !e.isRemoved() && !e.isSpectator() && e != attacker && !attacker.isAlliedTo(e) && !isRaidenProtectedAlly(attacker, e)`) thành một phương thức tĩnh `ElementalGaugeManager.isValidHostileTarget(LivingEntity attacker, LivingEntity candidate)` để tái sử dụng đồng nhất trên tất cả 9 nhân vật và thực thể triệu hồi.
3. **Chuẩn hóa `VertexPoseHelper` cho `client/character/*VisualRenderer.java`:**
   - Các hàm dựng hình học 3D thủ công (`vertex`, `quad`, `box`, `ring` với `Matrix4f` và `VertexConsumer`) hiện được khai báo `private static` riêng lẻ trong từng `*VisualRenderer.java`. Có thể gom các hàm vẽ hình học cơ bản vào `ElementalReactionBadgeRenderer` hoặc `ClientAuraTracker` để giảm ~400 dòng code trùng lặp ở tầng client renderer.


---

## 7. Cập Nhật Cân Bằng Tiến Trình, `electro_rod` & Rework 6 Vũ Khí Cũ (Milestone 4)

### 7.1. Nguyên Liệu Mới `velorise_staves:electro_rod` & Cây Tiến Trình Công Thức Chế Tạo
- **Đăng ký `electro_rod`**: Thêm vật phẩm `velorise_staves:electro_rod` trên cả 3 loader (`VeloriseStavesMod.java`), kèm texture 16×16 & 32×32, model JSON, bản dịch 9 ngôn ngữ và 10 công thức chuyển đổi 2 chiều giữa `electro_rod` và `blaze_rod`, `breeze_rod`, `frost_rod`, `geo_rod`, `hydro_rod`.
- **Cập nhật công thức trượng Lôi & Thất Thần (Non-Archon Staff Progression)**:
  - **Arc Caster (`arc_caster.json`)**: Ô 3 = `minecraft:amethyst_shard`, Ô 5 & 7 = `velorise_staves:electro_rod`.
  - **Dragonfire Scepter (`dragonfire_scepter.json`)**: Ô 3 = `minecraft:dragon_head`, Ô 5 = `electro_element` / `omni_element`, Ô 7 = `velorise_staves:electro_rod`.
  - **Nahida (`thousand_floating_dreams.json`)**: Ô 2, 4, 6, 8 = `dendro_element` / `omni_element`, Ô 5 = `#velorise_staves:dendro_staves` (trượng Thảo non-Archon).
  - **Raiden Shogun (`engulfing_lightning.json`)**: Ô 2, 3, 4 = `electro_element` / `omni_element`, Ô 5, 7 = `#velorise_staves:electro_staves` (trượng Lôi non-Archon).
  - **Zhongli (`vortex_vanquisher.json`)**: Ô 2, 3, 6 = `velorise_staves:geo_rod`, Ô 5 = `geo_element` / `omni_element`, Ô 7 = `#velorise_staves:geo_staves` (trượng Nham non-Archon).
  - **Venti (`the_daybreak_chronicles.json`)**: Ô 2, 4 = `#velorise_staves:anemo_staves` (trượng Phong non-Archon), Ô 3, 7 = `anemo_element` / `omni_element`, Ô 6, 8 = `minecraft:breeze_rod`.
  - **Tsaritsa (`tsaritsa_bow.json`)**: Ô 2, 4 = `#velorise_staves:cryo_staves` (trượng Băng non-Archon), Ô 3, 7 = `cryo_element` / `omni_element`, Ô 6, 8 = `velorise_staves:frost_rod`.
  - **Omni Staff (`omni_staff.json`)**: Giữ nguyên quy tắc chỉ ghép từ 7 vũ khí **non-Archon** (`!isArchon()`), không chấp nhận vũ khí Thất Thần.

### 7.2. Rework Toàn Diện `Dragonfire Scepter` (Non-Archon, Ghép Được Vào Omni Staff)
- **Chuột Trái (`NA` — `4.0s` CD)**: Bắn cầu năng lượng Rồng Ender (`32m`, nổ `3.5❤` Basic / `5.25❤` Netherite sát thương Lôi `2U`/`3U` và để lại vùng mây điện Rồng `6s`).
- **Chuột Phải (`E` Skill — `8.0s` CD)**: Triệu hồi **Rồng Ender Nhỏ (`1.0 block`)** bay tuần tra mượt mà quanh người chơi trong `30s`, tự động dịch chuyển theo chủ nhân nếu cách quá `18m` (giống đệ của Furina), và mỗi **`2.0s` (`40 ticks`)** bắn tia điện Lôi vào kẻ địch gần nhất trong `12m` (`1.0❤` Basic / `1.5❤` Netherite, `1.0U` Electro).
- **Chiêu Cuối (`[R]` — `20.0s` CD, `-10%` khi có Cộng Hưởng Phong)**:
  - **Biến thể 1 (Khi đã triệu hồi Rồng Nhỏ — *Skyfall Dive*)**: Ban `2.0s` I-frame; Rồng Nhỏ vút thẳng lên bầu trời rồi biến mất; `2.0s` sau, **Rồng Ender Lớn** lao thẳng từ trên cao đâm sầm xuống mặt đất cách người chơi `1 block` phía trước, gây sát thương Lôi diện rộng **`5×5 blocks`** (`8.0❤` Basic / `12.0❤` Netherite, `4.0U` Electro) và tạo vùng mây điện Rồng `6s`.
  - **Biến thể 2 (Khi chưa triệu hồi Rồng Nhỏ — *Sweeping Strafe*)**: Ban `1.8s` I-frame; **Rồng Ender Lớn** lượn vòng cung sà sát mặt đất theo hướng nhìn của người chơi, càn quét dải đất **`rộng 3 blocks × dài 10 blocks`** (`6.0❤` Basic / `9.0❤` Netherite, `3.0U` Electro).
- **Hoạt Ảnh Nhàn Rỗi 3D (`3.0s` đứng yên)**: Rồng Ender Nhỏ chuyển mượt sang trạng thái bay lượn tự do quanh người chơi (hoặc tự hiện ra nếu chưa triệu hồi), định kỳ bay ra ngay trước mặt chủ nhân, nghiêng đầu nhìn và vỗ cánh tương tác.
- **Nội Tại Ẩn — *Dragon-Wing Glider***: Khi mang `Dragonfire Scepter` (hoặc `Omni Staff` đã hấp thụ `Dragonfire Scepter`), cánh Phong Chi Dực biến thành **Rồng Ender 3D sải cánh ngang `5.0 blocks`** quắp vai người chơi bay đi với tốc độ lượn ngang **nhanh hơn +50%** (`0.63D`, nhanh hơn mặc định `0.42D` và thấp hơn Venti `0.84D`).

### 7.3. Nâng Cấp 5 Vũ Khí Cũ (`Resonant Echo`, `Inferno Rod`, `Frostbolt Wand`, `Wither Scepter`, `Shulker Seeker`)
- **Resonant Echo (`ECHO`)**: Giữ CD mặc định `8.0s`; thêm hiệu ứng **Resonance** giảm **`1.0s` CD** mỗi mục tiêu hợp lệ bị tia sóng âm xuyên trúng (tối đa **5 mục tiêu = `-5.0s` CD**), kèm bộ lọc chống exploit (loại bỏ `ArmorStand`, bù nhìn `dummy`/`mannequin`/`statue`, thực thể `NoAI`/`Invulnerable`, thú cưng/đồng minh, và bắt buộc mục tiêu phải thực sự mất máu `postHp < preHp`).
- **Inferno Rod (`INFERNO`) & Frostbolt Wand (`FROST`) — *3rd-Shot Finale***: Phát bắn thứ 3 trong chuỗi 3 phát liên hoàn bay nhanh hơn **+35%**, gây **+50% sát thương**, áp **`2.0U`** (`3.0U` Netherite) và kích nổ cầu lửa **`2.5m` AoE** (*Inferno Rod*) hoặc bão tuyết đóng băng mặt đất **`2.5m` AoE + Slowness IV** (*Frostbolt Wand*).
- **Wither Scepter (`WITHER`)**:
  - **Nội tại 1 — *Wither Carapace (`<= 50% HP`)***: Khi máu `<= 50%` và cầm trượng trên tay chính hoặc tay phụ, miễn nhiễm **100% mọi sát thương đạn tầm xa (Projectiles)** giống giáp Wither Boss Phase 2.
  - **Nội tại 2 — *Withering Contagion***: Kẻ địch bị trúng đòn của Wither Scepter nếu bị hạ gục trong `8s` (hoặc chết khi đang mang Wither + ấn Thảo) sẽ phát nổ **Wither + Thảo bán kính `3.0m`** (`1.5❤`➔`2.25❤` sát thương Thảo, lan `1.5U` Dendro + `Wither II` `4s` cho quái xung quanh).
- **Shulker Seeker (`SHULKER`)**:
  - **Chủ động — *3rd-Shot Gravity Slam***: Viên đạn thứ 3 trong chuỗi 3 phát trúng đích sẽ hất tung mục tiêu lên cao cực nhanh (`+0.78Y`) rồi sau `0.5s` **dập mạnh mục tiêu xuống đất (`-1.45Y`)**, gây chấn động Lôi **`2.5m` AoE** (`2.5❤`➔`3.75❤` + `2.0U` Electro).
  - **Nội tại — *Shulker Shell Guard***: Khi cầm `Shulker Seeker` trên **tay chính (Main Hand)**, giảm **30%** (**40%** ở bản Netherite) mọi sát thương nhận vào như vỏ Shulker đóng nắp.
