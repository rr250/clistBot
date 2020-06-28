package com.mattermost.clistBot.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Slf4j
public class ZoomMeeting {
    private String id;
    private String startLink;
    private String joinLink;
}

