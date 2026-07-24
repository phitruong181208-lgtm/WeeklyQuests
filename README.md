# WeeklyQuests
Plugin nhiệm vụ theo tuần tự nhiên cho Paper 26.1.2 (Minecraft 1.26.1)

## Tính năng
- 8 tuần nhiệm vụ theo chủ đề, độ khó tăng dần
- Tích lũy tiến độ theo tuần tự nhiên trong năm
- GUI xem tiến độ trực quan với thanh tiến độ
- Tự động chuyển tuần mới và thông báo toàn máy chủ
- Thông báo đặc biệt khi hoàn thành toàn bộ 8 tuần

## Cài đặt
1. Cài đặt Java 21+
2. Biên dịch: `mvn clean package`
3. Copy `target/WeeklyQuests.jar` vào thư mục `plugins` của máy chủ Paper
4. Khởi động lại máy chủ

## Lệnh
- `/weeklyquest` hoặc `/wq` - Mở GUI xem nhiệm vụ tuần
- `/weeklyquest reload` - Tải lại cấu hình (chỉ quản trị)
