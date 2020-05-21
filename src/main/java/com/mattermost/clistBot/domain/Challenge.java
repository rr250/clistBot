package com.mattermost.clistBot.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Slf4j
public class Challenge {

    private long id;
    private long duration;
    private LocalDateTime start;
    private LocalDateTime end;
    private String event;
    private String href;

    public void setStart(String start){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .withZone(ZoneId.of("UTC"));
        this.start=LocalDateTime.parse(start, formatter);
    }

    public void setEnd(String end){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .withZone(ZoneId.of("UTC"));
        this.end=LocalDateTime.parse(end, formatter);
    }
}
