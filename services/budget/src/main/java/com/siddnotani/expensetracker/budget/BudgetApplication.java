package com.siddnotani.expensetracker.budget;

import com.siddnotani.expensetracker.shared.security.InternalJwtSecurityConfiguration;
import com.siddnotani.expensetracker.shared.security.CurrentUser;
import com.siddnotani.expensetracker.shared.security.SharedWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({InternalJwtSecurityConfiguration.class, SharedWebConfiguration.class, CurrentUser.class})
public class BudgetApplication {
    public static void main(String[] args) {
        SpringApplication.run(BudgetApplication.class, args);
    }
}
