package org.ngengine.auth;

public class AuthConfig implements Cloneable{
    protected final AuthStrategy strategy;
    protected String forNpub;
    protected Auth auth;
    public AuthConfig(AuthStrategy strategy, String forNpub) {
        this.strategy = strategy;
        this.forNpub = forNpub;
    }

    public AuthConfig(AuthStrategy strategy) {
        this.strategy = strategy;
        this.forNpub = null;
    }

    public AuthStrategy getStrategy() {
        return strategy;
    }

    public String getForNpub() {
        return forNpub;
    }

    public void setForNpub(String forNpub) {
        this.forNpub = forNpub;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Auth getAuth() {
        return auth;
    }

    @Override
    public AuthConfig clone() {
        try {
            return (AuthConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Cloning not supported", e);
        }
    }
}
