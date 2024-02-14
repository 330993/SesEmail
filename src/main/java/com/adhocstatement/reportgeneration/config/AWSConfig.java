package com.adhocstatement.reportgeneration.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;


/*This Class is for dynamodb setup and configuration*/
@Configuration 
@ConfigurationProperties(prefix="amazon.aws")
@EnableDynamoDBRepositories
(basePackages = "com.adhocstatement.reportgeneration.repository")
public class AWSConfig {

	private static final Logger logger = LoggerFactory.getLogger(AWSConfig.class);

	@Value("${amazon.dynamodb.endpoint}")
	private String amazonDynamoDBEndpoint;

	@Value("${amazon.aws.accesskey}")
	private String amazonAWSAccessKey;

	@Value("${amazon.aws.secretkey}")
	private String amazonAWSSecretKey;

	@Value("${amazon.aws.region}")
	private String amazonAWSRegion;

	@Value("${amazon.aws.region}")
	private Region amazonAWSRegion1;

	@Bean
	@Primary 
	public AmazonDynamoDB amazonDynamoDB(AWSCredentialsProvider awsCredentialsProvider) {
		logger.info("Start- Dynamodb credentials bean created");
		AmazonDynamoDB amazonDynamoDB= AmazonDynamoDBClientBuilder.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(amazonDynamoDBEndpoint, amazonAWSRegion))
				.withCredentials(awsCredentialsProvider).build();
		logger.info("End- Dynamodb credentials bean created");
		return amazonDynamoDB;
	}


	@Bean	  
	@Primary 
	public AWSCredentialsProvider awsSesCredentialsProvider() 
	{ 
		return new AWSStaticCredentialsProvider(new BasicAWSCredentials(amazonAWSAccessKey,amazonAWSSecretKey));
	}

	// Configure AWS credentials using StaticCredentialsProvider
	public AwsCredentialsProvider awsCredentialsProvider1() {
		AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(amazonAWSAccessKey, amazonAWSSecretKey));
		return credentialsProvider;
	}

	@Bean
	public S3Client s3Client() {
		// Set up AWS S3 client
		S3Client s3Client = S3Client.builder()
				.region(amazonAWSRegion1)
				.credentialsProvider(awsCredentialsProvider1())// Specify your desired AWS region
				.build();
		return s3Client;
	}

	@Bean
	public AmazonSimpleEmailService sesClient() {
		 AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
                 .withCredentials(awsSesCredentialsProvider())
                 .withRegion(amazonAWSRegion) // Specify your AWS region here
                 .build();
		 return client;
	}
}
