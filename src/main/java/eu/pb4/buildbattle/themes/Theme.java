package eu.pb4.buildbattle.themes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class Theme {
    public static final Codec<Theme> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("themeName").forGetter(theme -> theme.name),
            Codec.list(Codec.STRING).fieldOf("themes").forGetter(theme -> theme.list)
    ).apply(instance, Theme::new));

    private final String name;
    private final List<String> list;

    private Theme(String name, List<String> list) {
        this.name = name;
        this.list = list;
    }

    public String getRandom() {
        return this.list.get((int) Math.round(Math.random() * (this.list.size() - 1)));
    }

}
