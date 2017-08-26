package com.kiwi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiwi.model.CarouselInfo;
import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/redis")
public class RedisController {

    @Autowired
    private RedisTemplate redisTemplate;


    @RequestMapping(method = RequestMethod.GET)
    public void get() throws Exception {

        Jedis jedis = getConnection();
        Map<String, String> ginza = jedis.hgetAll("ginza");

        List<CarouselColumn> columns = new ArrayList<>();

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
    }

    private static Jedis getConnection() throws Exception {
        URI redisURI = new URI(System.getenv("REDIS_URL"));
        return new Jedis(redisURI);
    }

}
