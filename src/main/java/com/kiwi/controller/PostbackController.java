package com.kiwi.controller;

import com.kiwi.model.DataInfo;
import com.kiwi.postgre.ConnectionProvider;
import com.kiwi.util.MessagingUtil;
import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.message.template.ImageCarouselColumn;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

@Slf4j
@Controller
class PostbackController {

    @Autowired
    private MessagingUtil messagingUtil;

    // select number
    private static final int SELECT_NUM = 4;

    // result
    private static final String RESULT_CORRECT = "correct";
    private static final String RESULT_WRONG = "wrong";

    // result stickers
    private static final String[] CORRECT_STICKERS = {"13", "14", "407", "125", "179"};
    private static final String[] WRONG_STICKERS = {"7", "9", "16", "21", "108", "111", "403"};

    void eventHandle(PostbackEvent event) throws Exception {

        String postackData = event.getPostbackContent().getData();
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
            messagingUtil.pushText(event.getSource().getSenderId(), "OK.Next question.");

            ResultSet rs = stmt.executeQuery("SELECT * FROM DATA WHERE category = '" + category + "' ORDER BY random() LIMIT " + SELECT_NUM);
            sendImageCarouselMessageForQuestion(event.getReplyToken(), rs, event);

        } else {
            // 指定されたcategoryからランダムで取得する
            ResultSet rs = stmt.executeQuery("SELECT * FROM DATA WHERE category = '" + postackData + "' ORDER BY random() LIMIT " + SELECT_NUM);
            sendImageCarouselMessageForQuestion(event.getReplyToken(), rs, event);
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
            messagingUtil.pushText(event.getSource().getSenderId(), "Which is " + "\"" + dataInfo.getTitle() + "\"" + "?");
        } else {
            messagingUtil.pushText(event.getSource().getSenderId(), "Which is " + "\"" + dataInfo.getName() + "\"" + "?");
        }
        Action action = new PostbackAction("choose", dataInfo.getCategory() + ":" + RESULT_CORRECT, dataInfo.getName());
        ImageCarouselColumn imageCarouselColumn = new ImageCarouselColumn(dataInfo.getThumbnailImageUrl(), action);

        columns.set(num, imageCarouselColumn);

        messagingUtil.replyImageCarousel(replyToken, columns);
    }

    private ImageCarouselColumn createImageCarouselColumnForQuestion(DataInfo dataInfo) throws Exception {
        Action action = new PostbackAction("choose", dataInfo.getCategory() + ":" + RESULT_WRONG, dataInfo.getName());
        return new ImageCarouselColumn(dataInfo.getThumbnailImageUrl(), action);
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

       messagingUtil.pushSticker(destination, packageId, stickerId);
    }
}
