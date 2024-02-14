package com.adhocstatement.reportgeneration.repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Repository;

import com.adhocstatement.reportgeneration.config.AWSConfig;
import com.adhocstatement.reportgeneration.model.StatementRequest;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;

@PropertySource("classpath:statementEnum.properties")

@Repository
public class StatementRequestRepository {
	private static final Logger logger = LoggerFactory.getLogger(StatementRequestRepository.class);

	@Autowired
	private DynamoDBMapper dbmapper;

	@Value("${STATUSPENDING}")
	private String statusPending;

	@Value("${STATUSFAILED}")
	private String statusFailed;
	@Value("${STATUSPDFEMAILSENT}")
	private String statusPdfEmailSent;

	@Value("${STATEMENTINDEXNAME}")
	private String statementIndexName;

	// Get records as per criteria i.e period range
	public List<StatementRequest> loadRecords(String period, String channel, String status) {
		logger.info("index name" + statementIndexName);
		logger.info("Start- inside load records for period=" + period + " and channel=" + channel);
		Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
		eav.put(":v2", new AttributeValue().withS(status));
		DynamoDBQueryExpression<StatementRequest> queryExpression = null;
		if (channel != null) {
			eav.put(":v1", new AttributeValue().withN(period));
			eav.put(":v5", new AttributeValue().withS(channel));
			queryExpression = new DynamoDBQueryExpression<StatementRequest>().withIndexName(statementIndexName)
					.withScanIndexForward(true).withConsistentRead(false)
					.withKeyConditionExpression("statementRequestStatus = :v2")
					.withFilterExpression("durationInDays < :v1 and channel= :v5").withExpressionAttributeValues(eav)
					.withLimit(50);

		} else if (channel == null && period == null) {
			queryExpression = new DynamoDBQueryExpression<StatementRequest>().withIndexName(statementIndexName)
					.withScanIndexForward(true).withConsistentRead(false)
					.withKeyConditionExpression("statementRequestStatus = :v2").withExpressionAttributeValues(eav)
					.withLimit(50);
		} else {
			eav.put(":v1", new AttributeValue().withN(period));
			queryExpression = new DynamoDBQueryExpression<StatementRequest>().withIndexName(statementIndexName)
					.withScanIndexForward(true).withConsistentRead(false)
					.withKeyConditionExpression("statementRequestStatus = :v2")
					.withFilterExpression("durationInDays >= :v1").withExpressionAttributeValues(eav).withLimit(50);
		}
		queryExpression.setScanIndexForward(true);
		queryExpression.setLimit(50);
		queryExpression.setIndexName(statementIndexName);
		logger.info("Query Expression"+queryExpression);
		List<StatementRequest> statementRequest = dbmapper.query(StatementRequest.class, queryExpression);

		logger.info("End- inside load records for period=" + period + " and channel=" + channel);
		return statementRequest;
	}

	// Update record with status
	public String updateRecord(String id, StatementRequest request, String status) throws ParseException {
		logger.info("Start - update record for account Number :" + request.getAccountNumber() + "Unique id" + id
				+ " with status+++" + status);

		Date currentDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String strDate = formatter.format(currentDate);
		Date newDate = formatter.parse(strDate);
		long epochTime = newDate.getTime();

		StatementRequest statement1 = dbmapper.load(StatementRequest.class, id.toString(), request.getCreatedTime());
		statement1.setStatementRequestStatus(status);
		statement1.setUpdatedTime(newDate);
		// statement1.setUpdatedTimeEpoc(epochTime);
		dbmapper.save(statement1, new DynamoDBSaveExpression().withExpectedEntry("statementRequestId",
				new ExpectedAttributeValue(new AttributeValue().withS(id.toString()))));

		logger.info("End - update record for account Number :" + request.getAccountNumber() + "Unique id" + id
				+ " with status+++" + status);

		return "Updated Successfully";
	}

	// Update CRN Number
	public String updateCRNNumber(StatementRequest statement, String crn) {
		logger.info("Start - update CRN Number for account Number :" + statement.getAccountNumber());

		String id = statement.getStatementRequestId();
		StatementRequest statement1 = dbmapper.load(StatementRequest.class, id, statement.getCreatedTime());
		statement1.setCrnNumber(crn);
		logger.info("update CRN number for ID++++" + statement1.getStatementRequestId() + " and account number=="
				+ statement1.getAccountNumber());

		dbmapper.save(statement1, new DynamoDBSaveExpression().withExpectedEntry("statementRequestId",
				new ExpectedAttributeValue(new AttributeValue().withS(statement1.getStatementRequestId()))));

		logger.info("End - update CRN Number for account Number :" + statement.getAccountNumber());

		return "Updated Successfully";
	}

	// Update Time
	public String updateTime(String id, StatementRequest request, String timeParam) throws ParseException {
		logger.info("Start - update time record for account Number :" + request.getAccountNumber() + "Unique id" + id
				+ " with status+++");

		Date currentDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String strDate = formatter.format(currentDate);
		Date newDate = formatter.parse(strDate);

		StatementRequest statement1 = dbmapper.load(StatementRequest.class, id.toString(), request.getCreatedTime());
		// statement1.setStatementRequestStatus(status);
		if ("start".equals(timeParam)) {
			statement1.setProcess3StartTime(newDate);
		} else {
			statement1.setProcess3EndTime(newDate);
		}
		dbmapper.save(statement1, new DynamoDBSaveExpression().withExpectedEntry("statementRequestId",
				new ExpectedAttributeValue(new AttributeValue().withS(id.toString()))));

		logger.info("End - update time record for account Number :" + request.getAccountNumber() + "Unique id" + id
				+ " with status+++");

		return "Updated Time Successfully";
	}

	// Update Description with status
	public String updateDescription(StatementRequest request, String status, String message) throws ParseException {
		logger.info("Start - update Description for account Number :" + request.getAccountNumber() + " Unique id "
				+ request.getStatementRequestId() + " with status+++ " + status + " Reprocessed Count "
				+ request.getReprocessCount());

		Date currentDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String strDate = formatter.format(currentDate);
		Date newDate = formatter.parse(strDate);
		long epochTime = newDate.getTime();

		StatementRequest statement1 = dbmapper.load(StatementRequest.class, request.getStatementRequestId().toString(),
				request.getCreatedTime());
		if (status.equalsIgnoreCase(statusFailed) || status.equalsIgnoreCase(statusPdfEmailSent))
			statement1.setStatementRequestStatus(status);
		statement1.setUpdatedTime(newDate);
		statement1.setReprocessCount(request.getReprocessCount());
		String desc = request.getFailedDescrption();
		StringBuilder description = new StringBuilder();
		if (desc != null) {
			description.append(desc);
			description.append(", ");
		}
		description.append(message);
		statement1.setFailedDescrption(description.toString());
		dbmapper.save(statement1, new DynamoDBSaveExpression().withExpectedEntry("statementRequestId",
				new ExpectedAttributeValue(new AttributeValue().withS(request.getStatementRequestId().toString()))));

		logger.info("End - update Description for account Number :" + request.getAccountNumber() + "Unique id"
				+ request.getStatementRequestId() + " with status+++" + status);

		return "Updated Successfully";
	}

}
