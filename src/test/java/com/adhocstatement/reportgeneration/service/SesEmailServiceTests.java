package com.adhocstatement.reportgeneration.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import com.adhocstatement.reportgeneration.model.StatementRequest;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

@PropertySource("classpath:statementEnum.properties")
@ExtendWith({ MockitoExtension.class })
@PrepareForTest(BankReportService.class)
public class SesEmailServiceTests {
	@InjectMocks
	BankReportService service;
	@Mock
	private S3Client amazonS3Client;
	@Mock
	private AmazonSimpleEmailService sesClient;

	@Value("${S3BUCKET}")
	private String S3BUCKET;
	@Value("${BASELOCATION}")
	private String BASELOCATION;
	@Value("${JSONTOPDFFOLDER}")
	private String JSONTOPDFFOLDER;

	@Test
	void testSesSendEmail() {

		StatementRequest statementRequest = new StatementRequest();
		statementRequest.setChannel("Mobile Banking");
		statementRequest.setEmailAddress("test@gmail.com");
		statementRequest.setStatementType("Email");
		statementRequest.setPeriodFrom("2022-08-15");
		statementRequest.setPeriodTo("2022-08-28");
		statementRequest.setAccountNumber("6512876071");
		ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder().bucket(S3BUCKET)
				.prefix(BASELOCATION + "" + JSONTOPDFFOLDER).build();
		ListObjectsV2Response listObjectsResponse = null;
		when(amazonS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listObjectsResponse);
		software.amazon.awssdk.services.s3.model.S3Object S3Objects = listObjectsResponse.contents().get(0);
		String objectKey = S3Objects.key();
		GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(S3BUCKET).key(objectKey).build();
		ResponseInputStream<GetObjectResponse> s3Object1 = null;
		when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Object1);
		service.sendEmail(statementRequest);
		verify(amazonS3Client, times(1)).listObjectsV2(listObjectsRequest);
		assertTrue(true);
	}
}
