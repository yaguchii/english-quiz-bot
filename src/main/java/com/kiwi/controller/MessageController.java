package com.kiwi.controller;

import com.kiwi.model.DataInfo;
import com.kiwi.model.QuizInfo;
import com.kiwi.postgre.ConnectionProvider;
import com.linecorp.bot.client.LineMessagingServiceBuilder;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.ImageCarouselColumn;
import com.linecorp.bot.model.message.template.ImageCarouselTemplate;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

@Slf4j
@LineMessageHandler
public class MessageController {

    // select number
    private static final int SELECT_NUM = 4;

    // result
    private static final String RESULT_CORRECT = "correct";
    private static final String RESULT_WRONG = "wrong";

    // result stickers
    private static final String[] CORRECT_STICKERS = {"13", "14", "407", "125", "179"};
    private static final String[] WRONG_STICKERS = {"7", "9", "16", "21", "108", "111", "403"};

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
            String postackData = postbackEvent.getPostbackContent().getData();
            log.info("postackData=" + postackData);

            ConnectionProvider connectionProvider = new ConnectionProvider();
            Connection connection = connectionProvider.getConnection();
            Statement stmt = connection.createStatement();

            if (postackData.contains(RESULT_CORRECT) || postackData.contains(RESULT_WRONG)) {
                // postackData is like "dog:wrong"
                String category = postackData.split(":")[0];
                String result = postackData.split(":")[1];

                // sticker送信
                sendSticker(event.getSource().getSenderId(), result);

                // 同じカテゴリでクイズ継続
                // send "Next question."
                sendMessage(event.getSource().getSenderId(), "OK.Next question.");

                ResultSet rs = stmt.executeQuery("SELECT * FROM DATA WHERE category = '" + category + "' ORDER BY random() LIMIT " + SELECT_NUM);
                sendImageCarouselMessageForQuestion(postbackEvent.getReplyToken(), rs, event);

            } else {
                // 指定されたcategoryからランダムで取得する
                ResultSet rs = stmt.executeQuery("SELECT * FROM DATA WHERE category = '" + postackData + "' ORDER BY random() LIMIT " + SELECT_NUM);
                sendImageCarouselMessageForQuestion(postbackEvent.getReplyToken(), rs, event);
            }

            stmt.close();
            connection.close();

        } else if (event instanceof BeaconEvent) {
            final BeaconEvent beaconEvent = (BeaconEvent) event;
            //reply(handleBeaconEvent(beaconEvent));
        }
    }

    private void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
        log.info("Text message event: " + event);
        log.info("SenderId: " + event.getSource().getSenderId());
        log.info("UserId: " + event.getSource().getUserId());

        // connection
        ConnectionProvider connectionProvider = new ConnectionProvider();
        Connection connection = connectionProvider.getConnection();
        Statement stmt = connection.createStatement();

        if (event.getMessage().getText().equals("quiz") ||
                event.getMessage().getText().equals("Quiz") ||
                event.getMessage().getText().equals("クイズ")) {

            sendMessage(event.getSource().getSenderId(), "Choose a category😎");

            // ユーザ情報取得
            if (event.getSource().getUserId() != null) {
                setUserProfile(event.getSource().getUserId(), connection);
            }

            ResultSet rs = stmt.executeQuery("SELECT * FROM QUIZ");
            sendQuizList(event.getReplyToken(), rs);
        }

        if (event.getMessage().getText().equals("end") ||
                event.getMessage().getText().equals("End") ||
                event.getMessage().getText().equals("終了")) {
            sendMessage(event.getSource().getSenderId(), "Thank you for playing. see you😎");
        }

        stmt.close();
        connection.close();
    }

    private void sendImageCarouselMessageForQuestion(String replyToken, ResultSet rs, Event event) throws Exception {

        List<ImageCarouselColumn> columns = new ArrayList<>();
        List<DataInfo> dataInfos = new ArrayList<>();
        while (rs.next()) {
            DataInfo dataInfo = new DataInfo();
            dataInfo.setCategory(rs.getString("category"));
            dataInfo.setName(rs.getString("name"));
            dataInfo.setThumbnailImageUrl(rs.getString("thumbnailImageUrl"));
            dataInfo.setTitle(rs.getString("title"));
            ImageCarouselColumn imageCarouselColumn = createImageCarouselColumnForQuestion(dataInfo);

            columns.add(imageCarouselColumn);
            dataInfos.add(dataInfo);
        }

        // quizInfosからランダムに正解を選び、ユーザに質問する
        Random rand = new Random();
        int num = rand.nextInt(SELECT_NUM);
        DataInfo dataInfo = dataInfos.get(num);

        // case of leader, ask title .
        if (dataInfo.getCategory().equals("Leader")) {
            sendMessage(event.getSource().getSenderId(), "Which is " + "\"" + dataInfo.getTitle() + "\"" + "?");
        } else {
            sendMessage(event.getSource().getSenderId(), "Which is " + "\"" + dataInfo.getName() + "\"" + "?");
        }
        Action action = new PostbackAction("choose", dataInfo.getCategory() + ":" + RESULT_CORRECT, dataInfo.getName());
        ImageCarouselColumn imageCarouselColumn = new ImageCarouselColumn(dataInfo.getThumbnailImageUrl(), action);

        columns.set(num, imageCarouselColumn);

        ImageCarouselTemplate imageCarouselTemplate = new ImageCarouselTemplate(columns);
        TemplateMessage templateMessage = new TemplateMessage(
                "this is a image carousel template",
                imageCarouselTemplate);

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


    private void sendQuizList(String replyToken, ResultSet rs) throws Exception {

        List<ImageCarouselColumn> columns = new ArrayList<>();
        while (rs.next()) {
            QuizInfo quizInfo = new QuizInfo();
            quizInfo.setCategory(rs.getString("category"));
            quizInfo.setThumbnailImageUrl(rs.getString("thumbnailImageUrl"));
            ImageCarouselColumn imageCarouselColumn = createImageCarouselColumn(quizInfo);
            columns.add(imageCarouselColumn);
        }

        ImageCarouselTemplate imageCarouselTemplate = new ImageCarouselTemplate(columns);
        TemplateMessage templateMessage = new TemplateMessage(
                "this is a image carousel template",
                imageCarouselTemplate);

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

    private ImageCarouselColumn createImageCarouselColumn(QuizInfo quizInfo) throws Exception {
        Action action = new PostbackAction(quizInfo.getCategory(), quizInfo.getCategory(), "Let's start with " + quizInfo.getCategory() + " quiz!");
        return new ImageCarouselColumn(quizInfo.getThumbnailImageUrl(), action);
    }

    private ImageCarouselColumn createImageCarouselColumnForQuestion(DataInfo dataInfo) throws Exception {
        Action action = new PostbackAction("choose", dataInfo.getCategory() + ":" + RESULT_WRONG, dataInfo.getName());
        return new ImageCarouselColumn(dataInfo.getThumbnailImageUrl(), action);
    }

    private void setUserProfile(String userId, Connection connection) throws Exception {
        Response<UserProfileResponse> response =
                LineMessagingServiceBuilder
                        .create(System.getenv("LINE_BOT_CHANNEL_TOKEN"))
                        .build()
                        .getProfile(userId)
                        .execute();
        if (response.isSuccessful()) {
            UserProfileResponse profile = response.body();
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("INSERT INTO LINE_USER (user_id, display_name, picture_url, status_message) " +
                    "VALUES ('" + userId + "', '" + profile.getDisplayName() + "', '" + profile.getPictureUrl() + "' , '" + profile.getStatusMessage() + "') " +
                    "ON CONFLICT (user_id) DO UPDATE SET display_name = '" + profile.getDisplayName() + "', picture_url = '" + profile.getPictureUrl() + "' , status_message= '" + profile.getStatusMessage() + "'");
        } else {
            log.info(response.code() + " " + response.message());
        }
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

    private void sendSticker(String destination, String result) throws Exception {

        String packageId;
        String stickerId;

        // シャッフルした配列からstickerを選択
        if (result.equals(RESULT_CORRECT)) {
            List<String> list = Arrays.asList(CORRECT_STICKERS);
            Collections.shuffle(list);
            packageId = "1";
            stickerId = list.get(0);
        } else {
            List<String> list = Arrays.asList(WRONG_STICKERS);
            Collections.shuffle(list);
            packageId = "1";
            stickerId = list.get(0);
        }
        StickerMessage stickerMessage = new StickerMessage(packageId, stickerId);
        PushMessage pushMessage = new PushMessage(
                destination,
                stickerMessage
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
