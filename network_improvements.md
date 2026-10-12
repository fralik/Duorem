# Network improvements

## Goal

Restore Wake-on-LAN compatibility for users affected by issue #37, then improve
network handling consistently across the app without undoing the Android
modernization.

Previously, Duorem let Android route Wake-on-LAN datagrams. Version 1.2.0 selects
a Wi-Fi/Ethernet network and explicitly binds the socket to it. That binding can
be rejected while a VPN is active, before the wake packet is sent. This is the
leading explanation for the report; delivery still needs confirmation on the
reporter's setup.

The higher minimum Android version does not require explicit socket binding.
Local-network permissions and VPN routing restrictions remain relevant regardless
of which socket approach is used.

## Phase 1: focused Wake-on-LAN compatibility fix

1. Stop explicitly binding the Wake-on-LAN UDP socket to the selected Android
   network. Let Android and the user's VPN configuration choose the route.
2. Parse the validated numeric destination without network-specific resolution.
3. Preserve MAC/address/port validation, explicit broadcast-address precedence,
   automatic LAN broadcast fallback, broadcast support, cancellation, socket
   cleanup, local-network permissions, and error reporting.
4. Apply the change in the shared sender so both the main screen and widgets
   benefit. Leave SSH, status probes, discovery, network selection, and permission
   handling unchanged in this phase.
5. Do not introduce a routing preference, broad exception fallback, SDK downgrade,
   or automatic VPN bypass.

### Verification and release

- Add a regression test proving that sending works without consulting the
  selected Android network, even when that network would reject socket binding.
- Check the exact UDP packet, configured destination and port, automatic
  destination fallback, invalid-input failures, and cancellation.
- Run focused JVM tests and build the debug app.
- Before release, verify real-device wake delivery with and without a VPN, from
  both the main screen and a widget. Ask the reporter to confirm their setup.
- Include a home Wi-Fi network without Internet, concurrent mobile data, and
  local-network permission denial in device checks.
- Document that a successful send is not proof that the computer woke up.

### Tradeoffs

Ordinary routing removes the binding operation implicated in #37; it does not
guarantee delivery through every VPN. A VPN may block LAN traffic. With several
connections, Android may not choose the intended LAN, especially when that LAN
has no Internet. Do not claim that the compatibility fix overcomes those limits
or also fixes VPN-related SSH/status/discovery problems.

## Phase 2: consistent long-term networking

1. Separate local-network information used for discovery/broadcast calculation
   from the routing policy used to contact a configured device.
2. Define a shared routing policy for Wake-on-LAN, SSH shutdown/restart, status,
   discovery, and widgets. Prefer ordinary Android routing for normal device
   connections, respecting the user's VPN configuration.
3. Handle non-default LANs deliberately. Evaluate explicit network selection
   where Android permits it, and preserve local discovery and LAN-without-Internet
   support. Specify when a different route may be attempted before implementing
   retries; do not silently defeat VPN policy.
4. Keep address resolution, reachability checks, and transport routing consistent.
   Reevaluate the selected network and cancel stale work when connectivity changes.
5. Distinguish missing permission, rejected network access, an unreachable device,
   and unavailable SSH in logs and user-facing results. Do not label every
   networking failure as an offline device. Avoid asserting that a VPN caused an
   error unless there is sufficient evidence.
6. Extend Help with actionable guidance for local-network permission, VPN LAN
   access, and Android's blocking of connections outside a VPN.

### Acceptance matrix

Exercise supported Android versions and both foreground/widget entry points:

| Setup | Expected outcome |
| --- | --- |
| Ordinary Wi-Fi or Ethernet | Wake and configured SSH commands work. |
| LAN without Internet, with mobile data available | The intended local device remains usable through an allowed route. |
| VPN allowing LAN destinations | Normal routing remains compatible with the VPN configuration. |
| VPN blocking LAN traffic or outside-VPN connections | Failures are explained; restrictions are not bypassed. |
| Missing or revoked local-network permission | Request permission or explain the denial; do not send traffic. |
| Wi-Fi disconnect/reconnect or network switch | Cancel stale work and use current network information. |
| Device online without SSH | Report reachability separately from SSH availability. |

Ship phase 1 independently. Keep phase 2 in a separate change so the immediate
compatibility fix does not depend on a networking redesign.
