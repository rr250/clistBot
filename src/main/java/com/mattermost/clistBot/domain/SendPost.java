package com.mattermost.clistBot.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Slf4j
public class SendPost {
    private String channel_id;
    private String message="Challenges starting today";
    private Attachments props = new Attachments();
}
