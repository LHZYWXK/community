package hk.hku.cs.community.util;

import hk.hku.cs.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户的信息，用于代替 session 对象
 * 线程隔离的
 */
@Component
public class HostHolder {
    private ThreadLocal<User> users = new ThreadLocal<>();

    public void setUser(User user) {
        users.set(user);
    }

    public User getUser() {
        return users.get();
    }

    public void clear() {
        users.remove();
    }

}
