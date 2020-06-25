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

    @Value("${mattermost_uri}")
    private String mattermostUri;

    @Override
    @Scheduled(fixedDelay = 1000*60,initialDelay = 1000*60)
    public  void callCloud() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("https://clistbot.el.r.appspot.com/");
        httpClient.execute(httpGet);
    }


    @Override
    @Scheduled(cron="0 0 0 * * *",zone = "Asia/Kolkata")
//    @Scheduled(fixedDelay = 1000*86400,initialDelay = 1000*60)
    public void sendDailyChallenges() throws IOException {
        JSONObject challengesJSON = getChallengesJSON();
        List<Challenge> challengeList = getChallenges(challengesJSON);
        SendPost sendPost = getSendPost(challengeList);
        sendPost(sendPost);
    }

    private JSONObject getChallengesJSON() throws IOException {
        LocalDate now= LocalDate.now(ZoneId.of("Asia/Kolkata"));
        log.info("{}",now);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("https://clist.by/api/v1/contest/?limit=20&offset=0&order_by=-start&start__gt="+now+"T00:00:00&start__lt="+now+"T23:59:59");
        httpGet.setHeader("Authorization",clistToken);
        HttpResponse httpResponse = httpClient.execute(httpGet);
        HttpEntity httpEntity = httpResponse.getEntity();
        String responseString = EntityUtils.toString(httpEntity);
        JSONObject responseJson = new JSONObject(responseString);
        log.info("{}",responseJson.getJSONArray("objects"));
        return responseJson;
    }

    private List<Challenge> getChallenges(JSONObject challengesJSON){
        List<Challenge> challengeList = new ArrayList<>();
        for(int i=0;i<challengesJSON.getJSONArray("objects").length();i++){
            Challenge challenge = new Challenge();
            challenge.setDuration(challengesJSON.getJSONArray("objects").getJSONObject(i).getLong("duration"));
            challenge.setStart(challengesJSON.getJSONArray("objects").getJSONObject(i).getString("start"));
            challenge.setEnd(challengesJSON.getJSONArray("objects").getJSONObject(i).getString("end"));
            challenge.setHref(challengesJSON.getJSONArray("objects").getJSONObject(i).getString("href"));
            challenge.setId(challengesJSON.getJSONArray("objects").getJSONObject(i).getLong("id"));
            challenge.setEvent(challengesJSON.getJSONArray("objects").getJSONObject(i).getString("event"));
            challenge.setResource(challengesJSON.getJSONArray("objects").getJSONObject(i).getJSONObject("resource").getString("name"));
            challengeList.add(challenge);
            log.info("{}",challenge.getResource());
        }
        return challengeList;
    }

    private SendPost getSendPost(List<Challenge> challengeList){
        SendPost sendPost = new SendPost();
        for (Challenge challenge : challengeList) {
            Attachment attachment = new Attachment();
            attachment.setTitle(challenge.getEvent());
            attachment.setTitle_link(challenge.getHref());
            attachment.setAuthor_name(challenge.getResource());
            attachment.setAuthor_link(challenge.getResource());
            attachment.setText("Start : " + challenge.getStart().format(DateTimeFormatter.ofPattern("MMMM dd   HH:mm' hrs'")) + " \n" + "End : " + challenge.getEnd().format(DateTimeFormatter.ofPattern("MMMM dd   HH:mm' hrs'")));
            sendPost.getProps().getAttachments().add(attachment);
            log.info("{}",attachment.getTitle());
        }
        sendPost.setChannel_id(channelId);
        if(challengeList.size() == 0)
            sendPost.setMessage("No challenges starting today");
        return sendPost;
    }

    private void sendPost(SendPost sendPost) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(mattermostUri+"/api/v4/posts");
        httpPost.addHeader("Authorization",botToken);
        Gson gson = new Gson();
        StringEntity postingString = new StringEntity(gson.toJson(sendPost),"UTF8");
        log.info("{}",gson.toJson(sendPost));
        httpPost.setEntity(postingString);
        httpPost.setHeader("Content-type", "application/json");
        HttpResponse httpResponse = httpClient.execute(httpPost);
        log.info("{}",httpResponse.getStatusLine());
    }
}
