package com.openclassrooms.starterjwt.dto;

import java.util.Date;

public record SessionSummary(Long id, String name, Date date, String description) {
}
