package com.ll.carjini.domain.chatbot.service;

import com.ll.carjini.domain.chatbot.entity.Chat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisChatService {

    private final RedisTemplate<String, Chat> chatRedisTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private String getChatHistoryKey(Long carOwnerId) {
        return "chat:history:" + carOwnerId;
    }

    private String getPendingChatKey(Long carOwnerId) {
        return "chat:pending:" + carOwnerId;
    }

    public void saveChat(Chat chat) {
        Long carOwnerId = chat.getCarOwnerId();
        String historyKey = getChatHistoryKey(carOwnerId);
        String pendingKey = getPendingChatKey(carOwnerId);

        chatRedisTemplate.opsForList().rightPush(historyKey, chat);

        long historySize = chatRedisTemplate.opsForList().size(historyKey);
        if (historySize > 20) {
            chatRedisTemplate.opsForList().trim(historyKey, historySize - 20, -1);
        }

        chatRedisTemplate.opsForList().rightPush(pendingKey, chat);
    }


    public List<Chat> getRecentChats(Long carOwnerId, int count) {
        String key = getChatHistoryKey(carOwnerId);
        long size = chatRedisTemplate.opsForList().size(key);

        List<Chat> chats = chatRedisTemplate.opsForList().range(key, Math.max(0, size - count), size - 1);
        return chats != null ? chats : new ArrayList<>();
    }


    public List<Chat> getPendingChats(Long carOwnerId) {
        String pendingKey = getPendingChatKey(carOwnerId);
        List<Chat> pendingChats = chatRedisTemplate.opsForList().range(pendingKey, 0, -1);
        return pendingChats != null ? pendingChats : new ArrayList<>();
    }

    public void clearPendingChats(Long carOwnerId) {
        String pendingKey = getPendingChatKey(carOwnerId);
        redisTemplate.delete(pendingKey);
    }
}