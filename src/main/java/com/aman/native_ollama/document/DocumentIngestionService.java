package com.aman.native_ollama.document;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class DocumentIngestionService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    private final DocumentParser parser = new ApacheTikaDocumentParser();

    public DocumentIngestionService(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel
    ) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public void smartIngest(MultipartFile file) throws IOException {
        byte[] fileBytes = file.getBytes();
        String fileHash = DigestUtils.md5DigestAsHex(fileBytes);

        if (isAlreadyIngested(fileHash)) {
            System.out.println("File [" + file.getOriginalFilename() + "] already exists. Skipping.");
            return;
        }
        Document document;
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            document = parser.parse(is);
            if (document.text() == null || document.text().isBlank()) {
                throw new RuntimeException("Could not extract text from the file.");
            }

            document.metadata()
                    .put("file_hash", fileHash)
                    .put("file_name", file.getOriginalFilename())
                    .put("content_type", file.getContentType());
        }

        EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(document);

        System.out.println("Successfully ingested: " + file.getOriginalFilename());
    }

    private boolean isAlreadyIngested(String hash) {
        Embedding hashEmbedding = embeddingModel.embed(hash).content();
        Filter filter = MetadataFilterBuilder.metadataKey("file_hash").isEqualTo(hash);
        return !embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(hashEmbedding)
                .filter(filter)
                .maxResults(1)
                .build()).matches().isEmpty();
    }
}