package kali.microservices.infrastructureservice.openstack;

/**
 * Live ground-truth usage vs. quota for the whole OpenStack project, read directly from
 * Nova/Cinder/Neutron rather than our own tracking tables — the same numbers Horizon's own
 * "Overview" limit summary shows, so it never drifts from real usage even for resources
 * created outside this platform.
 */
public record PlatformTotals(
        int runningInstances,
        Quota instances,
        Quota vcpus,
        Quota ramMb,
        Quota volumes,
        Quota volumeGb,
        Quota snapshots,
        Quota floatingIps,
        Quota securityGroups,
        Quota securityGroupRules,
        Quota networks,
        Quota ports,
        Quota routers
) {
    /** `limit` is the project quota; OpenStack reports -1 for "unlimited". */
    public record Quota(int used, int limit) {
    }
}
