package net.tomasbot.mp.api.service;

import java.util.*;

public class RandomEntitySelector {

    private static final Random R = new Random();

    public static <T> List<T> selectRandom(Collection<T> entities, final int count) {
        List<T> all = new ArrayList<>(entities);
        Set<T> random = new HashSet<>();

        final int maxTries = 10 + count * 2;

        int tries = 0;
        while (random.size() < count && tries++ <= maxTries) {
            int i = R.nextInt(all.size());
            random.add(all.get(i));
        }

        return random.stream().toList();
    }
}
