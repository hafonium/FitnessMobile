# UI Reconstruction Specification

## 1. Source

- **Loại giao diện:** Màn hình ứng dụng di động iOS/Android (Dark Theme).
- **Kích thước Viewport (ước tính):** ~390 × 844 px (Tỷ lệ khung hình chuẩn iPhone hiện đại ~9:19.5).
- **Thành phần hệ thống (System Chrome):**
  - **Status bar (Top):** Giờ `08:18` (góc trái), icon chế độ máy bay, Wi-Fi, pin `72%` (góc phải).
  - **Home Indicator (Bottom):** Thanh gạch ngang trắng bo tròn ở đáy màn hình.
- **Phân loại màn hình:** Màn hình Khám phá (**Discover Tab**) - Chuyên mục **"Walk & Run"** (Đi bộ & Chạy bộ).

---

## 2. Visual Summary

Giao diện là màn hình khám phá kế hoạch luyện tập theo chủ đề "Walk & Run" với phong cách **Dark Mode thuần đen (`#000000`)** hiện đại, hình ảnh trực quan và độ tương phản cao.

Bố cục gồm 5 phân vùng chính từ trên xuống:
1. **Top Category TabBar:** Thanh chọn danh mục luyện tập ngang ("At Home", "Gym", "Walk & Run" - đang active).
2. **Search Bar:** Thanh tìm kiếm dạng viên thuốc (pill shape) màu xám đen mờ.
3. **Hero Feature Card ("FREE MODE"):** Thẻ nổi bật với hình nền bản đồ giao thông đêm (Dark map), giới thiệu chế độ chạy tự do kèm nút bấm CTA "START" màu xanh dương.
4. **Training Plans Section:** Danh mục các gói tập dài hạn dạng danh sách thẻ ảnh (Image Cards) bo góc lớn, phủ text in hoa nổi bật.
5. **Bottom Navigation Bar:** Thanh điều hướng 5 tab cố định đáy, tab `Discover` đang được kích hoạt với icon la bàn nổi bật.

---

## 3. Screen Structure

```text
DiscoverWalkAndRunScreen
├── StatusBar (System)
│
├── FixedHeaderArea
│   ├── CategoryTabBar (Horizontal Scroll/Flex)
│   │   ├── TabItem ("At Home" - Inactive)
│   │   ├── TabItem ("Gym" - Inactive)
│   │   └── TabItem ("Walk & Run" - Active + Indicator Line)
│   └── SearchBarContainer
│       ├── SearchIcon (Magnifying Glass)
│       └── SearchPlaceholder ("Search workouts, plans...")
│
├── ContentScrollView (Vertical Stack)
│   ├── FreeModeHeroCard (Card with Map Background)
│   │   ├── MapBackgroundImage (Dark Night Map)
│   │   ├── HeroCardContent (Centered Vertical Stack)
│   │   │   ├── HeroTitle ("FREE MODE")
│   │   │   ├── HeroSubtitle ("Free running & walking with map")
│   │   │   └── HeroActionButton ("START")
│   │
│   └── TrainingPlansSection
│       ├── SectionTitle ("Training plans")
│       └── PlanCardList (Vertical Stack)
│           ├── PlanCard 1 ("Walking for Weight Loss")
│           │   ├── CardBackgroundImage (Couple walking outdoors)
│           │   └── CardOverlayContent (Left-aligned)
│           │       ├── ProgramDurationBadge ("20 WEEKS PROGRAM")
│           │       └── ProgramTitle ("WALKING FOR\nWEIGHT LOSS")
│           │
│           └── PlanCard 2 ("Running for...") (Partially visible)
│               ├── CardBackgroundImage (Man running)
│               └── CardOverlayContent (Left-aligned)
│                   ├── ProgramDurationBadge ("12 WEEKS PROGRAM")
│                   └── ProgramTitle ("RUNNING FOR...")
│
└── BottomNavigationBar (Fixed Bottom)
    ├── TabItem ("Personal" - Notebook Icon)
    ├── TabItem ("Classic" - Stopwatch Icon)
    ├── TabItem ("Discover" - Compass Icon in Solid Blue Circle - Active)
    ├── TabItem ("Daily" - Calendar Icon)
    └── TabItem ("Me" - User Icon)
```

---

## 4. Layout Specification

### 4.1. Screen Layout
| Thuộc tính | Giá trị ước tính |
|---|---|
| Width | 100% viewport (~390px) |
| Height | 100% viewport |
| Layout | Vertical flex (Header cố định + Content cuộn dọc + BottomNav cố định) |
| Background | `#000000` (Pure Black) |
| Horizontal Padding | ~16 px (cho Content & Search Bar) |

### 4.2. CategoryTabBar
| Thuộc tính | Giá trị ước tính |
|---|---|
| Layout | Horizontal flex, `align-items: center`, `justify-content: center` |
| Gap giữa các tab | ~20–24 px |
| Chiều cao bar | ~44–48 px |
| Active Indicator | Thanh line ngang màu trắng (`width: ~40px`, `height: ~3px`, `border-radius: 2px`) nằm ngay dưới chữ "Walk & Run" |

### 4.3. SearchBar
| Thuộc tính | Giá trị ước tính |
|---|---|
| Width | 100% (trong lề padding 16px) |
| Height | ~44–48 px |
| Layout | Horizontal flex, `align-items: center`, `padding: 0 16px`, gap: ~10px |
| Background | `#242426` (Xám tối mờ) |
| Border Radius | `9999px` (Full Pill) |

### 4.4. FreeModeHeroCard
| Thuộc tính | Giá trị ước tính |
|---|---|
| Width | 100% |
| Height | ~220–240 px |
| Layout | Centered Flex column (`justify-content: center`, `align-items: center`) |
| Border Radius | ~20–24 px |
| Overflow | `hidden` |
| Inner Content Gap | ~8–12 px |

---

## 5. Component Specifications

### 5.1. CategoryTabBar & TabItem
- **Mục đích:** Chuyển đổi giữa các phân hệ luyện tập khác nhau.
- **Tab Inactive ("At Home", "Gym"):**
  - Typography: Font-size ~16–18 px, Weight: Regular (400), Color: `#70747E` (Xám).
- **Tab Active ("Walk & Run"):**
  - Typography: Font-size ~20–22 px, Weight: Bold (800), Color: `#FFFFFF`.
  - Indicator: Thanh line ngang trắng bo góc nằm chính giữa phía dưới chữ ~4px.

### 5.2. SearchBar
- **Mục đích:** Tìm kiếm bài tập hoặc gói luyện tập.
- **Icon:** Kính lúp (Magnifying glass) màu xám sáng `#8E929A`, kích thước ~18×18 px.
- **Placeholder Text:** `Search workouts, plans...` (Font ~15px, Regular, Color: `#8E929A`).

### 5.3. FreeModeHeroCard
- **Mục đích:** Kích hoạt tính năng theo dõi GPS chạy/đi bộ tự do ngoài trời.
- **Background:** Ảnh vệ tinh/bản đồ đường phố ban đêm tông xanh navy tối (`#0B1B3D`), có lớp phủ tối nhẹ (dark overlay ~30%) để làm nổi bật chữ.
- **Nội dung:**
  - `HeroTitle`: "FREE MODE" (Font ~24–26 px, Extra-Bold 800, Color: `#FFFFFF`, Letter-spacing: ~0.5px).
  - `HeroSubtitle`: "Free running & walking with map" (Font ~14–15 px, Regular 400, Color: `#D1D5DB`).
  - `ActionButton ("START")`:
    - Geometry: Height ~44–48 px, Width: ~180–200 px, `border-radius: 9999px`.
    - Background: Xanh dương hoàng gia sáng `#0057FF`.
    - Text: "START" (Font ~16px, Extra-Bold 800, Color: `#FFFFFF`, in hoa toàn bộ).

### 5.4. TrainingPlansSection & PlanCard
- **Section Title:** "Training plans" (Font ~20–22 px, Bold 700, Color: `#FFFFFF`, margin-top ~20px, margin-bottom ~12px).
- **PlanCard (Thẻ gói tập):**
  - Geometry: Height ~180–200 px, Width: 100%, `border-radius: ~20px`, `overflow: hidden`, `margin-bottom: 16px`.
  - Background: Ảnh chụp nhiếp ảnh chất lượng cao (Outdoor fitness lifestyle).
  - Gradient Overlay: Lớp phủ chuyển màu đen mờ từ góc trái sang phải (`linear-gradient(to right, rgba(0,0,0,0.7) 0%, rgba(0,0,0,0.2) 60%, transparent 100%)`) giúp đọc rõ chữ.
  - Text Stack (Căn trái, `padding: ~20px`):
    - `Badge`: "20 WEEKS PROGRAM" / "12 WEEKS PROGRAM" (Font ~12–13 px, Bold 700, Color: `#FFFFFF`, Uppercase).
    - `Title`: "WALKING FOR\nWEIGHT LOSS" (Font ~22–24 px, Extra-Bold 800, Color: `#FFFFFF`, Line-height ~1.15, Uppercase).

### 5.5. BottomNavigationBar
- **Geometry:** Height ~64–70 px, Width 100%, `background: #000000`, `border-top: 1px solid #18191D`.
- **5 Tab Items:**
  1. `Personal`: Icon cuốn sổ ghi chú (`#70747E`) + text "Personal".
  2. `Classic`: Icon đồng hồ bấm giờ (`#70747E`) + text "Classic".
  3. `Discover` (**Active**): Icon la bàn trắng nằm trong hình tròn xanh dương `#0057FF` (đường kính ~28px) + text "Discover" màu xanh `#0057FF`.
  4. `Daily`: Icon tờ lịch (`#70747E`) + text "Daily".
  5. `Me`: Icon hình người đại diện (`#70747E`) + text "Me".

---

## 6. Typography

| Role | Estimated Size | Weight | Line Height | Color | Sample Text |
|---|---:|---|---:|---|---|
| Active Category Tab | 20–22 px | Bold (800) | Normal | `#FFFFFF` | `Walk & Run` |
| Inactive Category Tab | 16–18 px | Regular (400) | Normal | `#70747E` | `At Home`, `Gym` |
| Hero Card Title | 24–26 px | Extra-Bold (800) | ~30 px | `#FFFFFF` | `FREE MODE` |
| Hero Card Subtitle | 14–15 px | Regular (400) | ~18 px | `#D1D5DB` | `Free running & walking with map` |
| Primary CTA Button | 16 px | Extra-Bold (800) | Normal | `#FFFFFF` | `START` |
| Section Heading | 20–22 px | Bold (700) | ~26 px | `#FFFFFF` | `Training plans` |
| Plan Card Title | 22–24 px | Extra-Bold (800) | ~28 px | `#FFFFFF` | `WALKING FOR WEIGHT LOSS` |
| Plan Duration Badge | 12–13 px | Bold (700) | ~16 px | `#FFFFFF` | `20 WEEKS PROGRAM` |
| Search Input Text | 15 px | Regular (400) | Normal | `#8E929A` | `Search workouts, plans...` |
| TabBar Label | 10–11 px | Medium (500) | Normal | `#70747E` / `#0057FF` | `Personal`, `Discover` |

---

## 7. Color Palette

| Token | Estimated Hex | Usage |
|---|---|---|
| `canvas-background` | `#000000` | Nền tổng thể màn hình |
| `surface-search` | `#242426` | Nền thanh tìm kiếm |
| `primary-accent` | `#0057FF` | Nút bấm START, Icon/Label Active Tab Discover |
| `text-primary` | `#FFFFFF` | Tiêu đề lớn, tên gói tập, tab active |
| `text-secondary` | `#D1D5DB` | Phụ đề hero card |
| `text-muted` | `#70747E` / `#8E929A` | Tab inactive, icon inactive, placeholder |
| `overlay-gradient` | `rgba(0, 0, 0, 0.65)` | Phủ chuyển màu trên các Image Cards |
| `border-divider` | `#18191D` | Viền ngăn cách Bottom Navigation Bar |

---

## 8. Spacing and Sizing Tokens

- **Góc bo (Border Radii):**
  - `radius-pill`: `9999px` (Nút START, Search Bar, Indicator)
  - `radius-card`: `20–24 px` (Hero Map Card, Training Plan Cards)
- **Khoảng cách (Spacing Scale):**
  - `screen-padding-h`: `16 px`
  - `section-gap`: `20–24 px`
  - `card-gap`: `14–16 px`
  - `inner-card-padding`: `20 px`
- **Kích thước điều khiển:**
  - `search-bar-height`: `46 px`
  - `start-button-height`: `46 px`
  - `bottom-nav-height`: `64 px`
  - `active-indicator-width`: `40 px`

---

## 9. Text Content

- **Category Bar:**
  - `At Home`
  - `Gym`
  - `Walk & Run`
- **Search Bar:**
  - `Search workouts, plans...`
- **Hero Card:**
  - `FREE MODE`
  - `Free running & walking with map`
  - `START`
- **Training Plans Section:**
  - `Training plans`
  - `20 WEEKS PROGRAM`
  - `WALKING FOR WEIGHT LOSS`
  - `12 WEEKS PROGRAM`
  - `RUNNING FOR...`
- **Bottom Navigation:**
  - `Personal`, `Classic`, `Discover`, `Daily`, `Me`

---

## 10. Assets and Icons

| Component | Asset / Icon | Size | Description |
|---|---|---:|---|
| SearchBar | Magnifying Glass | 18×18 px | Icon kính lúp nét mảnh màu xám nhạt |
| Hero Card | Map Background | Full Card | Ảnh texture bản đồ đường phố ban đêm (Dark GPS map) |
| Plan Card 1 | Couple Walking | Full Card | Ảnh cặp đôi thể thao đi bộ dưới cầu cạn |
| Plan Card 2 | Man Running | Full Card | Ảnh nam thanh niên đeo tai nghe đang chạy bộ |
| BottomNav | Notebook | 20×20 px | Icon cuốn sổ tay có 3 dòng kẻ |
| BottomNav | Stopwatch | 20×20 px | Icon đồng hồ bấm giờ thể thao |
| BottomNav | Compass (Active) | 28×28 px | Icon la bàn trắng nằm trong khối tròn xanh `#0057FF` |
| BottomNav | Calendar | 20×20 px | Icon bloc lịch |
| BottomNav | Me (User) | 20×20 px | Icon silhouette đầu người |

---

## 11. Interaction and State Observations

### Observed (Trực quan):
- **Category Tab:** Tab `Walk & Run` đang được kích hoạt (cỡ chữ lớn hơn, màu trắng, có thanh gạch chân).
- **Navigation Bar:** Tab `Discover` (vị trí thứ 3) đang được chọn.

### Inferred (Tương tác suy luận):
- Chạm vào `At Home` hoặc `Gym`: Chuyển đổi danh mục bài tập tương ứng và cập nhật danh sách Plan cards bên dưới.
- Chạm vào thanh Search: Kích hoạt bàn phím và mở màn hình tìm kiếm chi tiết.
- Chạm vào nút `START`: Mở màn hình GPS Tracking trực tiếp để bắt đầu đếm bước, đo quãng đường và vẽ bản đồ chạy/đi bộ.
- Chạm vào từng `PlanCard`: Mở màn hình chi tiết lộ trình tập (danh sách tuần, các buổi tập theo ngày).

---

## 12. Responsive Observations

- Màn hình thiết kế tối ưu theo chiều dọc của thiết bị di động.
- Danh mục bài tập (`ContentScrollView`) cuộn dọc mượt mà phía dưới thanh tìm kiếm.
- Chiều cao các `PlanCard` có thể cố định (~180–200px) trong khi chiều rộng co giãn `100%` theo chiều ngang thiết bị (đáp ứng tốt từ iPhone mini đến màn hình Max/Plus và Tablet).

---

## 13. Reconstruction Priorities

1. **Dark Theme Visual Identity:** Đảm bảo độ sâu màu nền đen tuyền `#000000` và độ nổi bật của các Image Cards.
2. **Typography Hierarchy & Casing:** Toàn bộ tiêu đề chương trình tập (`20 WEEKS PROGRAM`, `WALKING FOR WEIGHT LOSS`, `FREE MODE`, `START`) bắt buộc viết **IN HOA (ALL CAPS)** với font chữ đậm (Extra-Bold/800).
3. **Card Overlays:** Sử dụng lớp phủ gradient đen mờ lên ảnh nền để đảm bảo chữ trắng luôn sắc nét và dễ đọc.
4. **Hero Card Button & Indicator:** Đúng mã màu xanh dương `#0057FF` cho nút "START" và icon Active Tab.

---

## 14. Uncertainties

- *Số lượng gói tập:* Danh sách gói tập bên dưới có thể kéo dài nhiều phần tử hơn (đang cuộn dở ở thẻ thứ 2).
- *Thanh TabBar trên cùng:* Chưa rõ thanh Category TabBar có hỗ trợ vuốt ngang (horizontal scroll) khi có nhiều hơn 3 danh mục hay không.

---

## 15. Implementation Notes

### Hướng dẫn dành cho Engineering Agent triển khai:

1. **Kiến trúc Component khuyến nghị:**
   - `CategoryTabHeader`: Chứa danh sách tab và thanh trượt indicator active.
   - `SearchInputBar`: Component input tìm kiếm bo tròn pill-shape.
   - `FreeModeHeroCard`: Component thẻ bản đồ nhận action `onStart`.
   - `TrainingPlanCard`: Component tái sử dụng nhận props `{ duration, title, imageUrl, onPress }`.
   - `AppBottomNavBar`: Navigation bar cố định ở chân màn hình với 5 tabs.

2. **Xử lý Layout & Image Overlay:**
   - Dùng `ImageBackground` hoặc CSS `position: relative` bọc `Image` bên dưới và `Overlay Container` bên trên.
   - Luôn áp dụng gradient đen phía sau text: `background: linear-gradient(90deg, rgba(0,0,0,0.75) 0%, rgba(0,0,0,0.1) 70%, transparent 100%)`.

3. **Safe Area:**
   - Khối Header cần tính toán padding-top theo `SafeArea.top`.
   - `BottomNavigationBar` cần tính toán padding-bottom theo `SafeArea.bottom` để không bị đè bởi thanh Home Indicator.