package xyz.mulin.tvauto.model;

import java.util.Objects;

public final class Channel {
    private final String name;
    private final String url;

    public Channel(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public boolean isBuiltInHelpChannel() {
        return url.startsWith("file:///");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Channel)) return false;
        Channel channel = (Channel) other;
        return Objects.equals(name, channel.name) && Objects.equals(url, channel.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }
}
