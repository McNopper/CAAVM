package com.opencode.ide.client;

import com.opencode.ide.client.model.ConfigInfo;
import com.opencode.ide.client.model.Model;
import com.opencode.ide.client.model.Provider;
import com.opencode.ide.client.model.ProviderList;

/**
 * Resolves which model to send when the user has not picked one. opencode
 * servers (v1.18.x) fail with HTTP 500 when a message is posted without an
 * explicit {@code model} or with a model that no longer exists (a stale
 * {@code /config} default), so the choice must be validated against the live
 * provider/model list. Pure - unit-testable.
 */
public final class DefaultModels {

    private DefaultModels() {
    }

    /**
     * @param config    the server config (may be {@code null}); its default model wins when valid
     * @param providers the live provider list (may be {@code null})
     * @return {@code [providerId, modelId]} or {@code null} when nothing can be resolved
     */
    public static String[] resolve(ConfigInfo config, ProviderList providers) {
        String[] configured = (config != null) ? config.defaultModelParts() : null;
        if (configured != null && exists(providers, configured[0], configured[1])) {
            return configured;
        }
        // fall back: first provider (in server order) that has models
        if (providers != null && providers.providers() != null) {
            for (Provider provider : providers.providers()) {
                if (provider == null || provider.models() == null || provider.models().isEmpty()) {
                    continue;
                }
                for (Model model : provider.models().values()) {
                    if (model != null && model.id() != null) {
                        return new String[] { provider.id(), model.id() };
                    }
                }
            }
        }
        return null;
    }

    private static boolean exists(ProviderList providers, String providerId, String modelId) {
        if (providers == null || providers.providers() == null) {
            return false;
        }
        for (Provider provider : providers.providers()) {
            if (provider == null || !providerId.equals(provider.id()) || provider.models() == null) {
                continue;
            }
            for (Model model : provider.models().values()) {
                if (model != null && modelId.equals(model.id())) {
                    return true;
                }
            }
        }
        return false;
    }
}
