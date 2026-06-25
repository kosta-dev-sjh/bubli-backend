package com.bubli.agent.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("ai")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "bubli.rag.smoke",
        name = "enabled",
        havingValue = "true"
)
public class RagSmokeRunner implements ApplicationRunner {

    private final RagSmokeService ragSmokeService;

    @Override
    public void run(ApplicationArguments args) {
        RagSmokeResult result = ragSmokeService.run();
        log.info(
                "RAG smoke test completed: chat={}, embeddingDimensions={}, indexed={}, matched={}",
                result.chatModelResponded(),
                result.embeddingDimensions(),
                result.indexedDocumentCount(),
                result.matchedDocumentCount()
        );
    }
}
