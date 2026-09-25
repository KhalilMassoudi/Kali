package kali.microservices.infrastructureservice.openstack;

/**
 * Live ground-truth resource totals for the whole OpenStack project, read directly from
 * Nova/Cinder/Neutron rather than our own tracking tables — matches exactly what Horizon's
 * own "Overview" page shows, so it never drifts from real usage even for resources created
 * outside this platform.
 */
public record PlatformTotals(
        int instancesUsed,
        int runningInstances,
        int vcpusUsed,
        int ramMbUsed,
        int volumesUsed,
        int volumeGbUsed,
        int securityGroupsUsed,
        int floatingIpsUsed,
        int networksCount,
        int routersCount
) {
}
