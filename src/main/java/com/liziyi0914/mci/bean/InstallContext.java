package com.liziyi0914.mci.bean;

import com.liziyi0914.mci.Identifier;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
public class InstallContext {

    private final Map<Identifier<?>,Object> map = new HashMap<>();

    public <T> void put(Identifier<T> key, T value) {
        map.put(key, value);
    }

    public <T> T get(Identifier<T> key) {
        return Optional.ofNullable(map.get(key)).map(key::cast).orElse(null);
    }

    public String toString() {
        return map.toString();
    }

}
