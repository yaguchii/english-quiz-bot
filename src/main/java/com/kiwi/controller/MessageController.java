package com.kiwi.controller;

import com.linecorp.bot.client.LineMessagingServiceBuilder;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@LineMessageHandler
public class MessageController {

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
        log.info("Text message event: " + event);

        List<Action> actions = new ArrayList<>();
        URIAction uriAction = new URIAction("View detail",
                "https://tabelog.com/tokyo/A1301/A130103/13002611/");
        actions.add(uriAction);

        CarouselColumn carouselColumn = new CarouselColumn(
                "https://tblg.k-img.com/restaurant/images/Rvw/8431/150x150_square_8431740.jpg",
                "タイトル",
                "説明文", actions);

        List<CarouselColumn> columns = new ArrayList<>();
        columns.add(carouselColumn);

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

    @EventMapping
    public void handleDefaultMessageEvent(Event event) {
        log.info("event: " + event);
    }

}
