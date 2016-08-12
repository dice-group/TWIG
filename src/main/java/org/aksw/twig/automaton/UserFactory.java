package org.aksw.twig.automaton;

import java.util.HashMap;
import java.util.Map;

public class UserFactory {

    private final String baseName;

    private int userCount;

    private final Map<String, User> userNameMap = new HashMap<>();

    public UserFactory() {
        this.baseName = "user_";
    }

    public UserFactory(String baseName) {
        this.baseName = baseName;
    }

    public User newUser(int messageCount) {
        String name = baseName.concat(Integer.toString(userCount));
        User newUser = new User(messageCount, name);
        userNameMap.put(name, newUser);
        userCount++;
        return newUser;
    }

    public User byName(String name) {
        return userNameMap.get(name);
    }
}
