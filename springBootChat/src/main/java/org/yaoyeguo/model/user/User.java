package org.yaoyeguo.model.user;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class User {

    private String id;
    private String nick;
    private String address;
    private String avatarAddress;
    //上线时间
    private long time = 0;
}
