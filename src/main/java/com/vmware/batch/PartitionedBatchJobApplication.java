package com.vmware.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;

@SpringBootApplication
@EnableTask
public class PartitionedBatchJobApplication {

	public static void main(String[] args) {
		SpringApplication.run(PartitionedBatchJobApplication.class, args);
	}

}


