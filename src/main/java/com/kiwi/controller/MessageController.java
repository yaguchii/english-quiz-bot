package com.kiwi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiwi.model.ShopInfo;
import com.linecorp.bot.client.LineMessagingServiceBuilder;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import redis.clients.jedis.Jedis;
import retrofit2.Response;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@LineMessageHandler
public class MessageController {

    private static Jedis getConnection() throws Exception {
        URI redisURI = new URI(System.getenv("REDIS_URL"));
        return new Jedis(redisURI);
    }

    @EventMapping
    public void eventHandle(Event event) throws Exception {

        if (event instanceof MessageEvent) {
            final MessageEvent<?> messageEvent = (MessageEvent<?>) event;
            log.info("Text Event start");
            if (messageEvent.getMessage() instanceof TextMessageContent) {
                handleTextMessageEvent((MessageEvent<TextMessageContent>) event);
            }

        } else if (event instanceof UnfollowEvent) {
            UnfollowEvent unfollowEvent = (UnfollowEvent) event;
            //handleUnfollowEvent(unfollowEvent);

        } else if (event instanceof FollowEvent) {
            FollowEvent followEvent = (FollowEvent) event;
            //reply(handleFollowEvent(followEvent));

        } else if (event instanceof JoinEvent) {
            final JoinEvent joinEvent = (JoinEvent) event;
            //reply(handleJoinEvent(joinEvent));

        } else if (event instanceof LeaveEvent) {
            final LeaveEvent leaveEvent = (LeaveEvent) event;
            //handleLeaveEvent(leaveEvent);

        } else if (event instanceof PostbackEvent) {
            final PostbackEvent postbackEvent = (PostbackEvent) event;
            //reply(handlePostbackEvent(postbackEvent));
            log.info("Postback Event start");

            // area存在チェック
            Jedis jedis = getConnection();
            Map<String, String> areaMap = jedis.hgetAll("area");
            if (areaMap.containsKey(postbackEvent.getPostbackContent().getData())) {
                String areaNameEn = postbackEvent.getPostbackContent().getData();
                String areaNameJp = areaMap.get(areaNameEn);
//                sendMessage(postbackEvent.getSource().getSenderId(), areaNameJp + "のお店をご紹介しますぜ、社長。");
                sendCarouselMessage(postbackEvent.getReplyToken(), "gourmet:" + areaNameEn);
            }

        } else if (event instanceof BeaconEvent) {
            final BeaconEvent beaconEvent = (BeaconEvent) event;
            //reply(handleBeaconEvent(beaconEvent));
        }
    }

    private void setUserProfile(String userId) throws Exception {
        Jedis jedis = getConnection();
        Response<UserProfileResponse> response =
                LineMessagingServiceBuilder
                        .create(System.getenv("LINE_BOT_CHANNEL_TOKEN"))
                        .build()
                        .getProfile(userId)
                        .execute();
        if (response.isSuccessful()) {
            UserProfileResponse profile = response.body();
            log.info(profile.getDisplayName());
            log.info(profile.getPictureUrl());
            log.info(profile.getStatusMessage());

            HashMap<String, String> map = new HashMap<>();
            map.put("displayName", profile.getDisplayName());
            map.put("pictureUrl", profile.getPictureUrl());
            jedis.hmset("userId:" + userId, map);

        } else {
            log.info(response.code() + " " + response.message());
        }
    }

    private void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
        log.info("Text message event: " + event);
        log.info("SenderId: " + event.getSource().getSenderId());
        log.info("UserId: " + event.getSource().getUserId());

        // ユーザ情報取得
        if (event.getSource().getUserId() != null) {
            setUserProfile(event.getSource().getUserId());
        }

        // area存在チェック
        Jedis jedis = getConnection();
        Map<String, String> areaMap = jedis.hgetAll("area");

        if (areaMap.containsValue(event.getMessage().getText())) {
            // areaに含まれる場合

            String areaNameJp = event.getMessage().getText();
            BidiMap bidiMap = new DualHashBidiMap(areaMap);
            String areaNameEn = bidiMap.getKey(areaNameJp).toString();

            if (jedis.exists("gourmet:" + areaNameEn)) {
                // 1件以上お店情報がある場合

                // profile取得
//                setUserProfile(event.getSource().getUserId());

                // ○○をご案内いたしましょうか？ Yes, No
                sendConfirmMessage(event.getReplyToken(), areaNameJp, areaNameEn);

            } else {
                // areaにはあるが、お店情報が存在しない場合
                // nothing
            }

        } else {
            // areaに含まれない場合
            // nothing
        }
    }

    private void sendConfirmMessage(String replyToken, String areaNameJp, String areaNameEn) throws Exception {

        List<Action> actions = new ArrayList<>();
        PostbackAction postbackAction = new PostbackAction(
                "Yes",
                areaNameEn,
                "Yes");
        MessageAction messageAction = new MessageAction(
                "No",
                "No");

        actions.add(postbackAction);
        actions.add(messageAction);

        ConfirmTemplate confirmTemplate = new ConfirmTemplate(areaNameJp + "のお店をご案内したします。よろしいですか？", actions);
        TemplateMessage templateMessage = new TemplateMessage(
                "this is a confirm template",
                confirmTemplate);

        ReplyMessage replyMessage = new ReplyMessage(
                replyToken,
                templateMessage
        );
        Response<BotApiResponse> response =
                LineMessagingServiceBuilder
                        .create(System.getenv("LINE_BOT_CHANNEL_TOKEN"))
                        .build()
                        .replyMessage(replyMessage)
                        .execute();
        log.info(response.code() + " " + response.message());
    }

    private void sendCarouselMessage(String replyToken, String key) throws Exception {

        Jedis jedis = getConnection();
        List<CarouselColumn> columns = new ArrayList<>();
        List<String> storeLlist = jedis.lrange(key, 0, -1);

        for (String storeJson : storeLlist) {
            log.info(storeJson);
            CarouselColumn carouselColumn = createCarouselColumn(storeJson);
            columns.add(carouselColumn);
        }

        CarouselTemplate carouselTemplate = new CarouselTemplate(columns);
        TemplateMessage templateMessage = new TemplateMessage(
                "this is a carousel template",
                carouselTemplate);

        ReplyMessage replyMessage = new ReplyMessage(
                replyToken,
                templateMessage
        );
        Response<BotApiResponse> response =
                LineMessagingServiceBuilder
                        .create(System.getenv("LINE_BOT_CHANNEL_TOKEN"))
                        .build()
                        .replyMessage(replyMessage)
                        .execute();
        log.info(response.code() + " " + response.message());
    }

    private CarouselColumn createCarouselColumn(String jsonInString) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ShopInfo shopInfo = mapper.readValue(jsonInString, ShopInfo.class);

        List<Action> actions = new ArrayList<>();
        URIAction uriAction = new URIAction(
                "View detail",
                shopInfo.getUri());
        actions.add(uriAction);

        return new CarouselColumn(
                shopInfo.getThumbnailImageUrl(),
                shopInfo.getTitle(),
                shopInfo.getText(),
                actions);
    }

    private void sendMessage(String destination, String message) throws Exception {

        TextMessage textMessage = new TextMessage(message);
        PushMessage pushMessage = new PushMessage(
                destination,
                textMessage
        );
        Response<BotApiResponse> response =
                LineMessagingServiceBuilder
                        .create(System.getenv("LINE_BOT_CHANNEL_TOKEN"))
                        .build()
                        .pushMessage(pushMessage)
                        .execute();
        log.info(response.code() + " " + response.message());
    }

}
