package com.dinsho.solo;

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.dinsho.solo.Service.LoanFunder;
import com.dinsho.solo.Service.LoanProcessor;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

@SpringBootApplication
public class SoloApplication implements CommandLineRunner {
	private static Logger LOG = LoggerFactory
			.getLogger(SoloApplication.class);

	@Autowired
	private LoanProcessor loanProcessor;

	public static void main(String[] args) {
		SpringApplication.run(SoloApplication.class, args);
	}

	@Override
	public void run(String... args)
			throws InterruptedException, JsonSyntaxException, JsonIOException, FileNotFoundException {
		loanProcessor.ExecuteLoan();
	}

}