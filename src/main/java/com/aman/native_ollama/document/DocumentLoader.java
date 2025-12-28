package com.aman.native_ollama.document;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

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
    public void loadDocuments() throws Exception {

        ClassPathResource resource =
                new ClassPathResource("Documents/cv.txt");

        String content = new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        // Manual chunking (important!)
        int chunkSize = 500;
        int overlap = 50;

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            String chunk = content.substring(start, end);
            TextSegment segment = TextSegment.from(chunk);

            embeddingStore.add(
                    embeddingModel.embed(segment).content(),
                    segment
            );

            start += (chunkSize - overlap);
        }

        System.out.println("✅ cv.txt loaded into embedding store");
    }
}
