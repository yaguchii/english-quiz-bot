package com.kiwi.controller;

import com.kiwi.postgre.ConnectionProvider;
import com.linecorp.bot.client.LineMessagingServiceBuilder;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.response.BotApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.Response;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
@RestController
public class PushController {

    @RequestMapping(value = "/push", method = RequestMethod.GET)
    public String get(@RequestParam("text") String text, @RequestParam("destId") String destId) throws Exception {

        TextMessage textMessage = new TextMessage(text);
        PushMessage pushMessage = new PushMessage(
                destId,
                textMessage
        );

        Response<BotApiResponse> response = LineMessagingServiceBuilder
                .create(System.getenv("LINE_BOT_CHANNEL_TOKEN"))
                .build()
                .pushMessage(pushMessage)
                .execute();
        log.info(response.code() + " " + response.message());
        return response.code() + " " + response.message();
    }

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public void test() throws Exception {

        ConnectionProvider connectionProvider = new ConnectionProvider();
        Connection connection = connectionProvider.getConnection();
        Statement stmt = connection.createStatement();
//            stmt.executeUpdate("SELECT * FROM PLACES;");
        ResultSet rs = stmt.executeQuery("SELECT * FROM SHOPS WHERE area = 'ginza'");
        while (rs.next()) {
            System.out.println(rs.getString("title"));
            System.out.println(rs.getString("uri"));
            System.out.println(rs.getString("text"));
            System.out.println(rs.getString("thumbnailImageUrl"));
        }


    }

}
