# TODO - Thanh toán online (MoMo)

## Bước 1: Phân tích hiện trạng
- [x] Xác định luồng đặt vé hiện tại: `POST /datve` tạo thẳng `Ve`.
- [x] Xác định entity đã có cho thanh toán: `DonHang`, `ThanhToan`, `PhuongThucThanhToan`.

## Bước 2: Thiết kế luồng MoMo
- [x] Chốt thanh toán 1 ghế/lần.
- [x] Chọn placeholder cấu hình MoMo (partnerCode/accessKey/secretKey/endpoints).
- [x] Tạo `DonHang` trạng thái PENDING trước khi redirect MoMo.
- [ ] Nhận callback IPN, nếu SUCCESS thì mới tạo `Ve`.


## Bước 3: Backend triển khai
- [x] Thêm `PaymentController` với 2 endpoint:
  - `POST /payment/momo/create`
  - `POST /payment/momo/ipn`
- [x] Thêm `PaymentService` tích hợp MoMo (HTTP + signature) và lưu `DonHang` (tạm).

- [ ] Tái sử dụng/tách logic tính giá vé + phát hành vé từ `DatVeController`.

## Bước 4: Frontend triển khai
- [ ] Sửa `index.html` đổi nút sang “Thanh toán online”.
- [ ] Sửa `user.js` gọi `/payment/momo/create`, lấy `payUrl` rồi redirect.

## Bước 5: Cấu hình & chạy thử
- [ ] Thêm placeholder cấu hình MoMo vào `application.properties`.
- [ ] `mvn clean package` + chạy `mvn spring-boot:run`.
- [ ] Test flow end-to-end.

