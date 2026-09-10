package com.example.concurrent.perfomance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class EvenBetterAttributeStore {
    private Map<String, String> attributes = new ConcurrentHashMap<>();

    public boolean userLocationMatches(String name, String regexp) {
        String key = "users." + name + ".location";
        String location = attributes.get(key);
        if (location == null) return false;
        else return Pattern.matches(regexp, location);
    }

}
