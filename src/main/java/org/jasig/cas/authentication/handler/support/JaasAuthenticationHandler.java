/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.authentication.handler.support;

import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.validation.constraints.NotNull;

import org.jasig.cas.authentication.HandlerResult;
import org.jasig.cas.authentication.PreventedException;
import org.jasig.cas.authentication.UsernamePasswordCredential;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.authentication.principal.SimplePrincipal;
import org.springframework.util.Assert;

/**
 * JAAS Authentication Handler for CAAS. This is a simple bridge from CAS'
 * authentication to JAAS.
 * <p>
 * Using the JAAS Authentication Handler requires you to configure the
 * appropriate JAAS modules. You can specify the location of a jass.conf file
 * using the following VM parameter:
 * 
 * <pre>
 * -Djava.security.auth.login.config=$PATH_TO_JAAS_CONF/jaas.conf
 * </pre>
 * <p>
 * This example jaas.conf would try Kerberos based authentication, then try LDAP
 * authentication:
 * 
 * <pre>
 * CAS {
 *   com.sun.security.auth.module.Krb5LoginModule sufficient
 *     client=TRUE
 *     debug=FALSE
 *     useTicketCache=FALSE;
 *   edu.uconn.netid.jaas.LDAPLoginModule sufficient
 *     java.naming.provider.url="ldap://ldapserver.my.edu:389/dc=my,dc=edu"
 *     java.naming.security.principal="uid=jaasauth,dc=my,dc=edu"
 *     java.naming.security.credentials="password"
 *     Attribute="uid"
 *     startTLS="true";
 * };
 * </pre>
 * 
 * @author <a href="mailto:dotmatt@uconn.edu">Matthew J. Smith</a>
 * @author Marvin S. Addison
 * @since 3.0.5
 * @see javax.security.auth.callback.CallbackHandler
 * @see javax.security.auth.callback.PasswordCallback
 * @see javax.security.auth.callback.NameCallback
 */
public class JaasAuthenticationHandler extends AbstractUsernamePasswordAuthenticationHandler {

    /** If no realm is specified, we default to CAS. */
    private static final String DEFAULT_REALM = "CAS";

    /** The realm that contains the login module information. */
    @NotNull
    private String realm = DEFAULT_REALM;

    public JaasAuthenticationHandler() {
        Assert
                .notNull(Configuration.getConfiguration(),
                        "Static Configuration cannot be null. Did you remember to specify \"java.security.auth.login.config\"?");
    }

    /** {@inheritDoc} */
    @Override
    protected final HandlerResult authenticateUsernamePasswordInternal(final UsernamePasswordCredential credential)
            throws GeneralSecurityException, PreventedException {

        final String username = credential.getUsername();
        final LoginContext lc = new LoginContext(this.realm, new UsernamePasswordCallbackHandler(username, credential
                .getPassword()));
        try {
            logger.debug("Attempting authentication for: {}", username);
            lc.login();
        } finally {
            lc.logout();
        }

        Principal principal = null;
        final Set<java.security.Principal> principals = lc.getSubject().getPrincipals();
        if (principals != null && principals.size() > 0) {

            principal = new SimplePrincipal(principals.iterator().next().getName());
        }
        return createHandlerResult(credential, principal, null);
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    /**
     * A simple JAAS CallbackHandler which accepts a Name String and Password
     * String in the constructor. Only NameCallbacks and PasswordCallbacks are
     * accepted in the callback array. This code based loosely on example given
     * in Sun's javadoc for CallbackHandler interface.
     */
    protected static final class UsernamePasswordCallbackHandler implements CallbackHandler {

        /** The username of the principal we are trying to authenticate. */
        private final String userName;

        /** The password of the principal we are trying to authenticate. */
        private final String password;

        /**
         * Constuctor accepts name and password to be used for authentication.
         * 
         * @param userName name to be used for authentication
         * @param password Password to be used for authentication
         */
        protected UsernamePasswordCallbackHandler(final String userName, final String password) {
            this.userName = userName;
            this.password = password;

        }

        public void handle(final Callback[] callbacks) throws UnsupportedCallbackException {
            for (final Callback callback : callbacks) {
                if (callback.getClass().equals(NameCallback.class)) {
                    ((NameCallback) callback).setName(this.userName);
                } else if (callback.getClass().equals(PasswordCallback.class)) {
                    ((PasswordCallback) callback).setPassword(this.password.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(callback, "Unrecognized Callback");
                }
            }
        }
    }
}
