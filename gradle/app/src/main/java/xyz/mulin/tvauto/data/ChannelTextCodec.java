package xyz.mulin.tvauto.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.mulin.tvauto.model.Channel;

public final class ChannelTextCodec {
    private static final Pattern CHANNEL_PATTERN =
            Pattern.compile("^\\s*\"(.*?)\"\\s*,\\s*\"(.*?)\"\\s*$");

    private ChannelTextCodec() {
    }

    public static List<Channel> parse(String text) {
        List<Channel> result = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return result;

        String[] entries = text.split(";");
        for (String entry : entries) {
            Matcher matcher = CHANNEL_PATTERN.matcher(entry);
            if (!matcher.matches()) continue;

            String name = matcher.group(1).trim();
            String url = matcher.group(2).trim();
            if (!name.isEmpty() && !url.isEmpty()) {
                result.add(new Channel(name, url));
            }
        }
        return result;
    }

    public static String encode(List<Channel> channels) {
        StringBuilder builder = new StringBuilder();
        for (Channel channel : channels) {
            if (channel.isBuiltInHelpChannel()) continue;
            if (builder.length() > 0) builder.append('\n');
            builder.append('"')
                    .append(escape(channel.getName()))
                    .append("\",\"")
                    .append(escape(channel.getUrl()))
                    .append("\";");
        }
        return builder.toString();
    }

    private static String escape(String text) {
        return text.replace("\"", "\\\"");
    }
}
