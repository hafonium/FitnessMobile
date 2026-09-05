# UI Reconstruction Specification

## 1. Source

- **Loại giao diện:** Mobile Application (iOS / Android Bottom Sheet Modal)
- **Kích thước ảnh chụp (ước tính):** ~390 × 844 px (Tỷ lệ khung hình chuẩn smartphone hiện đại ~9:19.5)
- **Phân loại Viewport:** Mobile Portrait
- **Bối cảnh hiển thị:** Modal/Bottom Sheet trượt lên từ cạnh dưới, phủ mờ một phần màn hình nền (Workout screen) phía sau.

---

## 2. Visual Summary

Giao diện là một **Bottom Sheet / Modal cài đặt bài tập ("Workout Settings")** trong ứng dụng thể hình/workout. Toàn bộ nội dung được thiết kế theo phong cách thẻ (card-based layout) sáng màu trên nền xám nhạt (`#F4F5F8`). 

Cấu trúc gồm 4 khối chính:
1. **Header:** Tiêu đề "Workout Settings" và nút đóng (close button) dạng tròn.
2. **Music Card:** Trình phát nhạc tích hợp đầy đủ (toggle switch, thông tin bài hát kèm thumbnail tròn, cụm điều khiển playback, thanh trượt âm lượng và liên kết điều hướng nguồn phát).
3. **Cụm thẻ cài đặt danh sách:** 3 thẻ bo góc riêng biệt điều chỉnh "Sound options", "Rest timer", và "Prep timer" với chevron điều hướng sang phải.
4. **Footer Action:** Nút hành động chính "DONE" dạng pill nổi bật màu xanh dương rực rỡ ở đáy màn hình.

---

## 3. Screen Structure

```text
ModalContainer (Bottom Sheet)
├── ModalHeader
│   ├── Title ("Workout Settings")
│   └── CloseButton (IconButton '✕')
│
└── ContentScrollView / Stack
    ├── MusicCard (Card container)
    │   ├── CardHeader
    │   │   ├── SectionLabel ("Music")
    │   │   └── ToggleSwitch (Active: ON)
    │   ├── TrackInfoRow
    │   │   ├── AlbumThumbnail (Circular image)
    │   │   └── TrackMeta (Vertical stack: Title + Subtitle)
    │   ├── PlaybackControls (Horizontal flex)
    │   │   ├── RepeatButton (IconButton)
    │   │   ├── PrevButton (IconButton)
    │   │   ├── PlayPauseButton (Primary circular filled)
    │   │   ├── NextButton (IconButton)
    │   │   └── QueueButton (IconButton)
    │   ├── VolumeSliderRow (Horizontal flex)
    │   │   ├── VolumeLowIcon
    │   │   ├── SliderTrack (Filled + Thumb + Unfilled)
    │   │   └── VolumeHighIcon
    │   └── SecondaryLinkRow
    │       └── TextLink ("Stream audio from other apps >")
    │
    ├── SettingCard: SoundOptions
    │   ├── SettingLabel ("Sound options")
    │   └── ChevronRightIcon
    │
    ├── SettingCard: RestTimer
    │   ├── SettingLabel ("Rest timer")
    │   └── ValueBadgeRow ("Default" + ChevronRightIcon)
    │
    ├── SettingCard: PrepTimer
    │   ├── SettingLabel ("Prep timer")
    │   └── ValueBadgeRow ("15 secs" + ChevronRightIcon)
    │
    └── BottomActionBar
        └── PrimaryButton ("DONE")
```

---

## 4. Layout Specification

### Modal Container (Bottom Sheet)
| Thuộc tính | Giá trị ước tính |
|---|---|
| Position | Fixed/Absolute dính đáy màn hình (overlay trượt từ dưới lên) |
| Width | 100% viewport |
| Height | Tự co giãn theo nội dung (~85–90% chiều cao màn hình) |
| Layout | Vertical flex column |
| Padding | ~16px (trái/phải), ~16px (trên), ~24px (dưới an toàn) |
| Gap giữa các Cards | ~10–12 px |
| Background | `#F4F5F8` (Xám rất nhạt) |
| Border Radius | Bo góc trên: Top-Left ~24px, Top-Right ~24px |

---

## 5. Component Specifications

### 5.1. ModalHeader
- **Mục đích:** Hiển thị tên màn hình cài đặt và cung cấp nút đóng.
- **Layout:** Horizontal flex, `justify-content: space-between`, `align-items: center`.
- **Background:** Trong suốt hoặc đồng màu nền modal.
- **Kích thước:** Chiều cao ~48–56 px.
- **Phần tử con:**
  - `Title`: "Workout Settings" (Font đậm, cỡ chữ lớn).
  - `CloseButton`: Nút tròn xám nhạt (`#E2E4E9`), icon dấu nhân `✕` màu xám trung tính, đường kính ~32 px.

### 5.2. MusicCard
- **Mục đích:** Quản lý và điều khiển nhạc nền tập luyện.
- **Layout:** Vertical flex column, `padding: 16px`, `border-radius: 16px`, `background: #FFFFFF`.
- **Chi tiết các khối con:**
  1. **Header hàng 1:** Chữ "Music" (Bold) bên trái; Toggle Switch màu xanh dương (Active/ON) bên phải.
  2. **Track Info hàng 2:**
     - Avatar/Thumbnail: Hình tròn đường kính ~44–48 px.
     - Text stack: Tiêu đề "Dancing All Night" (Bold, ~16px), phụ đề "Home Workout Music" (Regular, ~13px, màu xám).
  3. **Playback Controls hàng 3:** 5 icon xếp ngang cách đều (`justify-content: space-between`, `align-items: center`):
     - Repeat/Loop icon (Outline, ~20px).
     - Previous track icon (Filled, ~20px).
     - Play/Pause Button: Nút tròn đặc màu xanh dương `#0057FF`, đường kính ~48–52 px, icon Play màu trắng căn giữa.
     - Next track icon (Filled, ~20px).
     - Playlist/List icon (Outline, ~20px).
  4. **Volume Slider hàng 4:**
     - Icon loa nhỏ bên trái, icon loa lớn bên phải.
     - Thanh trượt ở giữa: Phần đã kéo có màu xanh dương `#0057FF` kèm con trỏ tròn (Thumb ~12px), phần còn lại màu xám nhạt (`#E5E7EB`).
  5. **Secondary Link hàng 5:**
     - Text link căn giữa: "Stream audio from other apps >", màu xám `#6B7280`, font ~12–13 px.

### 5.3. SettingCard (Dùng cho "Sound options", "Rest timer", "Prep timer")
- **Mục đích:** Hiển thị hàng điều hướng cài đặt nhanh.
- **Geometry:** Height ~56–60 px, Width: 100%, `border-radius: 16px`, `background: #FFFFFF`.
- **Layout:** Horizontal flex, `justify-content: space-between`, `align-items: center`, `padding: 0 16px`.
- **Biến thể:**
  - *Dạng tiêu chuẩn ("Sound options"):* Title bên trái + Chevron phải.
  - *Dạng có giá trị ("Rest timer", "Prep timer"):* Title bên trái + Giá trị hiện tại màu xám ("Default", "15 secs") kèm Chevron bên phải.

### 5.4. PrimaryActionButton ("DONE")
- **Mục đích:** Xác nhận và đóng cài đặt.
- **Geometry:** Height ~52–56 px, Width: 100%, `border-radius: 9999px` (Full Pill).
- **Appearance:** `background: #0057FF`, text màu trắng `#FFFFFF`, font-weight 700, chữ in hoa (ALL CAPS).

---

## 6. Typography

| Role | Estimated Size | Weight | Color | Sample Text |
|---|---:|---|---|---|
| Modal Title | 20–22 px | Bold (700) | `#111827` | `Workout Settings` |
| Card Section Title | 15–16 px | Semi-Bold (600) | `#111827` | `Music`, `Sound options`, `Rest timer`, `Prep timer` |
| Track Title | 15–16 px | Bold (700) | `#111827` | `Dancing All Night` |
| Track Subtitle | 12–13 px | Regular (400) | `#71767F` | `Home Workout Music` |
| Inline Link | 12–13 px | Medium (500) | `#5A606A` | `Stream audio from other apps >` |
| Value Label | 14–15 px | Regular (400) / Medium | `#71767F` | `Default`, `15 secs` |
| CTA Button Text | 16 px | Bold (700) | `#FFFFFF` | `DONE` |

---

## 7. Color Palette

| Token | Estimated Hex | Usage |
|---|---|---|
| `canvas-backdrop` | `rgba(0, 0, 0, 0.5)` | Nền mờ phía sau modal |
| `surface-modal` | `#F4F5F8` | Nền tổng thể của Bottom Sheet |
| `surface-card` | `#FFFFFF` | Nền các thẻ trắng nổi trên modal |
| `primary-accent` | `#0057FF` | Nút DONE, nút Play, Toggle ON, thanh Slider |
| `text-primary` | `#111827` | Tiêu đề chính, tên bài hát, nhãn cài đặt |
| `text-secondary` | `#71767F` | Phụ đề, giá trị phụ (Default, 15 secs), icon âm lượng |
| `neutral-surface-dim` | `#E2E4E9` | Nền nút close tròn, track slider chưa kéo |
| `icon-neutral` | `#111827` | Các icon điều khiển playback (Next, Prev, Loop, List) |

---

## 8. Spacing and Sizing Tokens

- **Spacing Scale:**
  - `xs`: 4px
  - `sm`: 8px
  - `md`: 12px – 16px (Card padding & gap)
  - `lg`: 20px – 24px (Bottom margin & page margin)
- **Border Radii:**
  - `radius-sheet`: ~24px (Góc trên của bottom sheet)
  - `radius-card`: ~16px (Bo tròn thẻ con)
  - `radius-full`: 9999px (Pill buttons, thumbnail, play button)
- **Icon Sizes:**
  - Standard Icon: ~18–20 px
  - Volume Icon: ~16 px
  - Play Button: ~48–52 px

---

## 9. Text Content

- **ModalHeader:**
  - `Workout Settings`
- **MusicCard:**
  - `Music`
  - `Dancing All Night`
  - `Home Workout Music`
  - `Stream audio from other apps >`
- **Setting Items:**
  - `Sound options`
  - `Rest timer`
  - `Default`
  - `Prep timer`
  - `15 secs`
- **Bottom Button:**
  - `DONE`

---

## 10. Assets and Icons

| Component | Asset / Icon | Size | Description |
|---|---|---:|---|
| Header | Close Icon | 14×14 px | Biểu tượng `✕` nằm trong vòng tròn xám |
| MusicCard | Album Art | 44×44 px | Ảnh bìa tròn hiển thị sân khấu/sự kiện ca nhạc |
| MusicCard | Repeat Icon | 20×20 px | Mũi tên xoay vòng lặp |
| MusicCard | Prev / Rewind | 18×18 px | Icon 2 tam giác kép quay trái |
| MusicCard | Play Icon | 22×22 px | Biểu tượng tam giác Play màu trắng |
| MusicCard | Next / Fast-Forward | 18×18 px | Icon 2 tam giác kép quay phải |
| MusicCard | Playlist Icon | 18×18 px | Biểu tượng 3 gạch ngang kèm bullet tròn |
| MusicCard | Volume Low / High | 16×16 px | Biểu tượng hình loa phát âm thanh |
| Setting Cards | Chevron Right | 14×14 px | Mũi tên điều hướng `>` màu xám nhạt |

---

## 11. Interaction and State Observations

### Observed (Trực tiếp quan sát được):
- **Music Toggle Switch:** Đang ở trạng thái `ON` (nền xanh, nút gạt sang phải).
- **Music Player:** Đang hiển thị trạng thái `Play` (icon nút chính là hình tam giác Play).
- **Volume Slider:** Đang ở mức ~35–40% âm lượng.

### Inferred (Hành vi tương tác suy luận):
- Chạm vào nút Play: Bắt đầu phát nhạc, icon chuyển thành Pause `❚❚`.
- Tắt Toggle "Music": Có thể làm mờ/ẩn cụm điều khiển phát nhạc bên dưới.
- Chạm vào "Sound options", "Rest timer", "Prep timer": Mở popup/màn hình con để tùy chỉnh thông số chi tiết.
- Chạm vào nút "DONE" hoặc nút `✕` trên header: Đóng bottom sheet và áp dụng cài đặt.

---

## 12. Responsive Observations

- Đây là giao diện chuyên biệt cho màn hình di động (Mobile App).
- Chiều rộng các thẻ và nút DONE co giãn 100% theo chiều rộng viewport trừ đi phần padding hai bên (~16px mỗi bên).
- Với màn hình có chiều cao ngắn, danh sách các card bên trong nên được bọc trong một vùng cuộn dọc (`ScrollView`) để tránh tràn giao diện đè lên nút DONE.

---

## 13. Reconstruction Priorities

1. **Hierarchy & Card Layout:** Tạo đúng cấu trúc bottom sheet với nền xám nhạt và các thẻ bo tròn nền trắng.
2. **Music Widget Structure:** Sắp xếp chuẩn xác hàng thông tin bài hát, cụm 5 nút playback và thanh volume slider.
3. **Primary Accent Color:** Sử dụng đồng nhất mã màu xanh dương rực rỡ (`#0057FF`) cho nút DONE, Play button, Toggle switch và Slider fill.
4. **Typography & Spacing:** Cân đối font size và khoảng cách lề (padding/gap) giữa các thẻ cài đặt.

---

## 14. Uncertainties

- *Font family chính xác:* Không xác định được chính xác 100% tên font (có thể là SF Pro, Roboto, hoặc Poppins/Inter).
- *Hiệu ứng cuộn:* Chưa xác định được nút DONE cố định (sticky bottom) hay cuộn cùng danh sách khi danh sách dài ra.
- *Trạng thái Toggle OFF:* Chưa rõ các nút điều khiển nhạc có bị disabled (làm mờ) hay ẩn hoàn toàn khi tắt toggle Music.

---

## 15. Implementation Notes

- **Component hóa:** Nên tách thành các component tái sử dụng:
  - `BottomSheetContainer` (xử lý backdrop + border-radius trên).
  - `MusicPlayerCard` (gồm toggle, track info, playback control, volume).
  - `SettingNavigationItem` (tái sử dụng cho Sound options, Rest timer, Prep timer).
  - `PrimaryButton` (nút bấm dạng pill).
- **Layout Model:** Sử dụng hoàn toàn **Flexbox** (Vertical flex cho tổng thể và Horizontal flex cho các hàng điều khiển), tránh dùng Absolute Positioning ngoại trừ vị trí nút Close hoặc Modal Overlay.