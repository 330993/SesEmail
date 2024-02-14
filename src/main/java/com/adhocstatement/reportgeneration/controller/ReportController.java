package com.adhocstatement.reportgeneration.controller;
import java.text.ParseException;
import java.text.SimpleDateFormat;  
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adhocstatement.reportgeneration.model.StatementRequest;
import com.adhocstatement.reportgeneration.service.BankReportService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

import com.lowagie.text.DocumentException;




@RestController
public class ReportController
{	
	private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

	@Autowired
    private BankReportService bankReportService;
	


	
	@GetMapping(("/test/getBucket"))
	public void createBucketinS3Storage() throws IOException, ParseException, DocumentException
	{
		StatementRequest request = new StatementRequest();
		//bankReportService.generatePDFReport(request);
		
		
	}
	
	
	@GetMapping(("/test/createFolderInBucket"))
	public void createFolderInBucket()
	{
		 // Get today's date in the desired format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = dateFormat.format(new Date());

        // Create a folder (empty object with a '/' at the end of the key)
        String folderKey = currentDate + "/";
        ByteArrayInputStream emptyContent = new ByteArrayInputStream(new byte[0]);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);

       // amazonS3Client.putObject("adhoc-statements", folderKey, emptyContent, metadata);

        System.out.println("Folder created: " + folderKey);
	}
	
	
	
	
	
}
