# Nhom4-PhienDauGia

## Mo ta bai toan va pham vi

He thong dau gia truc tuyen ho tro nguoi dung dang ky, dang nhap, quan ly vi, tham gia dau gia, theo doi lich su mua hang va nhan thong bao. Seller co the dang ky vai tro ban hang, tao san pham dau gia, quan ly phien dau gia va xem thong ke doanh thu. Admin co the quan ly nguoi dung, san pham va cac phien dau gia trong he thong.

Pham vi he thong gom:

- Server Spring Boot cung cap REST API, WebSocket realtime va xu ly nghiep vu dau gia.
- Client JavaFX cung cap giao dien desktop cho bidder, seller va admin.
- Co so du lieu MySQL luu nguoi dung, san pham, phien dau gia, bid, giao dich va thong bao.

## Cong nghe su dung va moi truong chay

- Java 21 tro len.
- Spring Boot 3.4.0.
- Spring Data JPA / Hibernate.
- MySQL.
- JavaFX 23.
- Maven Wrapper.
- Lombok.
- JUnit 5, Mockito.
- GitHub Actions cho CI/CD.

Yeu cau cai dat:

- JDK 21 khuyen nghi cho chay on dinh. Project da duoc cap nhat de compile/test tren JDK 25.
- MySQL dang chay local hoac server rieng.
- Git va Maven Wrapper co san trong repo.
- IntelliJ IDEA hoac terminal PowerShell.

## Cau truc thu muc va module chinh

```text
.
├── .github/workflows/ci.yml          # Pipeline CI/CD GitHub Actions
├── .mvn/                             # Cau hinh Maven Wrapper
├── src/main/java/client/             # Ung dung JavaFX client
├── src/main/java/common/             # DTO, request, response, enum, constants dung chung
├── src/main/java/server/             # Spring Boot server
├── src/main/resources/client/        # File FXML giao dien JavaFX
├── src/main/resources/application.properties
├── src/test/java/                    # Unit test server/client
├── pom.xml                           # Cau hinh Maven dependencies/plugins
├── mvnw                              # Maven Wrapper Linux/macOS
└── mvnw.cmd                          # Maven Wrapper Windows
```

Module server chinh:

- `server.ServerApp`: entry point cua Spring Boot server.
- `server.controller`: REST API controllers.
- `server.service`: xu ly nghiep vu dau gia, bid, notification, transaction.
- `server.repository`: Spring Data JPA repositories.
- `server.model`: entity database.
- `server.config`: cau hinh WebSocket, scheduler va du lieu mau.

Module client chinh:

- `client.ClientLauncher`: entry point de chay JavaFX client.
- `client.ClientApp`: khoi tao giao dien desktop.
- `client.controller`: controller cho cac man hinh JavaFX.
- `client.pattern`: quan ly scene, HTTP client, WebSocket client va auction state.

## Vi tri file .jar

Sau khi build, file jar nam tai:

```text
target/Nhom4-PhienDauGia-0.0.1-SNAPSHOT.jar
```

Lenh build jar:

```powershell
.\mvnw.cmd clean package
```

Tren GitHub Actions, jar duoc upload trong artifact:

```text
auction-system-jar
```

## Cau hinh database

Tao file `.env.properties` o thu muc goc project. File nay da duoc ignore trong Git de tranh commit thong tin nhay cam.

Vi du:

```properties
DB_URL=jdbc:mysql://localhost:3306/auction_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
```

Neu MySQL root khong co password, co the de:

```properties
DB_PASSWORD=
```

## Huong dan chay Server/Client

Chay theo dung thu tu sau.

1. Khoi dong MySQL.

2. Tao file `.env.properties` va dien dung thong tin database.

3. Build va test project:

```powershell
.\mvnw.cmd clean test
```

4. Chay server:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.main-class=server.ServerApp'
```

Server mac dinh chay tai:

```text
http://localhost:8080
```

5. Mo terminal PowerShell moi va chay client:

```powershell
.\mvnw.cmd javafx:run '-Djavafx.mainClass=client.ClientLauncher'
```

Client ket noi den server qua:

```text
REST API: http://localhost:8080/api
WebSocket: ws://localhost:8080/ws
```

## Chuc nang da hoan thanh

- Dang ky va dang nhap nguoi dung.
- Phan quyen co ban cho bidder, seller va admin.
- Cap nhat thong tin ca nhan, dia chi, ngan hang va vi.
- Seller dang ky ban hang.
- Seller them san pham dau gia.
- Seller quan ly danh sach san pham/phien dau gia.
- Seller xem chi tiet san pham va thong ke doanh thu.
- Bidder xem danh sach san pham dau gia.
- Bidder dat gia trong phien dau gia.
- Hien thi cap nhat gia realtime qua WebSocket.
- Hien thi bieu do lich su gia/bid.
- Quan ly lich su mua hang va inventory cua bidder.
- He thong thong bao realtime.
- Admin dashboard quan ly nguoi dung va phien dau gia.
- Scheduler tu dong xu ly trang thai phien dau gia.
- Xu ly settlement, giao dich va notification sau khi ket thuc dau gia.
- Unit test cho cac service, controller, pattern va logic client.
- CI/CD GitHub Actions tu dong chay test, build jar va upload artifact.

## CI/CD voi GitHub Actions

Workflow nam tai:

```text
.github/workflows/ci.yml
```

Pipeline tu dong chay khi push hoac pull request len cac branch `main`, `master`, `demo`:

- Checkout source code.
- Cai JDK 21.
- Cache Maven dependencies.
- Chay test bang `.\mvnw.cmd test`.
- Build jar bang `.\mvnw.cmd -DskipTests package`.
- Upload file jar trong `target/*.jar`.

## Link bao cao va video demo

- Bao cao PDF: `CHUA_CAP_NHAT_LINK_BAO_CAO_PDF`
- Video demo: `CHUA_CAP_NHAT_LINK_VIDEO_DEMO`
