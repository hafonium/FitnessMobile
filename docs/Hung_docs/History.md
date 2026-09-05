# UI Reconstruction Specification

## 1. Source

- **Loại giao diện:** Màn hình ứng dụng di động (Mobile Application Screen - Lịch sử tập luyện).
- **Phân loại Viewport:** Mobile Portrait (~390 × 844 px hoặc tỷ lệ chuẩn 9:19.5).
- **Thành phần hệ thống (System Chrome):** Status bar ở cạnh trên (hiển thị giờ `17:01`, icon chuông, sóng mạng 4G, pin `18%`).
- **Mục đích màn hình:** Xem chi tiết lịch sử tập luyện theo tháng (Monthly Calendar) và danh sách bài tập theo tuần ("Weekly Summary").

---

## 2. Visual Summary

Giao diện là màn hình **History / Workout Logs (Lịch sử luyện tập)** theo phong cách Light Theme với hai khối nội dung chính nổi bật trên nền xám nhạt (`#F6F7F9`):

1. **Top Bar & Monthly Calendar Card:** Khối trên cùng chứa nút quay lại (Back arrow), tiêu đề "History" và toàn bộ lưới lịch tháng (Calendar Grid 7 cột) với ngày đã tập (`1`) được đánh dấu nổi bật bằng hình tròn màu xanh dương `#0057FF`.
2. **Weekly Summary Card:** Khối bên dưới tổng hợp tuần hiện tại (`Aug 30 - Sep 5`), gồm tiêu đề tóm tắt tổng số buổi/thời gian/calo, kèm danh sách 3 bài tập chi tiết đã hoàn thành trong ngày (Thumbnail ảnh, tên bài tập, giờ tập, thời lượng và lượng calo tiêu hao).
3. **Ad Banner Overlay:** Banner quảng cáo TikTok nổi ở chân màn hình.

---

## 3. Screen Structure

```text
HistoryScreen
├── StatusBar (System)
│
├── ContentScrollView (Vertical Stack)
│   ├── CalendarSectionCard (White background container)
│   │   ├── TopNavBar
│   │   │   ├── BackButton (Left Arrow '←')
│   │   │   └── ScreenTitle ("History")
│   │   └── MonthlyCalendarGrid
│   │       ├── WeekdayHeaders (S M T W T F S)
│   │       └── DaysGrid (5 rows × 7 columns)
│   │           ├── EmptySlots (S, M)
│   │           ├── ActiveDayBadge (Day 1: Blue circle with white text)
│   │           └── InactiveDays (Days 2 to 30)
│   │
│   ├── WeeklySummarySection
│   │   ├── SectionTitle ("Weekly Summary")
│   │   └── WeeklySummaryCard (White card container)
│   │       ├── SummaryCardHeader
│   │       │   ├── LeftGroup (DateRange "Aug 30 - Sep 5" + WorkoutCount "3 Workouts")
│   │       │   └── RightGroup (TotalDuration "⏱ 00:05" + TotalCalories "🔥 0.5 Kcal")
│   │       ├── CardDivider
│   │       └── WorkoutLogList (Vertical list of items)
│   │           ├── WorkoutItem 1
│   │           │   ├── Thumbnail (Day 1 - Lower Body photo)
│   │           │   └── ItemDetails (Timestamp + Title + Duration + Calories)
│   │           ├── ListDivider
│   │           ├── WorkoutItem 2
│   │           │   ├── Thumbnail (Abs Beginner photo)
│   │           │   └── ItemDetails (Timestamp + Title + Duration + Calories)
│   │           ├── ListDivider
│   │           └── WorkoutItem 3
│   │               ├── Thumbnail (App Custom Test icon)
│   │               └── ItemDetails (Timestamp + Title + Duration + Calories)
│   │
│   └── BottomAdBanner (TikTok Banner)
```

---

## 4. Layout Specification

### 4.1. CalendarSectionCard (Khối lịch trên cùng)
| Thuộc tính | Giá trị ước tính |
|---|---|
| Width | 100% viewport |
| Height | Tự co giãn theo lưới ngày (~280–300 px) |
| Layout | Vertical flex column |
| Padding | ~16px (trái/phải), ~12px (trên), ~20px (dưới) |
| Background | `#FFFFFF` |
| Border Radius | Bo góc dưới: Bottom-Left ~20–24px, Bottom-Right ~20–24px |

### 4.2. WeeklySummarySection & Card
| Thuộc tính | Giá trị ước tính |
|---|---|
| Width | 100% (Padding lề màn hình ~16px) |
| Layout | Vertical flex column |
| Card Background | `#FFFFFF` |
| Card Border Radius | ~16–20 px |
| Card Inner Padding | ~16 px |
| Item Gap / Dividers | Divider kẻ ngang màu `#ECEEF2`, dày 1px |

---

## 5. Component Specifications

### 5.1. TopNavBar
- **Layout:** Horizontal flex, `align-items: center`, gap ~16px.
- **BackButton:** Icon mũi tên quay trái `←` màu đen đậm `#111827`, kích thước ~24×24 px.
- **ScreenTitle:** Text "History", font size ~20–22 px, Bold (700), màu đen `#111827`.

### 5.2. MonthlyCalendarGrid
- **Lưới ngày:** CSS Grid 7 cột (`grid-template-columns: repeat(7, 1fr)`), căn giữa text (`text-align: center`).
- **Hàng thứ (WeekdayHeaders):** `S  M  T  W  T  F  S` (Màu đen/xám `#22252A`, Bold/Semi-bold, font ~14px).
- **Các ô ngày (DaysGrid):**
  - **Ngày tập / Active Day (`1`):** Hình tròn đặc màu xanh dương `#0057FF`, đường kính ~32–36 px, số `1` màu trắng `#FFFFFF`, Bold 700 căn giữa hoàn hảo.
  - **Ngày thường / Chưa tập (`2` đến `30`):** Text số màu xám trung tính `#8E95A2`, Regular, font ~14px.
  - **Khoảng cách dòng (Row Gap):** ~12–14 px.

### 5.3. SummaryCardHeader
- **Bố cục:** Horizontal flex, `justify-content: space-between`, `align-items: flex-start`, padding-bottom: ~12px.
- **Cột trái:**
  - Date Range: `Aug 30 - Sep 5` (Font ~16px, Bold 700, màu `#111827`).
  - Subtitle: `3 Workouts` (Font ~13px, Regular, màu `#8E95A2`).
- **Cột phải (Căn phải):**
  - Dòng 1: Icon đồng hồ xanh dương + text `00:05` (Font ~13px, Semi-bold).
  - Dòng 2: Icon ngọn lửa đỏ cam + text `0.5 Kcal` (Font ~13px, Semi-bold).

### 5.4. WorkoutItem (Reusable List Item)
- **Geometry:** Height ~68–76 px, Layout: Horizontal flex, `align-items: center`, gap ~12px.
- **Thumbnail:**
  - Kích thước ~52×52 px, `border-radius: 10–12px`.
  - Hiển thị ảnh chụp minh họa bài tập hoặc icon tùy chỉnh.
- **Info Stack (Vertical flex, flex: 1):**
  - Dòng 1 (Timestamp): `Sep 1, 4:54 PM` (Font ~12px, màu xám `#8E95A2`).
  - Dòng 2 (Workout Title): `Day 1 - LOWER BODY` (Font ~15px, Bold 700, màu `#111827`, in hoa/thường theo tên bài).
  - Dòng 3 (Metrics Row): Icon đồng hồ xanh + `00:03` &emsp; Icon lửa + `0.3 Kcal` (Font ~12px, màu xám đậm).

---

## 6. Typography

| Role | Estimated Size | Weight | Color | Sample Text |
|---|---:|---|---|---|
| Screen Title | 20–22 px | Bold (700) | `#111827` | `History` |
| Section Header | 18–20 px | Bold (700) | `#111827` | `Weekly Summary` |
| Date Range Header | 16 px | Bold (700) | `#111827` | `Aug 30 - Sep 5` |
| Workout Item Title | 15–16 px | Bold (700) | `#111827` | `Day 1 - LOWER BODY`, `Abs Beginner` |
| Calendar Weekdays | 14 px | Semi-Bold (600) | `#111827` | `S`, `M`, `T`, `W`, `T`, `F`, `S` |
| Calendar Active Date | 14–15 px | Bold (700) | `#FFFFFF` | `1` |
| Calendar Inactive Dates | 14 px | Regular (400) | `#8E95A2` | `2`, `3`, `4`... `30` |
| Item Subtitle / Time | 12–13 px | Regular (400) | `#8E95A2` | `Sep 1, 4:54 PM`, `3 Workouts` |
| Metric Inline Stats | 12–13 px | Medium (500) | `#374151` | `00:03`, `0.3 Kcal` |

---

## 7. Color Palette

| Token | Estimated Hex | Usage |
|---|---|---|
| `canvas-background` | `#F6F7F9` | Nền tổng thể phía dưới màn hình |
| `surface-card` | `#FFFFFF` | Nền khối Calendar và Weekly Summary Card |
| `primary-accent` | `#0057FF` | Vòng tròn đánh dấu ngày tập `1`, icon đồng hồ nhỏ |
| `accent-calories` | `#FF5722` / `#FF4D4F` | Icon ngọn lửa calo tiêu thụ |
| `text-primary` | `#111827` | Tiêu đề trang, tên bài tập, khoảng ngày |
| `text-secondary` | `#8E95A2` | Ngày chưa tập trên lịch, nhãn thời gian, số buổi tập |
| `border-divider` | `#ECEEF2` | Đường kẻ ngang phân tách giữa các bài tập |

---

## 8. Spacing and Sizing Tokens

- **Góc bo (Border Radii):**
  - `radius-calendar-bottom`: ~24 px (Bo tròn đáy khối lịch trên)
  - `radius-card`: ~16–20 px (Thẻ Weekly Summary)
  - `radius-thumbnail`: ~10–12 px (Ảnh thumbnail bài tập)
  - `radius-active-date`: 9999px (Vòng tròn ngày 1)
- **Khoảng cách (Spacing):**
  - `screen-padding-h`: 16 px
  - `section-gap`: ~16–20 px
  - `item-inner-padding`: ~10–12 px
- **Kích thước Thumbnail / Icon:**
  - Thumbnail ảnh: `52×52 px`
  - Inline metric icons: `14×14 px`
  - Back button icon: `24×24 px`

---

## 9. Text Content

- **Top Bar:** `History`
- **Calendar Grid:**
  - `S`, `M`, `T`, `W`, `T`, `F`, `S`
  - `1`, `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`, `10`, `11`, `12`, `13`, `14`, `15`, `16`, `17`, `18`, `19`, `20`, `21`, `22`, `23`, `24`, `25`, `26`, `27`, `28`, `29`, `30`
- **Weekly Summary Section:**
  - `Weekly Summary`
  - `Aug 30 - Sep 5`
  - `3 Workouts`
  - `00:05`, `0.5 Kcal`
- **Workout Logs:**
  - `Sep 1, 4:54 PM`, `Day 1 - LOWER BODY`, `00:03`, `0.3 Kcal`
  - `Sep 1, 9:46 AM`, `Abs Beginner`, `00:00`, `0.0 Kcal`
  - `Sep 1, 9:45 AM`, `test`, `00:02`, `0.2 Kcal`

---

## 10. Assets and Icons

| Component | Asset / Icon | Size | Description |
|---|---|---:|---|
| TopNavBar | Back Arrow | 24×24 px | Mũi tên hướng trái `←` |
| Calendar | Active Day Badge | 34×34 px | Vòng tròn nền xanh dương `#0057FF` |
| Summary & List | Clock Icon | 14×14 px | Biểu tượng đồng hồ màu xanh dương |
| Summary & List | Fire Icon | 14×14 px | Biểu tượng ngọn lửa màu cam/đỏ |
| List Item 1 | Thumbnail 1 | 52×52 px | Ảnh vận động viên tập luyện với bao cát |
| List Item 2 | Thumbnail 2 | 52×52 px | Ảnh vận động viên gập bụng |
| List Item 3 | Thumbnail 3 | 52×52 px | Icon đồ họa màu xanh dương hình cây bút |

---

## 11. Interaction and State Observations

### Observed:
- **Tháng hiển thị:** Tháng 9 (ngày 1 rơi vào Thứ Ba `T`).
- **Ngày được chọn / Có lịch sử:** Ngày `1` đang được highlight màu xanh, tương ứng danh sách bên dưới hiển thị đúng các bài tập của ngày `Sep 1`.
- **Tổng kết tuần:** Đã thực hiện `3 Workouts` với tổng thời gian `00:05` và tiêu hao `0.5 Kcal`.

### Inferred:
- Nhấn vào nút Back `←`: Quay lại màn hình Report hoặc Home trước đó.
- Chạm vào các ngày khác trên lưới lịch: Cập nhật danh sách bài tập bên dưới theo ngày được chọn.
- Chạm vào từng hàng bài tập (`WorkoutItem`): Mở màn hình chi tiết nhật ký bài tập đó (danh sách động tác, hiệp tập, nhịp tim...).

---

## 12. Responsive Observations

- Màn hình sử dụng `ScrollView` toàn trang.
- Lưới lịch `MonthlyCalendarGrid` luôn chiếm 100% bề ngang và chia đều 7 cột bằng nhau (`1fr` mỗi cột).
- Khối Weekly Summary co giãn chiều ngang theo viewport trừ 2 bên margin (16px mỗi bên).

---

## 13. Reconstruction Priorities

1. **Calendar Layout & Grid Alignment:** Căn chuẩn lưới 7 cột với khoảng cách đều, vị trí vòng tròn ngày `1` xanh dương đồng tâm.
2. **Surface & Separation:** Tạo sự phân tách rõ ràng giữa khối Calendar phía trên (nền trắng bo tròn đáy) và khối Weekly Summary phía dưới nổi trên nền xám.
3. **Reusable Workout Item Component:** Chuẩn hóa component hàng bài tập với thumbnail bo góc, tiêu đề đậm và hàng thông số (đồng hồ + calo).
4. **Color Tokens:** Màu xanh thương hiệu `#0057FF` dùng cho ngày active và icon thời gian.

---

## 14. Uncertainties

- *Chuyển tháng:* Chưa thấy nút điều hướng tháng trước/sau (có thể thực hiện bằng cử chỉ vuốt ngang Swipe trên lịch).
- *Hiển thị nhiều ngày tập:* Chưa rõ khi tập nhiều ngày trong tháng thì các ngày khác hiển thị dấu chấm nhỏ (dot indicator) hay cùng dạng vòng tròn xanh.

---

## 15. Implementation Notes

- **Calendar Grid:** Dùng CSS Grid `display: grid; grid-template-columns: repeat(7, 1fr); justify-items: center; align-items: center;`.
- **Danh sách bài tập:** Dùng `FlatList` hoặc component lặp `WorkoutItem`, chèn `<Divider />` giữa các phần tử trừ phần tử cuối cùng.
- **Tránh Hardcode:** Tách dữ liệu workout thành mảng JSON gồm `{ id, date, title, duration, calories, thumbnail }` để render động.