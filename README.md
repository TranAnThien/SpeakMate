# SpeakMate

SpeakMate là ứng dụng Android giúp người học luyện giao tiếp tiếng Anh bằng các cuộc hội thoại thoại thời gian thực với AI trong những tình huống thực tế.

## Chức năng theo SRS

- Luyện hội thoại tự do theo tình huống; hỗ trợ truyền âm thanh liên tục, nhận diện lượt nói, phản hồi âm thanh và ngắt lời AI.
- Ước lượng trình độ nói theo A1–B2, sau đó hướng dẫn học qua lộ trình gồm mục tiêu, bài luyện và bài đánh giá.
- Đánh giá theo năm tiêu chí: Range, Accuracy, Fluency, Interaction và Coherence. Kết quả còn xét các hành động bắt buộc của từng bài.
- Lưu các phiên luyện tập hoàn tất và transcript trên Cloud Firestore để xem lại trong History.

## Công nghệ

- Kotlin, Jetpack Compose; Android 8.0 (API 26) trở lên.
- Firebase Authentication với Google Sign-In và Cloud Firestore.
- Dịch vụ AI thoại thời gian thực, truyền âm thanh và transcript; nhà cung cấp sẽ được chọn khi triển khai.

Các tích hợp Firebase và AI trong SRS là phạm vi mục tiêu; dự án hiện mới có khung Android ban đầu.

## Cấu trúc chính

- `ui/`: màn hình và thành phần giao diện theo khu vực chức năng.
- `domain/`: mô hình, hợp đồng repository và business rules.
- `data/`: xử lý audio, DTO, dữ liệu local/remote và triển khai repository.
- `di/`: cấu hình dependency injection; `utils/`: tiện ích dùng chung.

## Build

Mở dự án bằng Android Studio hoặc chạy trên Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```
