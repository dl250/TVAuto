package xyz.mulin.tvauto.data;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import xyz.mulin.tvauto.model.UserScript;

public final class UserScriptRepository {
    private static final String KEY_USER_SCRIPTS = "user_scripts";

    private final SharedPreferences preferences;

    public UserScriptRepository(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public synchronized List<UserScript> loadAll() {
        List<UserScript> scripts = new ArrayList<>();
        String json = preferences.getString(KEY_USER_SCRIPTS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                scripts.add(new UserScript(
                        object.getString("sitePattern"),
                        object.getString("javascript")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scripts;
    }

    public synchronized void upsert(String sitePattern, String javascript) {
        String normalizedPattern = sitePattern.trim();
        List<UserScript> scripts = loadAll();

        boolean replaced = false;
        for (int i = 0; i < scripts.size(); i++) {
            UserScript script = scripts.get(i);
            if (script.getSitePattern().equalsIgnoreCase(normalizedPattern)) {
                scripts.set(i, new UserScript(normalizedPattern, javascript));
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            scripts.add(new UserScript(normalizedPattern, javascript));
        }
        saveAll(scripts);
    }

    public synchronized boolean deleteByPattern(String sitePattern) {
        List<UserScript> scripts = loadAll();
        boolean removed = false;
        for (int i = scripts.size() - 1; i >= 0; i--) {
            if (scripts.get(i).getSitePattern().equalsIgnoreCase(sitePattern)) {
                scripts.remove(i);
                removed = true;
            }
        }
        if (removed) saveAll(scripts);
        return removed;
    }

    public synchronized String exportAsJson() {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("scripts", toJsonArray(loadAll()));
            return root.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"version\":1,\"scripts\":[]}";
        }
    }

    public synchronized ImportResult importFromJson(String json) throws Exception {
        Object parsed = new JSONTokener(json).nextValue();
        JSONArray array;
        if (parsed instanceof JSONObject) {
            array = ((JSONObject) parsed).getJSONArray("scripts");
        } else if (parsed instanceof JSONArray) {
            array = (JSONArray) parsed;
        } else {
            throw new IllegalArgumentException("Invalid script file");
        }

        List<UserScript> scripts = loadAll();
        int added = 0;
        int updated = 0;

        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            String sitePattern = object.getString("sitePattern").trim();
            String javascript = object.getString("javascript");
            if (sitePattern.isEmpty()) continue;

            int existingIndex = indexOfPattern(scripts, sitePattern);
            if (existingIndex >= 0) {
                scripts.set(existingIndex, new UserScript(sitePattern, javascript));
                updated++;
            } else {
                scripts.add(new UserScript(sitePattern, javascript));
                added++;
            }
        }

        saveAll(scripts);
        return new ImportResult(added, updated);
    }

    public synchronized UserScript findBestMatch(String url) {
        if (url == null || url.isEmpty()) return null;

        String normalizedUrl = url.toLowerCase(Locale.ROOT);
        UserScript bestMatch = null;
        for (UserScript script : loadAll()) {
            String pattern = script.getSitePattern().trim();
            if (pattern.isEmpty()) continue;

            if (normalizedUrl.contains(pattern.toLowerCase(Locale.ROOT))) {
                if (bestMatch == null || pattern.length() > bestMatch.getSitePattern().length()) {
                    bestMatch = script;
                }
            }
        }
        return bestMatch;
    }

    private void saveAll(List<UserScript> scripts) {
        try {
            JSONArray array = toJsonArray(scripts);
            preferences.edit().putString(KEY_USER_SCRIPTS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int indexOfPattern(List<UserScript> scripts, String sitePattern) {
        for (int i = 0; i < scripts.size(); i++) {
            if (scripts.get(i).getSitePattern().equalsIgnoreCase(sitePattern)) {
                return i;
            }
        }
        return -1;
    }

    private JSONArray toJsonArray(List<UserScript> scripts) throws Exception {
        JSONArray array = new JSONArray();
        for (UserScript script : scripts) {
            JSONObject object = new JSONObject();
            object.put("sitePattern", script.getSitePattern());
            object.put("javascript", script.getJavascript());
            array.put(object);
        }
        return array;
    }

    public static final class ImportResult {
        private final int added;
        private final int updated;

        ImportResult(int added, int updated) {
            this.added = added;
            this.updated = updated;
        }

        public int getAdded() {
            return added;
        }

        public int getUpdated() {
            return updated;
        }
    }
}
