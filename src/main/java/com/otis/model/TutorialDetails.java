package com.otis.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
public class TutorialDetails {
    private Long id;
    private Date createdOn;
    private String createdBy;
    private Tutorial tutorial;
}
