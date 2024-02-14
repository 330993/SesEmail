package com.adhocstatement.reportgeneration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


@SpringBootApplication
//@EnableSwagger2
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
public class ReportGenerationApplication{

	public static void main(String[] args) {
		
		SpringApplication.run(ReportGenerationApplication.class, args);
		
	}
		
}
