# UI Reconstruction Specification

## 1. Source

- **Loại giao diện:** Mobile UI Card Component (Dark Theme).
- **Phân loại Viewport:** Mobile Portrait (thành phần thẻ widget nằm trong màn hình dashboard theo dõi sức khỏe/luyện tập).
- **Bối cảnh hiển thị:** Thẻ hiển thị tiến độ giảm cân cá nhân ("Lose Weight Progress Card") trên nền tối.

---

## 2. Visual Summary

Giao diện là một **Card widget theo dõi mục tiêu giảm cân (Weight Loss Goal Tracker Card)** theo phong cách Dark Mode hiện đại. Thẻ sử dụng bề mặt màu xám than tối (`#18191D`) với các góc bo tròn lớn (~20–24px) nổi trên nền đen (`#000000`).

Cấu trúc gồm 4 phần chính xếp theo chiều dọc:
1. **Card Header:** Tiêu đề mục tiêu "Lose Weight" căn trái.
2. **Current Value Indicator:** Cụm hiển thị cân nặng hiện tại (`70.9 kg`) kèm ngày ghi nhận (`Sep 4`), được căn theo vị trí con trỏ (thumb) trên thanh tiến độ.
3. **Progress Bar & Range Bounds:** Thanh tiến trình 2 màu kèm con trỏ tròn trắng, phía dưới là mốc bắt đầu (`72.0 kg - Starting`) và mốc mục tiêu (`65.0 kg - Goal`).
4. **Action Button:** Nút bấm pill màu xanh dương rực rỡ "Record" căn giữa ở đáy thẻ.

---

## 3. Screen Structure

```text
WeightLossCard (Container)
├── CardTitle ("Lose Weight")
│
├── CurrentProgressIndicator (Aligned with Thumb position)
│   ├── CurrentWeightValue ("70.9" + "kg")
│   └── RecordDate ("Sep 4")
│
├── ProgressBarTrack
│   ├── ActiveTrack (Blue gradient / Solid blue)
│   ├── SliderThumb (White circular knob)
│   └── InactiveTrack (Dark grey)
│
├── RangeBoundsRow (Horizontal flex, space-between)
│   ├── StartingGroup (Align Left)
│   │   ├── StartValue ("72.0 kg")
│   │   └── StartLabel ("Starting")
│   └── GoalGroup (Align Right)
│       ├── GoalValue ("65.0 kg")
│       └── GoalLabel ("Goal")
│
└── ActionRow (Center aligned)
    └── RecordButton ("Record")
```

---

## 4. Layout Specification

### WeightLossCard
| Thuộc tính | Giá trị ước tính |
|---|---|
| Width | 100% (trong vùng lề an toàn ~16px mỗi bên) |
| Height | Tự co giãn theo nội dung (~240–260 px) |
| Layout | Vertical flex column |
| Padding | ~20–24 px (tất cả các cạnh) |
| Gap giữa các khối | ~12–16 px |
| Background | `#18191D` (Dark Charcoal) |
| Border Radius | ~20–24 px |
| Border / Shadow | Không viền (None), bề mặt phẳng (Flat surface) |

---

## 5. Component Specifications

### 5.1. CardTitle
- **Mục đích:** Tên của mục tiêu đang theo dõi.
- **Vị trí:** Góc trên bên trái thẻ.
- **Typography:** Size ~18–20 px, Weight: Bold (700), Color: `#FFFFFF`.

### 5.2. CurrentProgressIndicator
- **Mục đích:** Hiển thị cân nặng mới nhất được ghi nhận và ngày cập nhật.
- **Layout:** Vertical stack căn theo trục X tương ứng với vị trí của `SliderThumb` trên thanh tiến độ (ở đây là khoảng ~15–16% tính từ cạnh trái thanh track).
- **Phần tử con:**
  - `WeightValue`: `70.9` (Bold 700, ~24–28 px, `#FFFFFF`) + `kg` (Regular 400, ~14–16 px, `#8E929A`).
  - `DateLabel`: `Sep 4` (Regular 400, ~13–14 px, `#8E929A`).

### 5.3. ProgressBarTrack
- **Mục đích:** Trực quan hóa tiến độ giảm cân từ mốc bắt đầu tới mục tiêu.
- **Kích thước:** Chiều cao track ~6 px, bo tròn hai đầu (`border-radius: 9999px`).
- **Cấu trúc:**
  - `ActiveTrack`: Nằm từ mốc 0% đến vị trí thumb, màu xanh dương rực rỡ `#007AFF` hoặc gradient xanh sáng.
  - `SliderThumb`: Hình tròn màu trắng thuần `#FFFFFF`, đường kính ~14–16 px, đặt nổi đè lên thanh track tại vị trí giá trị hiện tại.
  - `InactiveTrack`: Nằm từ thumb đến mốc 100%, màu xám đậm `#3A3C42`.

### 5.4. RangeBoundsRow
- **Mục đích:** Hiển thị mốc xuất phát và mục tiêu cần đạt.
- **Layout:** Horizontal flex, `justify-content: space-between`, `width: 100%`, margin-top: ~8 px.
- **Khối "Starting" (Trái):**
  - Giá trị: `72.0 kg` (Bold 600, ~16 px, `#FFFFFF`).
  - Nhãn: `Starting` (Regular 400, ~13–14 px, `#8E929A`).
- **Khối "Goal" (Phải):**
  - Giá trị: `65.0 kg` (Bold 600, ~16 px, `#FFFFFF`, text-align: right).
  - Nhãn: `Goal` (Regular 400, ~13–14 px, `#8E929A`, text-align: right).

### 5.5. ActionButton ("Record")
- **Mục đích:** Kích hoạt modal/form nhập số cân nặng mới.
- **Geometry:** Height ~44–48 px, Width: ~160–180 px (căn giữa trong thẻ), `border-radius: 9999px` (Full Pill).
- **Appearance:** `background: #0062FF` (Royal Blue), Text `#FFFFFF`, Font-weight: Bold (700), Font-size: ~15–16 px.

---

## 6. Typography

| Role | Estimated Size | Weight | Color | Sample Text |
|---|---:|---|---|---|
| Card Header | 18–20 px | Bold (700) | `#FFFFFF` | `Lose Weight` |
| Current Metric (Number) | 26–28 px | Bold (800) | `#FFFFFF` | `70.9` |
| Current Metric (Unit) | 14–15 px | Regular (400) | `#8E929A` | `kg` |
| Current Date | 13–14 px | Regular (400) | `#8E929A` | `Sep 4` |
| Bound Metric Value | 16–17 px | Semi-Bold (600) | `#FFFFFF` | `72.0 kg`, `65.0 kg` |
| Bound Metric Label | 13–14 px | Regular (400) | `#8E929A` | `Starting`, `Goal` |
| Button Label | 15–16 px | Bold (700) | `#FFFFFF` | `Record` |

---

## 7. Color Palette

| Token | Estimated Hex | Usage |
|---|---|---|
| `canvas-background` | `#000000` | Nền đen ngoài màn hình chính |
| `surface-card` | `#18191D` | Nền bề mặt của card widget |
| `primary-accent` | `#0062FF` | Nút bấm Record & phần active của thanh tiến độ |
| `track-inactive` | `#3A3C42` | Phần còn lại của thanh tiến độ |
| `thumb-color` | `#FFFFFF` | Con trỏ tròn chỉ điểm hiện tại |
| `text-primary` | `#FFFFFF` | Tiêu đề card, số cân nặng hiện tại & mốc |
| `text-secondary` | `#8E929A` | Ngày tháng (`Sep 4`), nhãn `Starting`, `Goal`, đơn vị `kg` |

---

## 8. Spacing and Sizing Tokens

- **Góc bo (Border Radii):**
  - `radius-card`: ~20–24 px
  - `radius-pill`: 9999px (nút Record, Slider track, Slider thumb)
- **Khoảng cách (Spacing):**
  - `card-padding`: ~20–24 px
  - `element-gap-sm`: ~4–6 px (khoảng cách giữa số và nhãn phụ)
  - `element-gap-md`: ~16–20 px (khoảng cách giữa thanh progress và nút bấm)
- **Kích thước điều khiển:**
  - `progress-height`: ~6 px
  - `thumb-diameter`: ~14–16 px
  - `button-height`: ~46 px
  - `button-width`: ~170 px

---

## 9. Text Content

- **Tiêu đề:** `Lose Weight`
- **Chỉ số hiện tại:** `70.9 kg`, `Sep 4`
- **Mốc xuất phát:** `72.0 kg`, `Starting`
- **Mốc mục tiêu:** `65.0 kg`, `Goal`
- **Nút hành động:** `Record`

---

## 10. Assets and Icons

- Không có icon dạng SVG/Bitmap độc lập (thuần Typography, Vector Shapes và CSS layout).
- Thành phần đồ họa gồm có:
  - Thanh trượt tiến độ (Custom Progress Bar / Slider).
  - Con trỏ tròn màu trắng (Circular Thumb).

---

## 11. Interaction and State Observations

### Observed:
- Tiến độ hiện tại tính theo công thức: 
  $$\text{Progress} = \frac{\text{Start} - \text{Current}}{\text{Start} - \text{Goal}} = \frac{72.0 - 70.9}{72.0 - 65.0} = \frac{1.1}{7.0} \approx 15.7\%$$
- Vị trí khối text "70.9 kg / Sep 4" và con trỏ trắng đang hiển thị chính xác ở vị trí ~15.7% của thanh tiến độ.

### Inferred:
- Nhấn nút `Record`: Mở modal/bottom-sheet cho phép người dùng nhập chỉ số cân nặng của ngày hôm nay.
- Thanh tiến trình có thể là thanh tĩnh (Read-only Progress Indicator) chứ không phải thanh kéo chỉnh tay (Interactive Slider), vì giá trị hiển thị phụ thuộc vào lịch sử log cân nặng.

---

## 12. Responsive Observations

- Thẻ tự động chiếm 100% chiều rộng container khả dụng.
- Vị trí cụm chỉ số "70.9 kg / Sep 4" và Thumb cần được tính toán linh hoạt bằng tỷ lệ phần trăm (`left: ${progressPercentage}%`) để đảm bảo luôn khớp với thanh trượt trên mọi kích thước màn hình.
- Nút `Record` giữ kích thước chiều rộng cố định (~160–180px) và luôn căn giữa (`align-self: center` hoặc `margin: 0 auto`).

---

## 13. Reconstruction Priorities

1. **Geometry & Dark Theme Contrast:** Màu nền card `#18191D` tương phản rõ ràng trên nền đen `#000000` với bo góc chuẩn ~20px.
2. **Progress Indicator Alignment:** Căn chỉnh chính xác vị trí con trỏ tròn và khối thông tin `70.9 kg - Sep 4` đồng trục dọc với tỷ lệ tiến độ thực tế.
3. **Vibrant Primary Accent:** Mã màu xanh dương sáng `#0062FF` cho nút "Record" và dải active progress.
4. **Typography Hierarchy:** Phân cấp rõ rệt giữa số đo lớn màu trắng (`70.9`, `72.0`, `65.0`) và nhãn phụ màu xám (`Sep 4`, `Starting`, `Goal`).

---

## 14. Uncertainties

- *Tính tương tác của Slider:* Chưa xác định được người dùng có thể kéo trực tiếp con trỏ trên thanh để mô phỏng cân nặng hay thanh chỉ mang tính hiển thị tiến độ (Read-only).
- *Gradient trên Active Track:* Phần màu xanh của thanh tiến độ có thể là màu đơn sắc (Solid Blue) hoặc dải Gradient nhẹ từ xanh dương sang xanh Cyan.

---

## 15. Implementation Notes

- **Cách dựng cụm chỉ số cân nặng hiện tại:** 
  - Khối chứa thanh progress nên đặt `position: relative`.
  - Khối `CurrentProgressIndicator` và `SliderThumb` đặt `position: absolute`, sử dụng công thức `left: [tỉ_lệ_%]` kết hợp `transform: translateX(-50%)` để luôn căn chuẩn tâm con trỏ.
- **Nút "Record":** Không sử dụng `width: 100%`, dùng Flexbox với `justify-content: center` từ container cha và gán `min-width: 160px`.


# UI Reconstruction Specification

## 1. Source

- **Loại giao diện:** Mobile UI Components (Dark Theme) — Phần mở rộng phía dưới của màn hình theo dõi cân nặng & sức khỏe.
- **Phân loại Viewport:** Mobile Portrait (tương thích iOS / Android).
- **Mối quan hệ:** Nằm trực tiếp bên dưới thẻ "Lose Weight" trước đó trong cùng một màn hình cuộn dọc (`ScrollView`) trên nền đen (`#000000`).

---

## 2. Visual Summary

Giao diện gồm **2 thẻ (Cards) widget chuyên sâu** phục vụ phân tích dữ liệu thể chất theo phong cách Dark Mode hiện đại:
1. **Weight Detailed Analytics Card:** Thẻ biểu đồ cân nặng chi tiết gồm mốc cân nặng hiện tại / mục tiêu (kèm icon chỉnh sửa), biểu đồ đường xu hướng 7 ngày (Line chart) với tooltip chỉ số, mốc mục tiêu `↓ Goal: 65.0 kg`, và 3 cột tóm tắt thông số nhanh (Thay đổi 7 ngày qua, Trung bình, BMI).
2. **BMI Classification Card:** Thẻ phân loại chỉ số khối cơ thể (BMI) trực quan với thanh dải màu 6 phân vùng chuẩn y tế (Tím/Xanh đậm, Xanh nhạt, Xanh ngọc, Vàng, Cam, Đỏ hồng), con trỏ tooltip chỉ số `20.3`, và nhãn trạng thái sức khỏe ("Healthy weight").

---

## 3. Screen Structure

```text
HealthMetricsDashboard (Extended)
├── [LoseWeightCard - Đã phân tích ở bước trước]
│
├── WeightAnalyticsCard (Thẻ biểu đồ & phân tích cân nặng)
│   ├── WeightHeaderRow (Horizontal flex, space-between)
│   │   ├── CurrentWeightBlock
│   │   │   ├── MetricLabel ("Current")
│   │   │   └── ValueWithEdit ("70.9" + "kg" + PencilIcon)
│   │   └── GoalWeightBlock (Align Right)
│   │       ├── MetricLabel ("Goal")
│   │       └── ValueWithEdit ("65.0 kg" + PencilIcon)
│   │
│   ├── ChartSubHeader ("September")
│   ├── WeightLineChartArea
│   │   ├── YAxisColumn (75.0, 73.8, 72.5, 71.3, 70.0)
│   │   ├── GridDottedLines
│   │   ├── TargetGoalLine ("↓ Goal: 65.0 kg")
│   │   ├── DataLinePath (Blue stroke with data points)
│   │   ├── ActiveDataTooltip (Pill badge "70.9" with pointer at Day 4)
│   │   └── XAxisRow (1, 2, 3, 4, 5, 6, 7)
│   │
│   └── SummaryMetricsRow (3-Column Horizontal flex)
│       ├── Column 1: Last7Days (Label "Last 7 Days" + Value "↓ 1.1")
│       ├── Column 2: Average (Label "Avg." + Value "71.5")
│       └── Column 3: CurrentBMI (Label "BMI" + DotIndicator + Value "20.3")
│
└── BMICard (Thẻ phân tích chỉ số BMI)
    ├── BMIHeaderRow (Horizontal flex, space-between)
    │   ├── CardTitle ("BMI")
    │   └── EditActionLink ("Edit" + PencilIcon)
    │
    ├── BMISpectrumContainer
    │   ├── ValueTooltipBadge (Pill badge "20.3" hovering over active zone)
    │   ├── SegmentedColorBar (6 rounded segments: Purple, Blue, Cyan, Yellow, Orange, Red)
    │   └── ScaleTicksRow (15, 16, 18.5, 25, 30, 35, 40)
    │
    └── StatusLegendRow (Horizontal flex)
        ├── StatusDot (Cyan colored bullet)
        └── StatusLabel ("Healthy weight")
```

---

## 4. Layout Specification

### 4.1. WeightAnalyticsCard
| Thuộc tính | Giá trị ước tính |
|---|---|
| Width | 100% viewport (trong lề padding ~16px) |
| Height | Tự co giãn theo nội dung (~320–340 px) |
| Layout | Vertical flex column |
| Padding | ~20–24 px |
| Gap giữa các khối | ~16–20 px |
| Background | `#18191D` (Dark Charcoal) |
| Border Radius | ~20–24 px |

### 4.2. BMICard
| Thuộc tính | Giá trị ước tính |
|---|---|
| Width | 100% viewport |
| Height | Tự co giãn theo nội dung (~180–200 px) |
| Layout | Vertical flex column |
| Padding | ~20–24 px |
| Gap giữa các khối | ~14–16 px |
| Background | `#18191D` |
| Border Radius | ~20–24 px |

---

## 5. Component Specifications

### 5.1. WeightAnalyticsCard

#### A. Header (Current & Goal Block)
- **Current Block (Trái):**
  - Nhãn "Current" (Font ~13–14px, màu xám `#8E929A`).
  - Giá trị "70.9" (Font ~32px, Bold 800, màu trắng `#FFFFFF`) + đơn vị "kg" (Font ~16px, xám) + icon cây bút chì `✏️` màu xám nhạt (~16×16px).
- **Goal Block (Phải - Căn phải):**
  - Nhãn "Goal" (Font ~13–14px, màu xám `#8E929A`).
  - Giá trị "65.0 kg" (Font ~18–20px, Semi-bold 600, màu trắng `#FFFFFF`) + icon cây bút chì `✏️`.

#### B. Line Chart Area
- **Tháng hiển thị:** Text "September" màu xám `#70747E`, căn giữa phía trên đồ thị.
- **Trục Y (Bên trái):** 5 mốc giá trị (`75.0`, `73.8`, `72.5`, `71.3`, `70.0`), font ~12px, xám nhạt.
- **Lưới tọa độ:** Các đường kẻ ngang nét đứt mờ (`border-top: 1px dashed #2B2D33`).
- **Đường mục tiêu (Goal Guideline):** Dòng chữ `↓ Goal: 65.0 kg` đi kèm đường kẻ mục tiêu.
- **Đường dữ liệu (Data Trendline):**
  - Nét vẽ màu xanh dương `#007AFF`, độ dày stroke ~3px.
  - Điểm dữ liệu (Data points): Vòng tròn viền xanh tâm trắng/rỗng.
- **Tooltip điểm hiện tại:**
  - Badge hình viên thuốc màu xám đậm `#3A3C43`, viền mờ, text trắng `70.9` (Bold, ~13px), có mũi nhọn chỉ xuống điểm ngày 4.
- **Trục X (Đáy biểu đồ):** 7 mốc ngày trong tuần `1  2  3  4  5  6  7`, font ~13px, căn đều theo cột.

#### C. Bottom Summary Metrics Row
- **Bố cục:** Horizontal flex chia làm 3 cột bằng nhau (`flex: 1`).
- **Cột 1 (Last 7 Days):** Nhãn "Last 7 Days" (xám), giá trị "↓ 1.1" (Bold 700, ~22–24px, màu trắng).
- **Cột 2 (Avg.):** Nhãn "Avg." (xám), giá trị "71.5" (Bold 700, ~22–24px, màu trắng).
- **Cột 3 (BMI):** Nhãn "BMI" (xám), giá trị gồm chấm tròn xanh ngọc `●` + "20.3" (Bold 700, ~22–24px, màu trắng).

---

### 5.2. BMICard

#### A. Header Row
- **Tiêu đề trái:** "BMI" (Font ~20px, Bold 700, màu trắng `#FFFFFF`).
- **Thao tác phải:** Chữ "Edit" + Icon bút chì `✏️` (Font ~15px, Regular/Medium, màu xám sáng `#A6ABB6`).

#### B. BMI Spectrum Bar & Indicator
- **Tooltip chỉ số:**
  - Badge dạng pill màu xám đậm `#3A3C43`, hiển thị giá trị `20.3` (Bold ~14px, màu trắng), có mũi nhọn chỉ xuống chính xác vị trí phân vùng BMI trên thanh dải màu.
- **Thanh phân vùng (Segmented Bar):**
  - Gồm 6 thanh pill nhỏ bo tròn nằm ngang, chiều cao ~8px, cách nhau gap ~3px:
    1. **Underweight Severely:** Tím/Xanh tím (`#3F51B5`)
    2. **Underweight Mild:** Xanh dương (`#2196F3`)
    3. **Normal / Healthy (Active):** Xanh ngọc Cyan (`#00E5FF` hoặc `#1DE9B6`)
    4. **Overweight:** Vàng chanh (`#FFEB3B` / `#FDD835`)
    5. **Obese Class I:** Cam (`#FF9800`)
    6. **Obese Class II+:** Hồng đỏ (`#FF2D55`)
- **Trục số đo (Scale Ticks):**
  - Các mốc số: `15`, `16`, `18.5`, `25`, `30`, `35`, `40` đặt ngay dưới các điểm tiếp giáp của từng phân đoạn màu, font ~12px, màu xám `#70747E`.

#### C. Status Legend
- **Bố cục:** Horizontal flex, `align-items: center`, gap ~8px.
- **Biểu tượng:** Chấm tròn màu xanh ngọc Cyan `●` (đường kính ~10px) đồng màu với phân vùng `18.5 - 25`.
- **Nhãn:** "Healthy weight" (Font ~15px, Medium 500, màu trắng `#FFFFFF`).

---

## 6. Typography

| Role | Estimated Size | Weight | Color | Sample Text |
|---|---:|---|---|---|
| Card Main Title | 20–22 px | Bold (700) | `#FFFFFF` | `BMI` |
| Big Primary Metric | 32–34 px | Bold (800) | `#FFFFFF` | `70.9` |
| Card Sub-metrics | 22–24 px | Bold (700) | `#FFFFFF` | `↓ 1.1`, `71.5`, `20.3` |
| Goal Metric Value | 18–20 px | Semi-Bold (600) | `#FFFFFF` | `65.0 kg` |
| Status / Edit Action | 15–16 px | Medium (500) | `#FFFFFF` / `#A6ABB6` | `Healthy weight`, `Edit` |
| Section Labels | 13–14 px | Regular (400) | `#8E929A` | `Current`, `Goal`, `Last 7 Days`, `Avg.`, `BMI` |
| Tooltip Values | 13–14 px | Semi-Bold (600) | `#FFFFFF` | `70.9`, `20.3` |
| Axis Ticks & Month | 11–13 px | Regular (400) | `#70747E` | `September`, `75.0`, `18.5`, `1 2 3...` |

---

## 7. Color Palette

| Token | Estimated Hex | Usage |
|---|---|---|
| `canvas-background` | `#000000` | Nền màn hình chính |
| `surface-card` | `#18191D` | Nền các thẻ phân tích dữ liệu |
| `surface-tooltip` | `#3A3C43` | Nền badge số nổi trên chart & BMI bar |
| `chart-line-blue` | `#007AFF` | Đường nối dữ liệu biểu đồ cân nặng |
| `bmi-cyan-healthy` | `#00E5FF` / `#00D2D3` | Phân vùng BMI chuẩn, chấm trạng thái "Healthy weight" |
| `bmi-purple` | `#3F51B5` | Phân vùng BMI cực gầy (<16) |
| `bmi-blue` | `#2196F3` | Phân vùng BMI gầy (16 - 18.5) |
| `bmi-yellow` | `#FDD835` | Phân vùng BMI thừa cân (25 - 30) |
| `bmi-orange` | `#FF9800` | Phân vùng BMI tiền béo phì (30 - 35) |
| `bmi-red` | `#FF2D55` | Phân vùng BMI béo phì nặng (>35) |
| `text-primary` | `#FFFFFF` | Tiêu đề, số liệu chính, nhãn trạng thái |
| `text-secondary` | `#8E929A` | Nhãn phân loại, đơn vị, icon phụ |
| `grid-divider` | `#2B2D33` | Đường lưới ngang nét đứt trong đồ thị |

---

## 8. Spacing and Sizing Tokens

- **Góc bo (Border Radii):**
  - `radius-card`: ~20–24 px
  - `radius-tooltip`: ~8–10 px (kèm caret/arrow)
  - `radius-bmi-segment`: ~4–6 px
- **Khoảng cách (Spacing):**
  - `card-gap`: ~16 px (Khoảng cách giữa các thẻ với nhau)
  - `card-padding`: ~20–24 px
  - `grid-row-gap`: ~16 px
- **Kích thước thành phần:**
  - `bmi-bar-height`: ~8 px
  - `edit-icon-size`: ~16×16 px
  - `chart-dot-size`: ~8×8 px

---

## 9. Text Content

- **WeightAnalyticsCard:**
  - `Current`, `70.9 kg`
  - `Goal`, `65.0 kg`
  - `September`
  - `75.0`, `73.8`, `72.5`, `71.3`, `70.0`
  - `↓ Goal: 65.0 kg`
  - `1`, `2`, `3`, `4`, `5`, `6`, `7`
  - `Last 7 Days`, `↓ 1.1`
  - `Avg.`, `71.5`
  - `BMI`, `20.3`
- **BMICard:**
  - `BMI`, `Edit`
  - `20.3`
  - `15`, `16`, `18.5`, `25`, `30`, `35`, `40`
  - `Healthy weight`

---

## 10. Assets and Icons

| Component | Asset / Icon | Size | Description |
|---|---|---:|---|
| WeightCard Header | Pencil Icon (Current) | 16×16 px | Icon bút chì xám nhạt biểu thị tính năng sửa |
| WeightCard Header | Pencil Icon (Goal) | 16×16 px | Icon bút chì xám nhạt biểu thị sửa mục tiêu |
| WeightCard Bottom | Arrow Down Icon | 14×14 px | Mũi tên chỉ xuống `↓` thể hiện cân nặng giảm |
| WeightCard Bottom | Cyan Indicator Dot | 8×8 px | Chấm tròn xanh ngọc phân loại BMI |
| BMICard Header | Edit Pencil Icon | 16×16 px | Icon bút chì đi kèm chữ "Edit" |
| BMICard Legend | Status Bullet | 10×10 px | Chấm tròn xanh ngọc tương ứng phân vùng lành mạnh |

---

## 11. Interaction and State Observations

### Observed:
- **Biểu đồ cân nặng:** Dữ liệu đang được ghi nhận đến Ngày 4 (`Sep 4`), điểm ngày 4 có tooltip `70.9` đang được chọn/active.
- **Chỉ số 7 ngày:** Cân nặng đã giảm `1.1 kg` (ký hiệu mũi tên giảm `↓ 1.1`).
- **BMI Spectrum:** Chỉ số hiện tại là `20.3` nằm chính xác trong phân vùng `18.5 - 25` (Màu xanh ngọc / "Healthy weight").

### Inferred:
- Nhấn vào icon bút chì ở "Current" hoặc "Goal": Mở hộp thoại thay đổi cân nặng hiện tại hoặc cập nhật mục tiêu mới.
- Nhấn vào các mốc ngày (1, 2, 3... 7) trên biểu đồ: Di chuyển tooltip để xem cân nặng của từng ngày cụ thể.
- Nhấn nút "Edit" trên thẻ BMI: Cho phép điều chỉnh chiều cao/cân nặng để tính toán lại chỉ số BMI.

---

## 12. Responsive Observations

- Hai thẻ này nằm trong cùng một luồng cuộn dọc (`ScrollView`) với thẻ "Lose Weight" phía trên.
- Chiều ngang biểu đồ và thanh dải màu BMI luôn giãn đều 100% bề rộng khả dụng của thẻ.
- Tọa độ các điểm trên biểu đồ (Line chart) và vị trí tooltip trên thanh BMI được tính toán động dựa theo tỷ lệ % giá trị thực tế trên thang đo.

---

## 13. Reconstruction Priorities

1. **Card Consistency:** Sử dụng đồng nhất màu nền card `#18191D`, góc bo `~20px` và lề đệm giống với thẻ "Lose Weight".
2. **Chart & Target Guideline:** Dựng biểu đồ đường với hệ trục tọa độ nét đứt và hiển thị đúng đường mục tiêu `↓ Goal: 65.0 kg`.
3. **BMI Segmented Bar:** Tái tạo chính xác dải màu 6 đoạn bo góc với thang số mốc y tế chuẩn (`15, 16, 18.5, 25, 30, 35, 40`).
4. **Tooltips with Pointers:** Thiết kế 2 badge tooltip bo tròn (`70.9` và `20.3`) có mũi tam giác chỉ chính xác vào điểm dữ liệu.

---

## 14. Uncertainties

- *Tương tác vuốt biểu đồ:* Chưa rõ biểu đồ có hỗ trợ cử chỉ vuốt ngang (Pan/Swipe) để xem các tuần trước đó hay chỉ hiển thị cố định 7 ngày gần nhất.
- *Thư viện biểu đồ:* Dạng biểu đồ đường tùy biến cao, có thể cần dùng Custom SVG hoặc thư viện vẽ chart nhẹ để đạt độ chính xác giao diện tối đa.

---

## 15. Implementation Notes

- **Thành phần biểu đồ (`WeightLineChart`):**
  - Dựng bằng SVG với `<polyline>` hoặc `<path>` cho nét vẽ xanh `#007AFF`.
  - Các đường gióng ngang dùng nét đứt `stroke-dasharray="3, 3"`.
  - Tooltip `70.9` định vị tuyệt đối (`position: absolute`) dựa trên tọa độ X, Y của data point ngày 4.
- **Thành phần thanh BMI (`BMISpectrumBar`):**
  - Sử dụng Flexbox chia tỷ lệ tương đối giữa các khoảng giá trị:
    - $[15 - 16] \rightarrow$ tỉ lệ $1$
    - $[16 - 18.5] \rightarrow$ tỉ lệ $2.5$
    - $[18.5 - 25] \rightarrow$ tỉ lệ $6.5$ (dài nhất)
    - $[25 - 30] \rightarrow$ tỉ lệ $5$
    - $[30 - 35] \rightarrow$ tỉ lệ $5$
    - $[35 - 40] \rightarrow$ tỉ lệ $5$
  - Tooltip `20.3` đặt theo công thức tính phần trăm vị trí trong tổng thang đo từ 15 đến 40.