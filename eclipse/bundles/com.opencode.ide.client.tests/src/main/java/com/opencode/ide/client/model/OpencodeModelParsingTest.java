package com.opencode.ide.client.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Locks the Gson - record mapping for the opencode OpenAPI types, using recorded
 * JSON samples for {@code /global/health}, {@code /agent} and
 * {@code /config/providers}. Exercises the tricky bits: snake_case permission
 * keys ({@code doom_loop}, {@code external_directory}), {@code models} as a map
 * keyed by model id, and the {@code default} reserved-word field.
 */
public class OpencodeModelParsingTest {

    private static final Gson GSON = new Gson();

    private static final String HEALTH_JSON = """
            { "healthy": true, "version": "1.18.16" }
            """;

    private static final String AGENTS_JSON = """
            [
              {
                "name": "build",
                "description": "The default agent. Executes tools based on configured permissions.",
                "mode": "primary",
                "native": true,
                "permission": [
                  { "permission": "*", "pattern": "*", "action": "allow" },
                  { "permission": "doom_loop", "pattern": "*", "action": "ask" },
                  { "permission": "external_directory", "pattern": "*", "action": "ask" }
                ],
                "options": {}
              },
              {
                "name": "explore",
                "description": "Fast read-only explorer.",
                "mode": "subagent",
                "native": true,
                "permission": [ { "permission": "read", "pattern": "*", "action": "allow" } ],
                "options": {}
              }
            ]
            """;

    private static final String PROVIDERS_JSON = """
            {
              "providers": [
                {
                  "id": "opencode",
                  "name": "OpenCode",
                  "source": "api",
                  "env": [],
                  "options": {},
                  "models": {
                    "claude-opus-5": {
                      "id": "claude-opus-5",
                      "providerID": "opencode",
                      "api": { "id": "anthropic", "url": "https://api.anthropic.com", "npm": "@ai-sdk/anthropic" },
                      "name": "Claude Opus 5",
                      "capabilities": {
                        "temperature": true, "reasoning": true, "attachment": true, "toolcall": true,
                        "input":  { "text": true, "audio": false, "image": true, "video": false, "pdf": true },
                        "output": { "text": true, "audio": false, "image": false, "video": false, "pdf": false }
                      },
                      "cost": { "input": 15.0, "output": 75.0, "cache": { "read": 1.5, "write": 18.75 } },
                      "limit": { "context": 200000, "output": 32000 },
                      "status": "active",
                      "options": {},
                      "headers": {}
                    }
                  }
                }
              ],
              "default": { "opencode": "claude-opus-5" }
            }
            """;

    @Test
    public void healthMaps() {
        HealthStatus health = GSON.fromJson(HEALTH_JSON, HealthStatus.class);
        assertTrue(health.healthy());
        assertEquals("1.18.16", health.version());
    }

    @Test
    public void agentsMapIncludingSnakeCasePermissions() {
        List<Agent> agents = GSON.fromJson(AGENTS_JSON,
                TypeToken.getParameterized(List.class, Agent.class).getType());

        assertEquals(2, agents.size());

        Agent build = agents.get(0);
        assertEquals("build", build.name());
        assertEquals(Agent.MODE_PRIMARY, build.mode());
        assertTrue("the 'native' field must map to nativeAgent", build.isNative());
        assertTrue(build.isPrimary());

        List<Agent.PermissionRule> rules = build.permission();
        assertNotNull(rules);
        assertEquals(3, rules.size());
        assertEquals("*", rules.get(0).permission());
        assertEquals("allow", rules.get(0).action());
        // snake_case permission category name must round-trip verbatim
        assertEquals("doom_loop", rules.get(1).permission());
        assertEquals("ask", rules.get(1).action());
        assertEquals("external_directory", rules.get(2).permission());

        // v1.18.x does not surface these on built-in agents -> remain null (forward-compat)
        assertNull("optional model should be null when absent", build.model());
        assertNull("optional temperature should be null when absent", build.temperature());

        Agent explore = agents.get(1);
        assertEquals(Agent.MODE_SUBAGENT, explore.mode());
        assertFalse(explore.isPrimary());
        assertTrue(explore.isNative());
    }

    @Test
    public void providersMapWithModelsAsMapAndDefault() {
        ProviderList list = GSON.fromJson(PROVIDERS_JSON, ProviderList.class);

        assertNotNull(list.providers());
        assertEquals(1, list.providers().size());

        Provider provider = list.providers().get(0);
        assertEquals("opencode", provider.id());
        assertEquals(Provider.SOURCE_API, provider.source());
        assertNotNull("models must deserialize as a map", provider.models());
        assertEquals(1, provider.models().size());

        Model model = provider.models().get("claude-opus-5");
        assertNotNull("model keyed by id must be present", model);
        assertEquals("claude-opus-5", model.id());
        assertEquals("opencode", model.providerID());
        assertTrue(model.capabilities().reasoning());
        assertTrue(model.capabilities().attachment());
        assertEquals(Model.STATUS_ACTIVE, model.status());
        assertEquals(200000L, model.limit().context());
        assertEquals(75.0, model.cost().output(), 0.0001);
        assertEquals(18.75, model.cost().cache().write(), 0.0001);

        assertNotNull("default map must deserialize", list.defaults());
        assertEquals("claude-opus-5", list.defaults().get("opencode"));
    }
}
