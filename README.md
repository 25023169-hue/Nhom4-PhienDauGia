Mô tả bài toán và phạm vi
Hệ thống đấu giá trực tuyến hỗ trợ người dùng đăng ký, đăng nhập, quản lý ví, tham gia đấu giá, theo dõi lịch sử mua hàng và nhận thông báo. Seller có thể đăng ký vai trò bán hàng, tạo sản phẩm đấu giá, quản lý phiên đấu giá và xem thống kê doanh thu. Admin có thể quản lý người dùng, sản phẩm và các phiên đấu giá trong hệ thống.

Phạm vi hệ thống gồm:

Server Spring Boot cung cấp REST API, WebSocket realtime và xử lý nghiệp vụ đấu giá.

Client JavaFX cung cấp giao diện desktop cho bidder, seller và admin.

Cơ sở dữ liệu MySQL lưu người dùng, sản phẩm, phiên đấu giá, bid, giao dịch và thông báo.

Công nghệ sử dụng và môi trường chạy
Java 21 trở lên.

Spring Boot 3.4.0.

Spring Data JPA / Hibernate.

MySQL.

JavaFX 23.

Maven Wrapper.

Lombok.

JUnit 5, Mockito.

GitHub Actions cho CI/CD.

Yêu cầu cài đặt:

JDK 21 khuyến nghị cho chạy ổn định. Project đã được cập nhật để compile/test trên JDK 25.

MySQL đang chạy local hoặc server riêng.

Git và Maven Wrapper có sẵn trong repo.

IntelliJ IDEA hoặc terminal PowerShell.

Cấu trúc thư mục và module chính
.
Nhom4-PhienDauGia/
├── .github/workflows/ci.yml          # Pipeline CI/CD GitHub Actions
├── .mvn/                             # Cấu hình Maven Wrapper
├── src/main/java/
│   │
│   ├── common/                  # Gói dùng chung (Shared) cho cả Client và Server
│   │   ├── dto/                 # Chứa các Data Transfer Object (AuctionItemDTO, BidDTO...)
│   │   ├── enums/               # Chứa các hằng số liệt kê (Role, ItemType, AuctionState...)
│   │   ├── request/             # Định nghĩa format dữ liệu gửi lên API (LoginRequest, BidRequest...)
│   │   ├── response/            # Định nghĩa format dữ liệu trả về từ API (ApiResponse, AuthResponse...)
│   │   └── Constants.java       # Các hằng số cấu hình hệ thống (BASE_URL, SOCKET_URL...)
│   │
│   ├── client/                  # Gói giao diện Frontend (JavaFX)
│   │   ├── controller/          # Xử lý logic giao diện (LoginController, BidderDashboardController...)
│   │   ├── model/               # Model nội bộ dành riêng cho Client (TransactionModel)
│   │   ├── pattern/             # Các Design Pattern áp dụng ở Client
│   │   │   ├── AuctionManager   # (Singleton) Quản lý phiên đăng nhập, thông tin user
│   │   │   ├── SceneManager     # (Singleton) Chuyển đổi qua lại giữa các màn hình (Scene)
│   │   │   └── WebSocketClientManager # Quản lý kết nối Socket realtime
│   │   ├── ClientApp.java       # Class khởi chạy ứng dụng JavaFX (Nạp giao diện login.fxml)
│   │   └── ClientLauncher.java  # Lớp bọc (Wrapper) để gọi ClientApp tránh lỗi module
│   │
│   └── server/                  # Gói Backend (Spring Boot + REST API)
│       ├── config/              # Cấu hình hệ thống Server
│       │   ├── AntiSnipingAspect# (AOP) Tự động kiểm tra và gia hạn giờ khi có thợ săn giá
│       │   ├── WebSocketConfig  # Thiết lập cổng Socket cho realtime
│       │   └── DataInitializer  # Khởi tạo dữ liệu mẫu (admin, user, item) khi database trống
│       ├── controller/          # Các REST API Endpoints (AuthController, BidController...)
│       ├── exception/           # Bắt và xử lý lỗi tùy chỉnh (InvalidBidException, GlobalExceptionHandler...)
│       ├── model/               # Các Entity map trực tiếp với Database (User, Bid, Auction, Item...)
│       ├── pattern/             # Các Design Pattern áp dụng ở Server
│       │   └── ItemFactory      # (Factory) Xử lý tạo các loại sản phẩm khác nhau (Art, Vehicle...)
│       ├── repository/          # Giao tiếp với Database (Kế thừa JpaRepository)
│       ├── service/             # Xử lý nghiệp vụ lõi (BidService, AuthService, NotificationService...)
│       │   └── AuctionSchedulerService # (Cron Job) Vòng lặp ngầm tự động mở/đóng phiên
│       └── ServerApp.java       # Class khởi động máy chủ Spring Boot (chạy ở cổng 8080)
│
├── src/main/resources/          # Thư mục chứa tài nguyên tĩnh
│   ├── application.properties   # File cấu hình cực kỳ quan trọng (Kết nối Database, Port...)
│   └── client/fxml/             # Nơi chứa toàn bộ file giao diện .fxml kéo thả bằng Scene Builder
│
├── pom.xml                            # Cấu hình thư viện Maven (Lombok, Spring, JavaFX, Jackson...)
├── mvnw                              # Maven Wrapper Linux/macOS
└── mvnw.cmd                          # Maven Wrapper Windows 


Module server chính:

server.ServerApp: entry point của Spring Boot server.

server.controller: REST API controllers.

server.service: xử lý nghiệp vụ đấu giá, bid, notification, transaction.

server.repository: Spring Data JPA repositories.

server.model: entity database.

server.config: cấu hình WebSocket, scheduler và dữ liệu mẫu.

Module client chính:

client.ClientLauncher: entry point để chạy JavaFX client.

client.ClientApp: khởi tạo giao diện desktop.

client.controller: controller cho các màn hình JavaFX.

client.pattern: quản lý scene, HTTP client, WebSocket client và auction state.

Vị trí file .jar
Sau khi build, file jar nằm tại:
target/Nhom4-PhienDauGia-0.0.1-SNAPSHOT.jar
Lệnh build jar:

.\mvnw.cmd clean package

Trên GitHub Actions, jar được upload trong artifact:
auction-system-jar
Cấu hình database
Tạo file .env.properties ở thư mục gốc project. File này đã được ignore trong Git để tránh commit thông tin nhạy cảm.

Ví dụ:

Properties
DB_URL=jdbc:mysql://localhost:3306/auction_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
Nếu MySQL root không có password, có thể để:

Properties
DB_PASSWORD=
Hướng dẫn chạy Server/Client
Chạy theo đúng thứ tự sau.

Khởi động MySQL.

Tạo file .env.properties và điền đúng thông tin database.

Build và test project:
.\mvnw.cmd clean test
Chạy server:

.\mvnw.cmd spring-boot:run '-Dspring-boot.run.main-class=server.ServerApp'
Server mặc định chạy tại:
http://localhost:8080
Mở terminal PowerShell mới và chạy client:

.\mvnw.cmd javafx:run '-Djavafx.mainClass=client.ClientLauncher'
Client kết nối đến server qua:
REST API: http://localhost:8080/api
WebSocket: ws://localhost:8080/ws
Chức năng đã hoàn thành
Đăng ký và đăng nhập người dùng.

Phân quyền cơ bản cho bidder, seller và admin.

Cập nhật thông tin cá nhân, địa chỉ, ngân hàng và ví.

Seller đăng ký bán hàng.

Seller thêm sản phẩm đấu giá.

Seller quản lý danh sách sản phẩm/phiên đấu giá.

Seller xem chi tiết sản phẩm và thống kê doanh thu.

Bidder xem danh sách sản phẩm đấu giá.

Bidder đặt giá trong phiên đấu giá.

Hiển thị cập nhật giá realtime qua WebSocket.

Hiển thị biểu đồ lịch sử giá/bid.

Quản lý lịch sử mua hàng và inventory của bidder.

Hệ thống thông báo realtime.

Admin dashboard quản lý người dùng và phiên đấu giá.

Scheduler tự động xử lý trạng thái phiên đấu giá.

Xử lý settlement, giao dịch và notification sau khi kết thúc đấu giá.

Unit test cho các service, controller, pattern và logic client.

CI/CD GitHub Actions tự động chạy test, build jar và upload artifact.

CI/CD với GitHub Actions
Workflow nằm tại:
.github/workflows/ci.yml
Pipeline tự động chạy khi push hoặc pull request lên các branch main, master, demo:

Checkout source code.

Cài JDK 21.

Cache Maven dependencies.

Chạy test bằng .\mvnw.cmd test.

Build jar bằng .\mvnw.cmd -DskipTests package.

Upload file jar trong target/*.jar.