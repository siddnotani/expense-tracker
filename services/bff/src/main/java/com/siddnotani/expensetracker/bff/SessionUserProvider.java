package com.siddnotani.expensetracker.bff;

import java.util.UUID;

public interface SessionUserProvider {
    UUID currentUserId();
}
