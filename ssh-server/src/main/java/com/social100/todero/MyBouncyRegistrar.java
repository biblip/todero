package com.social100.todero;

import org.apache.sshd.common.util.security.AbstractSecurityProviderRegistrar;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;

/** Minimal registrar that tells Apache MINA SSHD to use BouncyCastle. */
public class MyBouncyRegistrar extends AbstractSecurityProviderRegistrar {

    public MyBouncyRegistrar() {
        super("BC");      // registrar name
    }

    @Override
    public Provider getSecurityProvider() {
        return new BouncyCastleProvider();
    }

    @Override
    public boolean isSupported() {
        return true;      // always OK on a normal JVM
    }
}