package com.compassites.GDSWrapper.indigo;

import play.Play;

public class Indigo {
    public static final String DOMAIN_CODE;
    public static final String AGENT_CODE;
    public static final String AGENT_PASSWORD;
    public static final Integer contractVersion=452;
    public static final Integer sessionValidity=30;
    static {
        DOMAIN_CODE = Play.application().configuration().getString("indigo.domain.code");
        AGENT_CODE = Play.application().configuration().getString("indigo.domain.agentCode");
        AGENT_PASSWORD = Play.application().configuration().getString("indigo.domain.password");
    }
}
