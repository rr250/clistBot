package com.mattermost.clistBot.service;

import com.google.gson.Gson;
import com.mattermost.clistBot.domain.*;
import com.mattermost.clistBot.domain.infrastructure.PluginKeyValueStoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sun.misc.IOUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Service
@Slf4j
public class ClistBotServiceImpl implements ClistBotService {

    @Value("${clist_token}")
    private  String clistToken;

    @Value("${bot_token}")
    private String botToken;

    @Value("${zoom_access_token}")
    private String zoomAccessToken;

    @Value("${bot_id}")
    private String botId;

    @Value("${channel_id}")
    private String channelId;

    @Value("${mattermost_uri}")
    private String mattermostUri;

    @Autowired
    private PluginKeyValueStoreRepository pluginKeyValueStoreRepository;

//    @Override
//    @Scheduled(fixedDelay = 1000*60,initialDelay = 1000*60)
//    public  void callCloud() throws IOException {
//        CloseableHttpClient httpClient = HttpClients.createDefault();
//        HttpGet httpGet = new HttpGet("https://clistbot.el.r.appspot.com/");
//        httpClient.execute(httpGet);
//    }


    @Override
    @Scheduled(cron="0 0 0 * * *",zone = "Asia/Kolkata")
//    @Scheduled(fixedDelay = 1000*86400,initialDelay = 1000*60)
    public void sendDailyChallenges() throws IOException {
        JSONObject challengesJSON = getChallengesJSON();
        List<Challenge> challengeList = getChallenges(challengesJSON);
        SendPost sendPost = getSendPost(challengeList);
        sendPost(sendPost);
    }

    @Override
//    @Scheduled(fixedDelay = 1000*60,initialDelay = 1000*60)
    public  void sendEndChallengeNotification() throws IOException {
        JSONObject challengesJSON = getEndChallengesJSON();
//        JSONObject challengesJSON = getChallengesJSON();
        List<Challenge> challengeList = getChallenges(challengesJSON);
        for (Challenge challenge : challengeList) {
            if(challenge.getResource().equals("codeforces.com")||
                    challenge.getResource().equals("codechef.com")||
                    challenge.getResource().equals("hackerearth.com")||
                    challenge.getResource().equals("atcoder.jp")) {
                SendPost sendPost = getEndChallengeSendPost(challenge);
                executeCommand(sendPost);
            }
        }
    }

    @Override
//    @Scheduled(cron="0 0 0 * * FRI",zone = "Asia/Kolkata")
    @Scheduled(fixedDelay = 1000*86400,initialDelay = 1000*0)
    public void sendWeekendClasses() throws IOException, SQLException {
        List<Matterpoll> matterpollList = getMatterpollsThisWeek();
        for (Matterpoll matterpoll : matterpollList) {
            log.info("{}", matterpoll.getCreator());
            List<String> userIdList = getUserIdList(matterpoll);
            assert userIdList != null;
            if(userIdList.size()==0){
                continue;
            }
            userIdList=userIdList.size()>2?userIdList.subList(0,2):userIdList;
            List<User> userList = getUserList(userIdList);
            List<String> channelIds = getChannelIds(userIdList);
            ZoomMeeting zoomMeeting = getZoomMeeting(matterpoll.getQuestion().split("\\*").length>1?matterpoll.getQuestion().split("\\*")[1]:"");
        }
    }

    private List<Matterpoll> getMatterpollsThisWeek() throws SQLException, IOException {
        LocalDateTime now= LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        log.info("{}",now);
        List<PluginKeyValueStore> pluginKeyValueStoreList = pluginKeyValueStoreRepository.findAll();
        log.info("{}",pluginKeyValueStoreList);
        List<Matterpoll> matterpollList=new ArrayList<>();
        for (PluginKeyValueStore pluginKeyValueStore : pluginKeyValueStoreList) {
            if (pluginKeyValueStore.getPKey().substring(0, 4).equals("poll")) {
                byte[] bytes = new byte[9000];
                int read = pluginKeyValueStore.getPValue().getBinaryStream().read(bytes);
                String pValue = new String(bytes);
                JSONObject jsonObject = new JSONObject(pValue);
                long createdAtUnix = jsonObject.getLong("CreatedAt");
                LocalDateTime createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAtUnix), ZoneId.of("Asia/Kolkata"));
                log.info("{}", createdAt);
                if (createdAt.isAfter(ChronoLocalDateTime.from(now.minusDays(7)))) {
                    Matterpoll matterpoll = new Matterpoll();
                    matterpoll.setId(jsonObject.getString("ID"));
                    matterpoll.setCreatedAt(createdAt);
                    matterpoll.setCreator(jsonObject.getString("Creator"));
                    matterpoll.setQuestion(jsonObject.getString("Question"));
                    List<AnswerOption> answerOptions = new ArrayList<>();
                    JSONArray jsonArray = jsonObject.getJSONArray("AnswerOptions");
                    for (int j = 0; j < jsonArray.length(); j++) {
                        AnswerOption answerOption = new AnswerOption();
                        answerOption.setAnswer(jsonArray.getJSONObject(j).getString("Answer"));
                        JSONArray jsonArray1 = !jsonArray.getJSONObject(j).get("Voter").equals(null) ? jsonArray.getJSONObject(j).getJSONArray("Voter") : new JSONArray();
                        List<String> voters = new ArrayList<>();
                        for (int k = 0; k < jsonArray1.length(); k++) {
                            voters.add(jsonArray1.getString(k));
                        }
                        answerOption.setVoters(voters);
                        answerOptions.add(answerOption);
                    }
                    matterpoll.setAnswerOptions(answerOptions);
                    matterpollList.add(matterpoll);
                }
            }
        }
        return matterpollList;
    }

    private List<String> getUserIdList(Matterpoll matterpoll) {
        List<AnswerOption> answerOptions = matterpoll.getAnswerOptions();
        for (AnswerOption answerOption : answerOptions) {
            if (answerOption.getAnswer().equals("Yes")) {
                return answerOption.getVoters();
            }
        }
        return new ArrayList<>();
    }

    private List<String> getChannelIds(List<String> userIdList) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        List<String> channelIds = new ArrayList<>();
        for (String s : userIdList) {
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(s);
            jsonArray.put(botId);
            HttpPost httpPost = new HttpPost(mattermostUri + "/api/v4/channels/direct");
            httpPost.addHeader("Authorization", botToken);
            StringEntity postingString = new StringEntity(jsonArray.toString(), "UTF8");
            httpPost.setEntity(postingString);
            httpPost.setHeader("Content-type", "application/json");
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            String responseString = EntityUtils.toString(httpEntity);
            JSONObject responseJson = new JSONObject(responseString);
            log.info("{}", responseJson);
            channelIds.add(responseJson.getString("id"));
        }
        return channelIds;
    }

    private List<User> getUserList(List<String> userIdList) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        JSONArray jsonArray = new JSONArray(userIdList);
        HttpPost httpPost = new HttpPost(mattermostUri + "/api/v4/users/ids");
        httpPost.addHeader("Authorization", botToken);
        StringEntity postingString = new StringEntity(jsonArray.toString(), "UTF8");
        httpPost.setEntity(postingString);
        log.info("{}",jsonArray);
        httpPost.setHeader("Content-type", "application/json");
        HttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity httpEntity = httpResponse.getEntity();
        String responseString = EntityUtils.toString(httpEntity);
        JSONArray responseJson = new JSONArray(responseString);
        log.info("{}", responseJson);
        List<User> userList = new ArrayList<>();
        for(int i=0;i<responseJson.length();i++){
            User user = new User();
            user.setUserId(responseJson.getJSONObject(i).getString("id"));
            user.setUserName(responseJson.getJSONObject(i).getString("username"));
            user.setFirstName(responseJson.getJSONObject(i).getString("first_name"));
            user.setLastName(responseJson.getJSONObject(i).getString("last_name"));
        }
        return userList;
    }

    private ZoomMeeting getZoomMeeting(String topic) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("topic","sample");
        jsonObject.put("type",1);
        jsonObject.put("password","12345");
        jsonObject.put("type",1);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("https://api.zoom.us/v2/users/rrrishabh7@gmail.com/meetings");
        httpPost.addHeader("Authorization", zoomAccessToken);
        StringEntity postingString = new StringEntity(jsonObject.toString(), "UTF8");
        httpPost.setEntity(postingString);
        httpPost.setHeader("Content-type", "application/json");
        HttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity httpEntity = httpResponse.getEntity();
        String responseString = EntityUtils.toString(httpEntity);
        JSONObject responseJson = new JSONObject(responseString);
        log.info("{}",responseJson);
        return null;
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
        sendPost.setMessage("Challenges starting today");
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

    private JSONObject getEndChallengesJSON() throws IOException {
        LocalDateTime now= LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        log.info("{}",now);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("https://clist.by/api/v1/contest/?limit=20&offset=0&order_by=-start&end__gt="+now+"&end__lt="+now.plusMinutes(1));
        httpGet.setHeader("Authorization",clistToken);
        HttpResponse httpResponse = httpClient.execute(httpGet);
        HttpEntity httpEntity = httpResponse.getEntity();
        String responseString = EntityUtils.toString(httpEntity);
        JSONObject responseJson = new JSONObject(responseString);
        log.info("{}",responseJson.getJSONArray("objects"));
        return responseJson;
    }

    private SendPost getEndChallengeSendPost(Challenge challenge){
        SendPost sendPost = new SendPost();
        sendPost.setCommand("/poll \"CP:\\*"+challenge.getEvent()+"\\* has ended. Did you complete it?\" \"Yes and I would like to teach\" \"Yes\" \"No\"");
        sendPost.setChannel_id(channelId);
        sendPost.setProps(null);
        return sendPost;
    }

    private void executeCommand(SendPost sendPost) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(mattermostUri+"/api/v4/commands/execute");
        httpPost.addHeader("Authorization",botToken);
        Gson gson = new Gson();
        StringEntity postingString = new StringEntity(gson.toJson(sendPost),"UTF8");
        log.info("{}",gson.toJson(sendPost));
        httpPost.setEntity(postingString);
        httpPost.setHeader("Content-type", "application/json");
        HttpResponse httpResponse = httpClient.execute(httpPost);
        log.info("{}",httpResponse);
    }


}
