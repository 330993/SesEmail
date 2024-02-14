package com.adhocstatement.reportgeneration.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.adhocstatement.reportgeneration.model.StatementRequest;
import com.adhocstatement.reportgeneration.repository.StatementRequestRepository;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
@PropertySource("classpath:statementEnum.properties")

@Service
public class StatementRequestService {


	@Autowired
	private StatementRequestRepository statementRequestRepository;

	public List<StatementRequest> getStatementRecords(String period,  String channel, String status) {
		return statementRequestRepository.loadRecords(period,channel,status);
	}

	public String updateRecord(String id,StatementRequest request,String status) throws ParseException {
		return statementRequestRepository.updateRecord(id,request,status);
	}

	public String updateTime(String id,StatementRequest request,String timeParam) throws ParseException {
		return statementRequestRepository.updateTime(id,request,timeParam);
	}

	public String updateCrnNumber(StatementRequest statement,String crn) {
		return statementRequestRepository.updateCRNNumber(statement,crn);
	}

	public String updateDescription(StatementRequest request,String status, String message) throws ParseException {
		return statementRequestRepository.updateDescription(request,status, message);
	}

}
