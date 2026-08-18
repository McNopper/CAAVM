package com.opencode.ide.client;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.google.gson.Gson;
import com.opencode.ide.client.model.ConfigInfo;
import com.opencode.ide.client.model.ProviderList;

/**
 * Unit tests for {@link DefaultModels} - the validated default-model fallback
 * (opencode v1.18.x 500s on missing or stale models, so defaults must be
 * checked against the live provider list).
 */
public class DefaultModelsTest {

    private static final Gson GSON = new Gson();

    /** zai has glm-5.2 (valid default) and glm-4.6 is stale; ollama has one model. */
    private static final String PROVIDERS_JSON = """
            { "providers": [
                { "id": "zai-coding-plan", "name": "ZAI", "source": "api", "env": [], "options": {},
                  "models": {
                    "glm-5.2": { "id": "glm-5.2", "providerID": "zai-coding-plan", "name": "GLM 5.2" },
                    "glm-4.7": { "id": "glm-4.7", "providerID": "zai-coding-plan", "name": "GLM 4.7" } } },
                { "id": "ollama", "name": "Ollama", "source": "env", "env": [], "options": {},
                  "models": { "llama3": { "id": "llama3", "providerID": "ollama" } } } ],
              "default": {} }
            """;

    private static ProviderList providers() {
        return GSON.fromJson(PROVIDERS_JSON, ProviderList.class);
    }

    @Test
    public void validConfigDefaultWins() {
        ConfigInfo config = new ConfigInfo("zai-coding-plan/glm-5.2", null);
        assertArrayEquals(new String[] { "zai-coding-plan", "glm-5.2" },
                DefaultModels.resolve(config, providers()));
    }

    @Test
    public void staleConfigDefaultFallsBackToFirstAvailable() {
        // glm-4.6 no longer exists on the provider -> first available model of the first provider
        ConfigInfo config = new ConfigInfo("zai-coding-plan/glm-4.6", null);
        assertArrayEquals(new String[] { "zai-coding-plan", "glm-5.2" },
                DefaultModels.resolve(config, providers()));
    }

    @Test
    public void unknownProviderInConfigFallsBack() {
        ConfigInfo config = new ConfigInfo("ghost-provider/whatever", null);
        assertArrayEquals(new String[] { "zai-coding-plan", "glm-5.2" },
                DefaultModels.resolve(config, providers()));
    }

    @Test
    public void nullConfigFallsBack() {
        assertArrayEquals(new String[] { "zai-coding-plan", "glm-5.2" },
                DefaultModels.resolve(null, providers()));
    }

    @Test
    public void nothingAvailableYieldsNull() {
        assertNull(DefaultModels.resolve(null, null));
        assertNull(DefaultModels.resolve(null, GSON.fromJson("{\"providers\":[]}", ProviderList.class)));
    }

    @Test
    public void malformedConfigDefaultFallsBack() {
        ConfigInfo config = new ConfigInfo("no-slash", null); // defaultModelParts() -> null
        assertArrayEquals(new String[] { "zai-coding-plan", "glm-5.2" },
                DefaultModels.resolve(config, providers()));
    }
}
