/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.migration.migrators;

import java.util.Optional;
import org.keycloak.component.ComponentModel;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.userprofile.UserProfileProvider;

public class MigrateTo23_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("23.0.0");

    private static final String USER_PROFILE_ENABLED_PROP = "userProfileEnabled";
    private static final String UP_PIECES_COUNT_COMPONENT_CONFIG_KEY = "config-pieces-count";
    private static final String UP_PIECE_COMPONENT_CONFIG_KEY_BASE = "config-piece-";
    private static final String UP_COMPONENT_CONFIG_KEY = "kc.user.profile.config";

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(this::updateUserProfileConfig);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        updateUserProfileConfig(realm);
    }

    private void updateUserProfileConfig(RealmModel realm) {
        if (realm.getAttribute(USER_PROFILE_ENABLED_PROP, Boolean.FALSE)) {
            
            Optional<ComponentModel> component = realm.getComponentsStream(realm.getId(), UserProfileProvider.class.getName()).findAny();
            if (component.isPresent()) {
                ComponentModel userProfileComponent = component.get();
                int count = userProfileComponent.get(UP_PIECES_COUNT_COMPONENT_CONFIG_KEY, 0);
                userProfileComponent.getConfig().remove(UP_PIECES_COUNT_COMPONENT_CONFIG_KEY);
                if (count < 1) return; // default config
                String configuration;
                if (count == 1) {
                    configuration = userProfileComponent.get(UP_PIECE_COMPONENT_CONFIG_KEY_BASE + "0");
                    userProfileComponent.getConfig().remove(UP_PIECE_COMPONENT_CONFIG_KEY_BASE + "0");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < count; i++) {
                        String v = userProfileComponent.get(UP_PIECE_COMPONENT_CONFIG_KEY_BASE + i);
                        userProfileComponent.getConfig().remove(UP_PIECE_COMPONENT_CONFIG_KEY_BASE + i);
                        if (v != null) sb.append(v);
                    }
                    configuration = sb.toString();
                }
                userProfileComponent.getConfig().putSingle(UP_COMPONENT_CONFIG_KEY, configuration);
                realm.updateComponent(userProfileComponent);
            }
        }
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
