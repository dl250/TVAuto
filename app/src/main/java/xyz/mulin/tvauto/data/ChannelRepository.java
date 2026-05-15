package xyz.mulin.tvauto.data;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import xyz.mulin.tvauto.model.Channel;

public final class ChannelRepository {
    private static final String KEY_USER_CHANNELS = "user_channels";
    private static final String KEY_DEFAULTS_INITIALIZED = "defaults_initialized";

    private final SharedPreferences preferences;

    public ChannelRepository(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public synchronized List<Channel> loadUserChannels() {
        List<Channel> channels = new ArrayList<>();
        String json = preferences.getString(KEY_USER_CHANNELS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                channels.add(new Channel(object.getString("name"), object.getString("url")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return channels;
    }

    public synchronized void initializeDefaultsIfNeeded(String defaultChannelsText) {
        if (preferences.getBoolean(KEY_DEFAULTS_INITIALIZED, false)) return;

        List<Channel> existingChannels = loadUserChannels();
        if (!existingChannels.isEmpty()) {
            preferences.edit().putBoolean(KEY_DEFAULTS_INITIALIZED, true).apply();
            return;
        }

        List<Channel> defaultChannels = ChannelTextCodec.parse(defaultChannelsText);
        if (!defaultChannels.isEmpty()) {
            saveAll(defaultChannels);
        }
        preferences.edit().putBoolean(KEY_DEFAULTS_INITIALIZED, true).apply();
    }

    public synchronized boolean addChannel(Channel channel) {
        List<Channel> channels = loadUserChannels();
        for (Channel existing : channels) {
            if (existing.getUrl().equals(channel.getUrl())) return false;
        }
        channels.add(channel);
        saveAll(channels);
        return true;
    }

    public synchronized int importChannels(String text) {
        List<Channel> existing = loadUserChannels();
        LinkedHashMap<String, Channel> byUrl = new LinkedHashMap<>();
        for (Channel channel : existing) {
            byUrl.put(channel.getUrl(), channel);
        }

        int importedCount = 0;
        for (Channel channel : ChannelTextCodec.parse(text)) {
            if (!byUrl.containsKey(channel.getUrl())) {
                byUrl.put(channel.getUrl(), channel);
                importedCount++;
            }
        }

        if (importedCount > 0) {
            saveAll(new ArrayList<>(byUrl.values()));
        }
        return importedCount;
    }

    public synchronized boolean deleteByUrl(String url) {
        List<Channel> channels = loadUserChannels();
        boolean removed = false;
        for (int i = channels.size() - 1; i >= 0; i--) {
            if (channels.get(i).getUrl().equals(url)) {
                channels.remove(i);
                removed = true;
            }
        }
        if (removed) saveAll(channels);
        return removed;
    }

    public synchronized int deleteAll() {
        int deletedCount = loadUserChannels().size();
        if (deletedCount > 0) {
            saveAll(new ArrayList<>());
        }
        return deletedCount;
    }

    public synchronized int restoreDefaults(String defaultChannelsText) {
        List<Channel> defaultChannels = ChannelTextCodec.parse(defaultChannelsText);
        if (defaultChannels.isEmpty()) return 0;

        saveAll(defaultChannels);
        return defaultChannels.size();
    }

    public synchronized String exportAsText() {
        return ChannelTextCodec.encode(loadUserChannels());
    }

    private void saveAll(List<Channel> channels) {
        try {
            JSONArray array = new JSONArray();
            for (Channel channel : channels) {
                JSONObject object = new JSONObject();
                object.put("name", channel.getName());
                object.put("url", channel.getUrl());
                array.put(object);
            }
            preferences.edit().putString(KEY_USER_CHANNELS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
