package com.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(VertxConfiguration.class)
public class WebSocketJobHandler /*extends TextWebSocketHandler*/ {

//    private final VertxFacade vertxFacade;
//
//    private final ConcurrentHashMap<String, String> relateMap = new ConcurrentHashMap<>();
//
//    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();


//    @PostConstruct
//    public void addListener() {
//        ITopic<JobInstanceTrack> job = vertxFacade.getHazelcast().getTopic(JobConstant.JOB_EVENT);
//        job.addMessageListener(event -> {
//            JobInstanceTrack track = event.getMessageObject();
//            Optional.ofNullable(track).map(JobInstanceTrack::getAccountPeriod).map(sessionMap::get).ifPresent(set -> set.forEach(session -> {
//                try {
//                    JobNotice notification = JobNotice.notification(track.getAccountPeriod(), event.getMessageObject());
//                    session.sendMessage(new TextMessage(JSON.toJSONString(notification)));
//                } catch (IOException e) {
//                    set.remove(session);
//                    log.error("WebSocket send message error", e);
//                }
//            }));
//        });
//    }
//
//
//    @Override
//    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
//        JobNotice notice = JSON.parseObject(message.getPayload(), JobNotice.class);
//        sessionMap.computeIfAbsent(notice.getAccountPeriod(), x -> new CopyOnWriteArraySet<>()).add(session);
//        relateMap.put(session.getId(), notice.getAccountPeriod());
//        String response = JSON.toJSONString(JobNotice.subscription(notice.getAccountPeriod(), "OK"));
//        session.sendMessage(new TextMessage(response));
//    }
//
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
//        String accountPeriod = relateMap.get(session.getId());
//        Optional.ofNullable(accountPeriod).map(sessionMap::get).ifPresent(set -> set.remove(session));
//    }

}

