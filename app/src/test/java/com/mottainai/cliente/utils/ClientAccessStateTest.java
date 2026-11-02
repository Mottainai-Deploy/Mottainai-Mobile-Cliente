package com.mottainai.cliente.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Reúne os cenários de teste de ClientAccessState. */
public class ClientAccessStateTest {
    /** Verifica se a localização disponível não dispensa a sessão autenticada. */
    @Test public void sessionIsRequiredEvenWhenLocationIsAvailable() {
        assertEquals(ClientAccessState.LOGIN_REQUIRED,
                ClientAccessState.evaluate(false, false, true, true));
    }

    /** Verifica o bloqueio dos serviços quando falta permissão de localização. */
    @Test public void missingPermissionBlocksServices() {
        assertEquals(ClientAccessState.LOCATION_REQUIRED,
                ClientAccessState.evaluate(true, false, false, true));
    }

    /** Verifica o bloqueio dos serviços com a localização do aparelho desligada. */
    @Test public void disabledDeviceLocationBlocksServices() {
        assertEquals(ClientAccessState.LOCATION_REQUIRED,
                ClientAccessState.evaluate(true, false, true, false));
    }

    /** Verifica o acesso em primeiro plano com permissão e localização ativadas. */
    @Test public void grantAndEnabledDeviceAllowForegroundServices() {
        assertEquals(ClientAccessState.READY,
                ClientAccessState.evaluate(true, false, true, true));
    }

    /** Verifica o comportamento de localização do modo de demonstração de depuração. */
    @Test public void debugDemoDoesNotNeedRealLocation() {
        assertEquals(ClientAccessState.READY,
                ClientAccessState.evaluate(true, true, false, false));
    }

    /** Verifica se a marcação de demonstração não substitui uma sessão de acesso ausente. */
    @Test public void demoFlagCannotOverrideMissingBrowseSession() {
        assertEquals(ClientAccessState.LOGIN_REQUIRED,
                ClientAccessState.evaluate(false, true, true, true));
    }

    /** Verifica as combinações de sessão, permissão e localização do aparelho. */
    @Test public void allPermissionCombinationsRespectSessionAndDeviceRequirements() {
        for (boolean session : new boolean[]{false, true}) {
            for (boolean permission : new boolean[]{false, true}) {
                for (boolean enabled : new boolean[]{false, true}) {
                    ClientAccessState expected = !session ? ClientAccessState.LOGIN_REQUIRED
                            : permission && enabled ? ClientAccessState.READY : ClientAccessState.LOCATION_REQUIRED;
                    assertEquals(expected, ClientAccessState.evaluate(session, false, permission, enabled));
                }
            }
        }
    }
}
