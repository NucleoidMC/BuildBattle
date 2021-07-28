package eu.pb4.buildbattle.themes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public record Theme(String name, List<String> list) {
    public static final Codec<Theme> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("themeName").forGetter(theme -> theme.name),
            Codec.list(Codec.STRING).fieldOf("themes").forGetter(theme -> theme.list)
    ).apply(instance, Theme::new));

    public String getRandom() {
        return this.list.get((int) Math.round(Math.random() * (this.list.size() - 1)));
    }

    public List<String> getMultipleRandom(int count) {
        List<String> notSelected = new ArrayList<>(this.list);
        List<String> out = new ArrayList<>();

        for (int x = 0; x < count; x++) {
            out.add(notSelected.remove((int) Math.round(Math.random() * (notSelected.size() - 1))));
        }

        return out;
    }
}
