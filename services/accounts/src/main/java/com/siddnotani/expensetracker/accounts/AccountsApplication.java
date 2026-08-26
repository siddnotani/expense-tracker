package com.siddnotani.expensetracker.accounts;

import com.siddnotani.expensetracker.shared.security.InternalJwtSecurityConfiguration;
import com.siddnotani.expensetracker.shared.security.SharedWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({InternalJwtSecurityConfiguration.class, SharedWebConfiguration.class})
public class AccountsApplication {
    public static void main(String[] args) { SpringApplication.run(AccountsApplication.class, args); }
}
