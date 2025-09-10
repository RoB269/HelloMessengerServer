package com.github.rob269.helloMessengerServer;

import java.time.LocalDateTime;

public record Chat(long chatId, String name, Message lastMessage,
                   boolean isPrivate) {

    @Override
    public String toString() {
        return (isPrivate ? "Contact:" : "Chat:") + "[" + chatId + "]" + name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Chat chat)) return false;
        return this.chatId == chat.chatId;
    }
}
