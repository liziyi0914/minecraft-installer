package com.liziyi0914.mci.bean;

import com.liziyi0914.mci.Identifier;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
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

    public <T,S extends Collection<T>> void addAll(Identifier<S> key, S value) {
        Optional<S> opt = getOpt(key);
        if (opt.isPresent()) {
            opt.get().addAll(value);
        } else {
            put(key,value);
        }
    }

    public <T> T get(Identifier<T> key) {
        return Optional.ofNullable(map.get(key)).map(key::cast).orElse(null);
    }

    public <T> Optional<T> getOpt(Identifier<T> key) {
        return Optional.ofNullable(map.get(key)).map(key::cast);
    }

    public String toString() {
        return map.toString();
    }

}
