package com.q.library_management_system;

import org.springframework.boot.SpringApplication;

public class TestLibraryManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.from(LibraryManagementSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
