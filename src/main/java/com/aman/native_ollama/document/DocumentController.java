package com.aman.native_ollama.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/scholarshell")
public class DocumentController {

    private final DocumentIngestionService documentService;

    public DocumentController(DocumentIngestionService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "Upload and process a document",
            description = "Accepts PDF, XLSX, or PPTX files. Avoids duplicates using content hashing.")
    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(
            @Parameter(description = "The file to be processed (PDF, Excel, PowerPoint)",
                    content = @Content(schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file) {
        try {
            documentService.smartIngest(file);
            return "✅ File " + file.getOriginalFilename() + " is ready for chat!";
        } catch (Exception e) {
            return "Failed: " + e.getMessage();
        }
    }
}