package com.kiwi.controller;

import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@LineMessageHandler
public class EventController {

    @Autowired
    private MessageController messageController;

    @Autowired
    private PostbackController postbackController;

    @EventMapping
    public void eventHandle(Event event) throws Exception {

        if (event instanceof MessageEvent) {
            final MessageEvent<?> messageEvent = (MessageEvent<?>) event;
            log.info("Text Event start");
            if (messageEvent.getMessage() instanceof TextMessageContent) {
                messageController.eventHandle((MessageEvent<TextMessageContent>) event);
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
            postbackController.eventHandle(postbackEvent);

        } else if (event instanceof BeaconEvent) {
            final BeaconEvent beaconEvent = (BeaconEvent) event;
            //reply(handleBeaconEvent(beaconEvent));
        }
    }
}
