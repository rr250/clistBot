package com.mattermost.clistBot.service;

import com.google.gson.Gson;
import com.mattermost.clistBot.domain.Attachment;
import com.mattermost.clistBot.domain.Challenge;
import com.mattermost.clistBot.domain.SendPost;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ClistBotServiceImpl implements ClistBotService {

    @Value("${clist_token}")
    private  String clistToken;

    @Value("${bot_token}")
    private String botToken;

    @Value("${channel_id}")
    private String channelId;

    @Override
    @Scheduled(fixedDelay = 1000*86400,initialDelay = 0)
    /*  After  60 SEC this method will execute
     *  60 SEC will delay will by end time of method execution
     *  with initial Delay of 60 sec
     * */
    public void sendDailyChallenges() throws IOException {
        LocalDate now= LocalDate.now(ZoneId.of("Asia/Kolkata"));
        log.info("{}",now);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("https://clist.by/api/v1/contest/?limit=100&offset=0&order_by=-start&start__gt="+now+"T00:00:00&start__lt="+now+"T23:59:59");
        httpGet.setHeader("Authorization",clistToken);
        HttpResponse httpResponse = httpClient.execute(httpGet);
        log.info("{}",httpResponse.getEntity());
        HttpEntity httpEntity = httpResponse.getEntity();
        String retSrc = EntityUtils.toString(httpEntity);
        JSONObject result = new JSONObject(retSrc);
        log.info("{}",result.getJSONArray("objects"));
        List<Challenge> challengeList = new ArrayList<>();
        for(int i=0;i<result.getJSONArray("objects").length();i++){
            Challenge challenge = new Challenge();
            challenge.setDuration(result.getJSONArray("objects").getJSONObject(i).getLong("duration"));
            challenge.setStart(result.getJSONArray("objects").getJSONObject(i).getString("start"));
            challenge.setEnd(result.getJSONArray("objects").getJSONObject(i).getString("end"));
            challenge.setHref(result.getJSONArray("objects").getJSONObject(i).getString("href"));
            challenge.setId(result.getJSONArray("objects").getJSONObject(i).getLong("id"));
            challenge.setEvent(result.getJSONArray("objects").getJSONObject(i).getString("event"));
            log.info("{}",challenge.getEnd());
            challengeList.add(challenge);
        }

        SendPost sendPost = new SendPost();
        for (Challenge challenge : challengeList) {
            Attachment attachment = new Attachment();
            attachment.setTitle(challenge.getEvent());
            attachment.setTitle_link(challenge.getHref());
            attachment.setFooter("Start : " + challenge.getStart().format(DateTimeFormatter.ofPattern("dd-MM HH:mm")) + " " + "End : " + challenge.getEnd().format(DateTimeFormatter.ofPattern("dd-MM HH:mm")));
            sendPost.getProps().getAttachments().add(attachment);
        }
        sendPost.setChannel_id(channelId);
        log.info("{}",sendPost.getProps().getAttachments().get(0).getFooter());
        HttpPost httpPost = new HttpPost("http://localhost:8065/api/v4/posts");
        httpPost.addHeader("Authorization",botToken);
        Gson gson = new Gson();
        StringEntity postingString = new StringEntity(gson.toJson(sendPost));
        httpPost.setEntity(postingString);
        log.info("{}",gson.toJson(sendPost));
        httpPost.setHeader("Content-type", "application/json");
        httpClient.execute(httpPost);
    }
}
