package com.kiwi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiwi.model.CarouselInfo;
import com.linecorp.bot.client.LineMessagingServiceBuilder;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import retrofit2.Response;

import java.net.URI;
import java.util.ArrayList;
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
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
        log.info("Text message event: " + event);

        if (event.getMessage().getText().equals("銀座")) {
            justPush(event, "銀座のお店をご紹介します。");

            Jedis jedis = getConnection();

            List<CarouselColumn> columns = new ArrayList<>();
            Map<String, String> ginza = jedis.hgetAll("ginza");

            for (Map.Entry<String, String> entry : ginza.entrySet()) {
                System.out.println(entry.getKey());
                System.out.println(entry.getValue());

                ObjectMapper mapper = new ObjectMapper();
                String jsonInString = entry.getValue();

                CarouselInfo carouselInfo = mapper.readValue(jsonInString, CarouselInfo.class);

                // 地域名を渡すだけ
                List<Action> actions = new ArrayList<>();
                URIAction uriAction = new URIAction(
                        "View detail",
                        carouselInfo.getUri());
                actions.add(uriAction);

                CarouselColumn carouselColumn = new CarouselColumn(
                        carouselInfo.getThumbnailImageUrl(),
                        carouselInfo.getTitle(),
                        carouselInfo.getText(),
                        actions);

                columns.add(carouselColumn);
            }

            CarouselTemplate carouselTemplate = new CarouselTemplate(columns);

            TemplateMessage templateMessage = new TemplateMessage(
                    "this is a carousel template",
                    carouselTemplate);

            ReplyMessage replyMessage = new ReplyMessage(
                    event.getReplyToken(),
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

    }

    private void justPush(MessageEvent<TextMessageContent> event, String message) throws Exception {

        TextMessage textMessage = new TextMessage(message);
        PushMessage pushMessage = new PushMessage(
                event.getSource().getUserId(),
                textMessage
        );
        Response<BotApiResponse> response =
                LineMessagingServiceBuilder
                        .create(System.getenv("LINE_BOT_CHANNEL_TOKEN"))
                        .build()
                        .pushMessage(pushMessage)
                        .execute();
        System.out.println(response.code() + " " + response.message());
    }

    @EventMapping
    public void handleDefaultMessageEvent(Event event) {
        log.info("event: " + event);
    }

}
