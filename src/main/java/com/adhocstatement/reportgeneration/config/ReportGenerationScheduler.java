package com.adhocstatement.reportgeneration.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.adhocstatement.reportgeneration.model.StatementRequest;
import com.adhocstatement.reportgeneration.service.BankReportService;
import com.adhocstatement.reportgeneration.service.StatementRequestService;
import com.amazonaws.SdkBaseException;
import com.lowagie.text.DocumentException;

import jakarta.mail.MessagingException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 
 This Class is for Shedular task
 */
@Configuration
@EnableScheduling
@PropertySource("classpath:statementEnum.properties")
public class ReportGenerationScheduler {
	private static final Logger logger = LoggerFactory.getLogger(ReportGenerationScheduler.class);

	@Autowired
	private StatementRequestService statementRequestService;

	@Autowired
	private BankReportService bankReportService;
	// set this to false to disable this job; set it it true by
	@Value("${SCHEDULEDJOBENABLED:false}")
	private boolean scheduledJobEnabled;
	@Value("${STATEMENTPERIOD}")
	private String statementPeriod;
	@Value("${CHANNELNETBANKING}")
	private String channelNetbanking;
	@Value("${CHANNELMOBILEBANKING}")
	private String channelMobilebanking;

	@Value("${STATUSFAILED}")
	private String statusFailed;
	@Value("${STATUSPENDING}")
	private String statusPending;
	@Value("${STATUSREQUIREDREPROCESS3}")
	private String statusRequiredReprocess3;

	@Value("${S3BUCKET}")
	private String S3BUCKET;

	@Value("${STATUSJSONCREATED}")
	private String statusJsonCreated;
	@Value("${STATUSPDFGENERATED}")
	private String statusPdfGenerated;
	@Value("${STATUSPDFEMAILSENT}")
	private String statusPdfEmailSent;
	@Value("${REQUESTSTART}")
	private String requestStart;
	@Value("${REQUESTEND}")
	private String requestEnd;
	@Value("${FAILEDLIMIT}")
	private String failedLimit;

	// This job for channel netbanking and period less than 180 Days for status
	// pending
	@Async
	@Scheduled(fixedRate = 60000) // every 1 minute Job 1
	public void pullStatementRequestJob1() throws DocumentException, IOException, MessagingException, SQLException,
			ClassNotFoundException, IllegalArgumentException, IllegalAccessException, ParseException {
		logger.info("Start - job 1 pullStatementRequestJob1----");
		if (!scheduledJobEnabled) {
			return;
		}
		try {
			getStatementRecordsWithUpdate(statementPeriod, channelNetbanking, statusPdfGenerated);
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("exception while get records job 1 :----" + e.toString());
		}
		logger.info("End - job 1 pullStatementRequestJob1----");
	}

	// This job for channel mobilebanking and period less than 180 Days for status
	// pending
	/*------------------------------------------------------*/
	@Async
	@Scheduled(fixedRate = 60000) // every 1 minute Job 2
	public void pullStatementRequestJob2() throws Exception {
		logger.info("Start - job 2 pullStatementRequestJob2----");
		if (!scheduledJobEnabled) {
			return;
		}
		try {
			logger.info("job 2 inititaed----");
			getStatementRecordsWithUpdate(statementPeriod, channelMobilebanking, statusPdfGenerated);
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("exception while get records job 2 :----" + e.toString());
		}
		logger.info("End - job 2 pullStatementRequestJob2----");
	}

	// This job for period more than 180 Days for status pending
	/*------------------------------------------------------*/
	@Async
	@Scheduled(fixedRate = 60000) // every 1 minute Job 3
	public void pullStatementRequestJob3() throws Exception {
		logger.info("Start - job 3 pullStatementRequestJob3----");
		if (!scheduledJobEnabled) {
			return;
		}
		try {
			logger.info("job 3 inititaed----");
			getStatementRecordsWithUpdate(statementPeriod, null, statusPdfGenerated);
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("exception while get records job 3 :----" + e.toString());
		}
		logger.info("End - job 3 pullStatementRequestJob3----");
	}

	// This job for failed records to reprocess
	/*------------------------------------------------------*/
	@Async
	@Scheduled(fixedRate = 60000) // every 1 minute Job 3
	public void pullStatementRequestJobForreprocessRecordJob4() throws Exception {
		logger.info("Start - job 4 pullStatementRequestJob4----");
		if (!scheduledJobEnabled) {
			return;
		}
		try {
			logger.info("job 4 inititaed----");
			getStatementRecordsWithUpdate(null, null, statusRequiredReprocess3);
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("exception while get records job 4 :----" + e.toString());
		}
		logger.info("End - job 4 pullStatementRequestJob4----");
	}

	// This Method for Job 1,2,3,4 for accessing records and send email to customer.
	public void getStatementRecordsWithUpdate(String periodRange, String channel, String status)
			throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, DocumentException,
			IOException, MessagingException, SQLException, ParseException {
		// getDetails for web request and statement duration less than 180 days
		List<StatementRequest> records = statementRequestService.getStatementRecords(periodRange, channel, status);
		logger.info("records size for ___" + records.size());
		int counter = 0;
		if (records.size() > 0) {
			// List<StatementRequest> failedList = new ArrayList<StatementRequest>();
			List<StatementRequest> successList = new ArrayList<StatementRequest>();
			for (StatementRequest request : records) {
				if (counter <= 50) {
					try {
						// update to inprogress
						statementRequestService.updateRecord(request.getStatementRequestId(), request,
								statusRequiredReprocess3);
						successList.add(request);
						counter++;
					} catch (Exception ex) {
						// failedList.add(request);
						logger.error(
								"P3 - ReportGenerationScheduler : getStatementRecordsWithUpdate()-exception while updating record"
										+ ex.toString());
						String message = "P3 - ReportGenerationScheduler : getStatementRecordsWithUpdate()-exception while updating record"
								+ ex.toString();
						updateDescription(request, status, message);
					}
				} else {
					break;
				}
			}

			// Looping for generate report
			for (StatementRequest request : successList) {
				// pullDataForEachRequestFromCustomDB
				try {
					statementRequestService.updateTime(request.getStatementRequestId(), request, requestStart);
					logger.info("sending email for Request Id "+request.getStatementRequestId()+" and CRN Number "+ request.getCrnNumber());
					this.sendEmail(request);
					statementRequestService.updateTime(request.getStatementRequestId(), request, requestEnd);
					updateDescription(request, statusPdfEmailSent, "");
					logger.info("Email Id sent Successfully and status updated successfully as email sent for Request Id "+request.getStatementRequestId());
				} catch (Exception ex) {
					logger.error(
							"P3 - ReportGenerationScheduler : getStatementRecordsWithUpdate()-exception while pullDataForEachRequestFromCustomDB"
									+ ex.toString());
					String message = "P3 - ReportGenerationScheduler : getStatementRecordsWithUpdate()-pullDataForEachRequestFromCustomDB"
							+ ex.toString();
					updateDescription(request, status, message);
				}

			}

		}

	}

	// This method for sending mail to customer
	/*------------------------------------------------------*/
	public void sendEmail(StatementRequest request) throws ParseException, IOException, DocumentException,Exception {
		bankReportService.sendEmail(request);
	}

	public void updateDescription(StatementRequest request, String status, String message) throws ParseException {
		if(status.equalsIgnoreCase(statusPdfEmailSent)) {
			status=statusPdfEmailSent;
		}else {
		Long reprocessCount = request.getReprocessCount() != null ? Long.valueOf(request.getReprocessCount()) : 0;
		reprocessCount++;
		request.setReprocessCount(reprocessCount.toString());
		status = reprocessCount >= Long.valueOf(failedLimit) ? statusFailed : status;
		}
		statementRequestService.updateDescription(request, status, message);
	}

}
