package com.adhocstatement.reportgeneration.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
/*This Class is for Email Template creation*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailTemplate {

	private static final Logger logger = LoggerFactory.getLogger(EmailTemplate.class);

	private String template;
	private Map<String, String> replacementParams;
	public EmailTemplate(String customtemplate) { 

		try {
			this.template = loadTemplate(customtemplate);
		} catch (Exception e) {
			this.template = "Empty";
		}

	}
	private String loadTemplate(String customtemplate) throws Exception 
	{
		logger.info("Start -load template :"+customtemplate);
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(customtemplate).getFile());
		Path path = file.toPath();
		if(file.toString().contains("2520"))
		{
			String filepath = file.getAbsolutePath().replaceAll("2520", "20");
			path = Paths.get(filepath);
		}
		//System.out.println("file++"+file);
		String content = "Empty";
		try {
			content = new String(Files.readAllBytes(path));
		} catch (IOException e) {
			throw new Exception("Could not read template  = " + customtemplate);
		}
		logger.info("End -load template :"+customtemplate);
		return content;

	}
	public String getTemplate(Map<String, String> replacements) {
		logger.info("Start -get template :");
		String cTemplate = this.template;
		//Replace the String 
		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			cTemplate = cTemplate.replace("{{" + entry.getKey() + "}}", entry.getValue());
		}
		logger.info("End -load template :");
		return cTemplate;
	}
}