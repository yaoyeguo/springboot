package org.yaoyeguo.model.user;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private Integer code;
    //发送人信息
    private User user;

    private String receiverId;

    private String time;
    private String message;

    /**
     * 可以存放在线人数，在线用户列表，code等
     */
    private Map<String, Object> body = new HashMap<>();

    public WebSocketMessage(Integer code, String time, String message) {
        this.code = code;
        this.time = time;
        this.message = message;
    }

    public WebSocketMessage(Integer code, User user, String receiverId, String time, String message) {
        this.code = code;
        this.user = user;
        this.receiverId = receiverId;
        this.time = time;
        this.message = message;
    }
}
