package com.mattermost.clistBot.service;

import java.io.IOException;

public interface ClistBotService {
    void callCloud() throws IOException;
    void sendDailyChallenges() throws IOException;
}
