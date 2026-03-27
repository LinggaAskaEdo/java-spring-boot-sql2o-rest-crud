package com.otis.model.entity;

import java.util.Date;
import java.util.UUID;

public record TutorialDetails(UUID id, Date createdOn, String createdBy, Tutorial tutorial) {
}
