package com.adhocstatement.reportgeneration.service;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.adhocstatement.reportgeneration.config.EmailTemplate;
import com.adhocstatement.reportgeneration.model.StatementRequest;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

import jakarta.activation.DataHandler;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

@PropertySource("classpath:statementEnum.properties")
@Service
public class BankReportService {

	private static final Logger logger = LoggerFactory.getLogger(BankReportService.class);
	ClassLoader classLoader = getClass().getClassLoader();

	@Autowired
	private TemplateEngine templateEngine;
	
	@Value("${spring.physicalreport.dir}")
	private String physicalReportPath;

	@Autowired
	private S3Client amazonS3Client;
	@Autowired
	private AmazonSimpleEmailService sesClient;
	@Value("${TYPEEMAIL}")
	private String typeEmail;
	@Value("${TYPEPHYSICAL}")
	private String typePhysical;
	@Value("${PDFADMINPASSWORD}")
	public String adminPassword;
	@Value("${S3BUCKET}")
	private String S3BUCKET;
	@Value("${BASELOCATION}")
	private String BASELOCATION;
	@Value("${JSONTOPDFFOLDER}")
	private String JSONTOPDFFOLDER;
	@Value("${STATUSPDFEMAILSENT}")
	private String statusEmailSent;

	// send mail to customer
	public void sendEmail(StatementRequest request) {
		logger.info("Start - inside sendEmail() method account number++");
		String requestId = request.getStatementRequestId();
		ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder().bucket(S3BUCKET)
				.prefix(BASELOCATION + "" + JSONTOPDFFOLDER).build();
		// List objects inside the folder
		ListObjectsV2Response listObjectsResponse = amazonS3Client.listObjectsV2(listObjectsRequest);
		for (software.amazon.awssdk.services.s3.model.S3Object S3Objects : listObjectsResponse.contents()) {
			// This will print the names of files and folders in the bucket
			String objectKey = S3Objects.key();
			String objectrequestid = objectKey.split("/")[3];
			String finalValue = objectrequestid.split("[.]")[0];

			logger.info("Object/Folder Name: " + S3Objects.key() + " objectrequestid " + finalValue);
			if (requestId.equals(finalValue)) {
				String crnNumber = request.getCrnNumber();
				String strPattern = "\\d(?=\\d{4})";
				String crnMask = crnNumber.replaceAll("[^0-9]", "").replaceAll(strPattern, "#");
				String fileName = request.getStatementRequestId() + "_" + crnMask + ".pdf";
				String SENDER_EMAIL = "sender-email@example.com";
				SENDER_EMAIL = "ravirajincedoinc@gmail.com";
				String RECIPIENT_EMAIL = request.getEmailAddress();
				RECIPIENT_EMAIL = "ravirajece21@gmail.com";
				String SUBJECT = "Bank Account Statement from " + request.getPeriodFrom() + " to "
						+ request.getPeriodTo();
				GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(S3BUCKET).key(objectKey).build();
				ResponseInputStream<GetObjectResponse> s3Object1 = amazonS3Client.getObject(getObjectRequest);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				try {
					// InputStream inputStream = s3Object.getObjectContent();
					byte[] pdfBytes = s3Object1.readAllBytes();
					Session session = Session.getDefaultInstance(new Properties());
					// Create a MIME message
					MimeMessage message = new MimeMessage(session);
					message.setSubject(SUBJECT);
					message.setFrom(new InternetAddress(SENDER_EMAIL));
					message.addRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(RECIPIENT_EMAIL));

					// Create the message part
					MimeBodyPart messageBodyPart = new MimeBodyPart();
					Context context = new Context();
					context.setVariable("crnNumber", crnNumber);
					String messageBodyContents = templateEngine.process("SendEmail", context);
					ByteArrayDataSource msgsource = new ByteArrayDataSource(messageBodyContents, "text/html; charset=utf-8");
					messageBodyPart.setDataHandler(new DataHandler(msgsource));
					// Create the attachment part
					MimeBodyPart attachmentBodyPart = new MimeBodyPart();
					ByteArrayDataSource source = new ByteArrayDataSource(pdfBytes, "application/pdf");
					attachmentBodyPart.setDataHandler(new DataHandler(source));
					attachmentBodyPart.setFileName(fileName);

					// Create Multipart
					Multipart multipart = new MimeMultipart();
					multipart.addBodyPart(messageBodyPart);
					multipart.addBodyPart(attachmentBodyPart);

					// Set the content of the message
					message.setContent(multipart);
					// Create SES client and send the email
					message.writeTo(outputStream);
				} catch (Exception e) {
					logger.info("BankReportService->sendEmail(): " + e.getMessage());
				}
				RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

				SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage);
				sesClient.sendRawEmail(rawEmailRequest);

				// sesClient.sendEmail(request1);
			}
		}
	}

}
