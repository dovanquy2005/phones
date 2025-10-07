package com.fonestore.staff_api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
public class TestController {
    private final DataSource ds;
    public TestController(DataSource ds) { this.ds = ds; }

    @GetMapping("/test-db")
    public String testDb() throws Exception {
        try (Connection c = ds.getConnection()) {
            return "✅ DB connected: " + c.getCatalog();
        }
    }

    @GetMapping("/health")
    public String health() { return "UP"; }
}
