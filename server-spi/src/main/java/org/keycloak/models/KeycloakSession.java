/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models;

import org.keycloak.provider.Provider;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface KeycloakSession {

    KeycloakContext getContext();

    KeycloakTransactionManager getTransaction();

    <T extends Provider> T getProvider(Class<T> clazz);

    <T extends Provider> T getProvider(Class<T> clazz, String id);

    <T extends Provider> Set<String> listProviderIds(Class<T> clazz);

    <T extends Provider> Set<T> getAllProviders(Class<T> clazz);

    void enlistForClose(Provider provider);

    KeycloakSessionFactory getKeycloakSessionFactory();

    /**
     * Returns a managed provider instance.  Will start a provider transaction.  This transaction is managed by the KeycloakSession
     * transaction.
     *
     * @return
     * @throws IllegalStateException if transaction is not active
     */
    RealmProvider realms();

    /**
     * Returns a managed provider instance.  Will start a provider transaction.  This transaction is managed by the KeycloakSession
     * transaction.
     *
     * @return
     * @throws IllegalStateException if transaction is not active
     */
    UserSessionProvider sessions();



    void close();

    /**
     * Possibly both cached and federated view of users depending on configuration.
     *
     * @return
     */
    UserFederationManager users();

    /**
     *  Keycloak user storage.  Non-federated, but possibly cache (if it is on) view of users.
     */
    UserProvider userStorage();
}
