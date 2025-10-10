// AssetsController.java
package com.fonestore.staff_api.controller;

import org.springframework.core.io.*;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

@RestController
@RequestMapping("/api/assets")
public class AssetsController {

    // Thư mục ảnh nằm trong static (classpath) hoặc bên ngoài
    private static final String CLASSPATH_DIR = "static/staff-frontend/assets/products";
    // fallback theo đuôi nếu probeContentType() trả null
    private static final Set<String> EXT_FALLBACK = Set.of(
            "png","jpg","jpeg","webp","gif","bmp","svg","avif"
    );

    @GetMapping("/products")
    public List<String> listProductImages() throws IOException {
        // Ưu tiên classpath (chạy từ IDE). Nếu bạn đóng gói jar, cân nhắc dùng thư mục ngoài ứng dụng.
        Resource resource = new ClassPathResource(CLASSPATH_DIR);
        File folder = resource.getFile(); // chạy tốt khi chạy exploded từ IDE

        try (Stream<Path> s = Files.walk(folder.toPath(), 1)) {
            return s.filter(Files::isRegularFile)
                    .filter(this::isImage)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .map(p -> "staff-frontend/assets/products/" + p.getFileName().toString()) // trả path tương đối NGUYÊN GỐC
                    .toList();
        }
    }

    private boolean isImage(Path p) {
        try {
            String ct = Files.probeContentType(p);
            if (ct != null && ct.toLowerCase().startsWith("image/")) return true;
        } catch (IOException ignored) {}
        String name = p.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            String ext = name.substring(dot + 1);
            return EXT_FALLBACK.contains(ext);
        }
        return false;
    }
}
