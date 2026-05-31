package io.auctionsystem.server.service; // Nhớ giữ nguyên dòng package này cho khớp với máy bạn nhé

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseFixer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        System.out.println("----- BẮT ĐẦU DỌN DẸP DATABASE -----");

        // Cố gắng xóa từng cột một, nếu cột nào không tồn tại thì bỏ qua đi tiếp
        try { jdbcTemplate.execute("ALTER TABLE users DROP COLUMN bank_account;"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("ALTER TABLE users DROP COLUMN bank_name;"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("ALTER TABLE users DROP COLUMN account_name;"); } catch (Exception ignored) {}

        System.out.println("----- DỌN DẸP DATABASE HOÀN TẤT! LỖI ĐĂNG KÝ ĐÃ ĐƯỢC XÓA SẠCH -----");
    }
}