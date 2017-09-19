package com.kiwi.controller;

import com.kiwi.model.QuizInfo;
import com.kiwi.postgre.ConnectionProvider;
import com.kiwi.util.MessagingUtil;
import com.linecorp.bot.client.LineMessagingServiceBuilder;
import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.template.ImageCarouselColumn;
import com.linecorp.bot.model.profile.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import retrofit2.Response;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
class MessageController {

    @Autowired
    private MessagingUtil messagingUtil;

    void eventHandle(MessageEvent<TextMessageContent> event) throws Exception {

        // connection
        ConnectionProvider connectionProvider = new ConnectionProvider();
        Connection connection = connectionProvider.getConnection();
        Statement stmt = connection.createStatement();

        if (event.getMessage().getText().equals("quiz") ||
                event.getMessage().getText().equals("Quiz") ||
                event.getMessage().getText().equals("クイズ")) {

            messagingUtil.pushText(event.getSource().getSenderId(), "Choose a category😎");

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
            messagingUtil.pushText(event.getSource().getSenderId(), "Thank you for playing. see you😎");
        }

        stmt.close();
        connection.close();
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
        messagingUtil.replyImageCarousel(replyToken, columns);
    }

    private ImageCarouselColumn createImageCarouselColumn(QuizInfo quizInfo) throws Exception {
        Action action = new PostbackAction(quizInfo.getCategory(), quizInfo.getCategory(), "Let's start with " + quizInfo.getCategory() + " quiz!");
        return new ImageCarouselColumn(quizInfo.getThumbnailImageUrl(), action);
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

}
