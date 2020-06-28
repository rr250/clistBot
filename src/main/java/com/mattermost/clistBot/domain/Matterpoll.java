package com.mattermost.clistBot.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Slf4j
public class Matterpoll {
    private String id;
    private LocalDateTime createdAt;
    private String creator;
    private String question;
    private List<AnswerOption> answerOptions= new ArrayList<>();
}
