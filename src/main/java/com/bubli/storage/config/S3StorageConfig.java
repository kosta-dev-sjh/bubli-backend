package com.bubli.storage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageConfig {

	@Bean
	public S3Presigner s3Presigner(
			@Value("${aws.region:ap-northeast-2}") String region,
			@Value("${aws.access-key:}") String accessKey,
			@Value("${aws.secret-key:}") String secretKey
	) {
		S3Presigner.Builder builder = S3Presigner.builder()
				.region(Region.of(region));
		if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
			builder.credentialsProvider(StaticCredentialsProvider.create(
					AwsBasicCredentials.create(accessKey, secretKey)
			));
		} else {
			builder.credentialsProvider(DefaultCredentialsProvider.create());
		}
		return builder.build();
	}
}
