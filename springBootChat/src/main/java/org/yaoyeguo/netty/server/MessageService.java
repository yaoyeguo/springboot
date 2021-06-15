package org.yaoyeguo.netty.server;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.time.DateUtils;
import org.yaoyeguo.model.user.User;
import org.yaoyeguo.model.user.WebSocketMessage;
import org.yaoyeguo.util.DateFormatUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessageService {
    public String messageJsonFactory(int msgCode,String chatMsg,int sysMsgCode,Object o){
        SimpleDateFormat sdf = DateFormatUtils.getFormat(DateFormatUtils.DATE_FORMAT2);
        final WebSocketMessage message = new WebSocketMessage(msgCode,chatMsg, sdf.format(new Date()));
        final Map<String, Object> map = new HashMap<>();
        map.put("systemMessageCode",sysMsgCode);
        map.put("object",o);
        message.setBody(map);
        return JSONObject.toJSONString(message);
    }

    public String messageJsonFactory(int msgCode, String chatMsg, User user,String receiveId){
        SimpleDateFormat sdf = DateFormatUtils.getFormat(DateFormatUtils.DATE_FORMAT2);
        final WebSocketMessage message = new WebSocketMessage(msgCode, user,receiveId,sdf.format(new Date()),chatMsg);
        return JSONObject.toJSONString(message);
    }
}
