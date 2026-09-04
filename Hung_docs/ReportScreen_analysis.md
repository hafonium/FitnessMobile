
# Chỉnh sửa phần weight card trong ReportScreen.kt

Hiện tại đang sử dụng placeholder:
```kotlin
 Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(TileShape)
                                .background(CloudGray),
                            contentAlignment = Alignment.Center
                        ) { Text("weight trend", style = MaterialTheme.typography.bodySmall, color = SlateGray) }
```


## Các phần cần sửa đổi:

### 1. Thay đổi "weight trend" placeholder:

- Tìm trong room database các bảng lưu weight của user qua thời gian
- sort lại các weight record theo thời gian và kết hợp sử dụng LineChart (Time Series Line Chart / Trend Chart) - biểu đồ đường thể hiện biến động theo chuỗi thời gian (Thư viện phổ biến và hiện đại nhất hiện nay cho Compose là Vico hoặc YCharts)

### 2. Card interaction and new weight screen:

Allow the whole box of weight to be clickable:
- onClick = navigate to "weight screen", which hasn't been implemented yet.

- remove the `log` button

new Weight Screen components:
The whole screen comprises of 3 item/cards:
Read the analysis in `Hung_docs/WeighScreen_analysis.md`


