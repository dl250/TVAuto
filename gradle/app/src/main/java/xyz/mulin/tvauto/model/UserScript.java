package xyz.mulin.tvauto.model;

public final class UserScript {
    private final String sitePattern;
    private final String javascript;

    public UserScript(String sitePattern, String javascript) {
        this.sitePattern = sitePattern;
        this.javascript = javascript;
    }

    public String getSitePattern() {
        return sitePattern;
    }

    public String getJavascript() {
        return javascript;
    }
}
