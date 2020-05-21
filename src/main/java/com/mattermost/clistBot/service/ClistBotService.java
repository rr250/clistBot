package com.mattermost.clistBot.service;

import java.io.IOException;

public interface ClistBotService {
    void sendDailyChallenges() throws IOException;
}
