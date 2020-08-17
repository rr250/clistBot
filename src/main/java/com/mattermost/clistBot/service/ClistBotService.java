package com.mattermost.clistBot.service;

import java.io.IOException;
import java.sql.SQLException;

public interface ClistBotService {
//    void callCloud() throws IOException;
    void sendDailyChallenges() throws IOException;
    void sendEndChallengeNotification() throws IOException;
//    void sendWeekendClasses() throws IOException, SQLException;
}
