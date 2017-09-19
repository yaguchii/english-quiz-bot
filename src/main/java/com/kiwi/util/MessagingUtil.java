package com.kiwi.util;

import com.linecorp.bot.client.LineMessagingServiceBuilder;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.ImageCarouselColumn;
import com.linecorp.bot.model.message.template.ImageCarouselTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrofit2.Response;

import java.util.List;

@Slf4j
@Component
public class MessagingUtil {

    public void pushText(String destination, String message) throws Exception {

        TextMessage textMessage = new TextMessage(message);
        PushMessage pushMessage = new PushMessage(
                destination,
                textMessage
        );
        push(pushMessage);
    }

    public void pushSticker(String destination, String packageId, String stickerId) throws Exception {

        StickerMessage stickerMessage = new StickerMessage(packageId, stickerId);
        PushMessage pushMessage = new PushMessage(
                destination,
                stickerMessage
        );
        push(pushMessage);
    }

    public void replyImageCarousel(String replyToken, List<ImageCarouselColumn> columns) throws Exception {

        ImageCarouselTemplate imageCarouselTemplate = new ImageCarouselTemplate(columns);
        TemplateMessage templateMessage = new TemplateMessage(
                "this is a image carousel template",
                imageCarouselTemplate);

        ReplyMessage replyMessage = new ReplyMessage(
                replyToken,
                templateMessage
        );
        reply(replyMessage);
    }

    private void reply(ReplyMessage replyMessage) throws java.io.IOException {
        Response<BotApiResponse> response =
                LineMessagingServiceBuilder
                        .create(System.getenv("LINE_BOT_CHANNEL_TOKEN"))
                        .build()
                        .replyMessage(replyMessage)
                        .execute();
        log.info(response.code() + " " + response.message());
    }

    private void push(PushMessage pushMessage) throws java.io.IOException {
        Response<BotApiResponse> response =
                LineMessagingServiceBuilder
                        .create(System.getenv("LINE_BOT_CHANNEL_TOKEN"))
                        .build()
                        .pushMessage(pushMessage)
                        .execute();
        log.info(response.code() + " " + response.message());
    }

}
