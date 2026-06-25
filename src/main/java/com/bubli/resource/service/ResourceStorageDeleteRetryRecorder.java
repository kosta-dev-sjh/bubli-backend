package com.bubli.resource.service;

import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceStorageDeleteRetryRecord;
import com.bubli.resource.repository.ResourceStorageDeleteRetryRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ResourceStorageDeleteRetryRecorder {

	private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

	private final ResourceStorageDeleteRetryRecordRepository resourceStorageDeleteRetryRecordRepository;

	public void recordFailedDelete(ResourceFile file, RuntimeException cause) {
		resourceStorageDeleteRetryRecordRepository.save(ResourceStorageDeleteRetryRecord.create(
				file.getResourceId(),
				file.getId(),
				file.getStorageKey(),
				errorMessage(cause)
		));
	}

	private String errorMessage(RuntimeException cause) {
		String message = cause.getMessage();
		if (!StringUtils.hasText(message)) {
			message = cause.getClass().getSimpleName();
		}
		if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
			return message;
		}
		return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
	}
}
