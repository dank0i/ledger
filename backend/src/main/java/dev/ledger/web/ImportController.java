package dev.ledger.web;

import dev.ledger.service.CsvImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final CsvImportService importService;

    public ImportController(CsvImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/csv")
    public CsvImportService.ImportResult importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        return importService.importCsv(file.getInputStream());
    }
}
