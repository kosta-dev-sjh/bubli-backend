package com.bubli.resource.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.resource.dto.ResourceAnalysisTarget;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceAnalysisStateWriter {

    private final ResourceRepository resourceRepository;

    @Transactional
    public ResourceAnalysisTarget start(UUID resourceId) {
        Resource resource = resourceRepository.lockById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_404_001));
        resource.startAnalysis();
        return new ResourceAnalysisTarget(resource.getId(), resource.getRoomId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID resourceId) {
        resourceRepository.lockById(resourceId).ifPresent(Resource::markAnalysisFailed);
    }
}
