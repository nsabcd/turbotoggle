package com.turbotoggle.repository.projection;

public interface FlagPayloadProjection {
    String getFlagKey();
    String getFlagType();
    Boolean getEnabled();
    String getDefaultVariation();
    String getRulesJson();            // Native Postgres JSONB as String
    String getIndividualTargetsJson(); // Native Postgres JSONB as String
    Long getVersion();
}
