/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.example.multitenant.control;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import net.seannos.example.multitenant.model.Tenant;
import net.seannos.example.multitenant.util.Constants;
import net.seannos.example.multitenant.util.PropertyStore;

import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;

/**
 * This code was originally written by <a href=
 * "https://github.com/keycloak/keycloak/blob/1.1.0.Final/examples/multi-tenant/src/main/java/org/keycloak/example/multitenant/control/PathBasedKeycloakConfigResolver.java"
 * >Juraci Paixão Kröhling</a>
 * 
 * @author Takayuki Konishi <seannos.takayuki at gmail.com>
 */
public class PathBasedKeycloakConfigResolver implements KeycloakConfigResolver {

	private final Map<String, KeycloakDeployment> cache = new ConcurrentHashMap<String, KeycloakDeployment>();

	@Override
	public KeycloakDeployment resolve(HttpFacade.Request request) {

		String multitenant = PropertyStore.get(Constants.PROP_MULTITENANT_PATH)
				+ "/";

		String path = request.getURI();
		int multitenantIndex = path.indexOf(multitenant);
		if (multitenantIndex == -1) {
			throw new IllegalStateException(
					"Not able to resolve realm from the request path!");
		}

		String realm = path.substring(path.indexOf(multitenant)).split("/")[1];
		if (realm.contains("?")) {
			realm = realm.split("\\?")[0];
		}

		KeycloakDeployment deployment = cache.get(realm);
		if (null == deployment) {
			InputStream is = getInputStream(realm);
			if (is == null) {
				throw new IllegalStateException(
						"Not able to find any records about " + realm);
			}
			deployment = KeycloakDeploymentBuilder.build(is);
			cache.put(realm, deployment);
		}

		return deployment;
	}

	private InputStream getInputStream(String realm) {
		String findTenantByName = "findTenantByName";

		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("keycloak");
		EntityManager em = emf.createEntityManager();
		Query query = em.createNamedQuery(findTenantByName, Tenant.class);
		List<Tenant> tenants = (List<Tenant>) query
				.setParameter("realm", realm).getResultList();
		if (tenants.size() == 1) {
			return new ByteArrayInputStream(tenants.get(0).getJson()
					.getBytes(StandardCharsets.UTF_8));
		}
		return null;
	}

}
