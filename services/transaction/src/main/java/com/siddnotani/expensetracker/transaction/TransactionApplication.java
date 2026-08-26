package com.siddnotani.expensetracker.transaction;

import com.siddnotani.expensetracker.shared.security.InternalJwtSecurityConfiguration;
import com.siddnotani.expensetracker.shared.security.CurrentUser;
import com.siddnotani.expensetracker.shared.security.SharedWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({CurrentUser.class, InternalJwtSecurityConfiguration.class, SharedWebConfiguration.class})
public class TransactionApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionApplication.class, args);
    }
}
