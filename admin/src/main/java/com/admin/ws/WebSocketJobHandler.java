package com.admin.ws;

import com.alibaba.fastjson2.JSON;
import com.common.config.VertxConfiguration;
import com.common.constant.Constant;
import com.common.dag.NodeEdgeDAG;
import com.common.entity.JobFlowInstance;
import com.common.util.DAGUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hazelcast.core.HazelcastInstance;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(VertxConfiguration.class)
public class WebSocketJobHandler extends TextWebSocketHandler implements InitializingBean {

    private final HazelcastInstance hazelcast;

    private static final Map<WebSocketSession, Long> sessionMap = new ConcurrentHashMap<>();

    private static final Map<Long, CopyOnWriteArrayList<WebSocketSession>> notifyMap = new ConcurrentHashMap<>();


    @Override
    public void afterPropertiesSet() {
        hazelcast.getTopic(Constant.JOB_FLOW_EVENT).addMessageListener(message -> {
            try {
                JobFlowInstance instance = json2Instance(message.getMessageObject().toString());
                if (instance != null) {
                    sendMessageToClient(instance.getId(), instance);
                }
            } catch (Exception e) {
                log.error("WebSocketJobHandler sendMessageToClient error", e);
            }
        });
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 解析消息
        String payload = message.getPayload();
        Message messageBody = JSON.parseObject(payload, Message.class);

        // 处理订阅请求
        if ("subscribe".equals(messageBody.getType()) && messageBody.getWorkflowInstanceId() != null) {
            sessionMap.put(session, messageBody.getWorkflowInstanceId());
            notifyMap.computeIfAbsent(messageBody.getWorkflowInstanceId(), k -> new CopyOnWriteArrayList<>()).add(session);
            // 发送响应消息
            Message reply = new Message().setType("subscribe").setMessage("success");
            session.sendMessage(new TextMessage(JSON.toJSONString(reply)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 当连接关闭时，从会话映射中移除该客户端
        String clientId = session.getId();
        Long workflowInstanceId = sessionMap.remove(session);
        if (workflowInstanceId != null) {
            notifyMap.get(workflowInstanceId).remove(session);
        }
        log.info("WebSocket connection closed: clientId={}, workflowInstanceId={}", clientId, workflowInstanceId);
    }


    // 向指定客户端发送消息的方法
    public void sendMessageToClient(Long workflowInstanceId, JobFlowInstance instance) throws Exception {
        CopyOnWriteArrayList<WebSocketSession> sessions = notifyMap.getOrDefault(workflowInstanceId, new CopyOnWriteArrayList<>());
        for (WebSocketSession session : sessions) {
            log.info("WebSocketJobHandler job_flow_event instance = {} version = {}, status = {}", instance.getName(), instance.getVersion(), instance.getStatus());
            NodeEdgeDAG nodeEdgeDAG = DAGUtil.fromJSONString(instance.getDag());
            Message message = new Message().setWorkflowInstanceId(instance.getId()).setType("event").setStatus(instance.getStatus()).setDag(nodeEdgeDAG);
            session.sendMessage(new TextMessage(JSON.toJSONString(message)));
        }
    }


    private static JobFlowInstance json2Instance(String json) {
        try {
            return new ObjectMapper().registerModule(new JavaTimeModule()).readValue(json, JobFlowInstance.class);
        } catch (JsonProcessingException e) {
            log.info("Failed to serialize JobFlowInstance to JSON", e);
            return null;
        }
    }


    @Data
    @Accessors(chain = true)
    public static class Message {
        private Long workflowInstanceId;
        private String type;
        private String message;
        private Integer status;
        private NodeEdgeDAG dag;
    }
}

