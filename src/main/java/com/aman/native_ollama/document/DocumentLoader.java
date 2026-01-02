package com.aman.native_ollama.document;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class DocumentLoader {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public DocumentLoader(EmbeddingModel embeddingModel,
                          EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @PostConstruct
    public void loadDocuments() {
        if (!isStoreEmpty()) {
            System.out.println("Chroma already has vectors. Skipping ingestion to prevent duplicates.");
            return;
        }

        System.out.println(" Database empty. Starting ingestion...");

        try {
            Path path = Paths.get("src/main/resources/Documents/cv.txt");
            Document document = FileSystemDocumentLoader.loadDocument(path, new TextDocumentParser());
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(500, 50)) // Smart splitting
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            ingestor.ingest(document);

            System.out.println("cv.txt loaded successfully into ChromaDB");

        } catch (Exception e) {
            System.err.println(" Failed to load document: " + e.getMessage());
        }
    }

    private boolean isStoreEmpty() {
        try {
            // We use a dummy embedding to "probe" the store
            Embedding probe = embeddingModel.embed("probe").content();

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(probe)
                    .maxResults(1)
                    .build();

            return embeddingStore.search(request).matches().isEmpty();
        } catch (Exception e) {
            return true;
        }
    }
}