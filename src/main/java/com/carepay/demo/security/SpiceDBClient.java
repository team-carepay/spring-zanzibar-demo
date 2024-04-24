package com.carepay.demo.security;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import com.authzed.api.v1.Core;
import com.authzed.api.v1.PermissionService;
import com.authzed.api.v1.PermissionsServiceGrpc;
import com.authzed.grpcutil.BearerToken;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpiceDBClient implements ZanzibarClient {

    private final PermissionsServiceGrpc.PermissionsServiceBlockingStub permissionsService;

    private final String userSubjectType;

    public SpiceDBClient(final SpiceDBProperties spiceDBProperties) {
        final ManagedChannelBuilder<?> managedChannelBuilder = ManagedChannelBuilder.forTarget(spiceDBProperties.host());
        if (spiceDBProperties.useSsl()) {
            managedChannelBuilder.useTransportSecurity();
        } else {
            managedChannelBuilder.usePlaintext();
        }
        final ManagedChannel channel = managedChannelBuilder.build();
        final BearerToken bearerToken = new BearerToken(spiceDBProperties.token());
        permissionsService = PermissionsServiceGrpc.newBlockingStub(channel).withCallCredentials(bearerToken);
        userSubjectType = Optional.ofNullable(spiceDBProperties.userSubjectType()).orElse("user");
    }

    public String addRelation(final String resourceType, final Object resourceId, final String relation, final String subjectType, final Object subjectId) {
        PermissionService.WriteRelationshipsRequest writeRelationshipsRequest = PermissionService.WriteRelationshipsRequest.newBuilder()
                .addUpdates(
                        Core.RelationshipUpdate.newBuilder()
                                .setOperation(Core.RelationshipUpdate.Operation.OPERATION_CREATE)
                                .setRelationship(
                                        Core.Relationship.newBuilder()
                                                .setResource(
                                                        Core.ObjectReference.newBuilder()
                                                                .setObjectType(resourceType)
                                                                .setObjectId(resourceId.toString())
                                                                .build())
                                                .setRelation(relation)
                                                .setSubject(
                                                        Core.SubjectReference.newBuilder()
                                                                .setObject(
                                                                        Core.ObjectReference.newBuilder()
                                                                                .setObjectType(subjectType)
                                                                                .setObjectId(subjectId.toString())
                                                                                .build())
                                                                .build())
                                                .build())
                                .build())
                        .build();
        PermissionService.WriteRelationshipsResponse writeRelationshipsResponse = permissionsService.writeRelationships(writeRelationshipsRequest);
        return writeRelationshipsResponse.getWrittenAt().getToken();
    }

    public boolean checkPermission(String resourceType, Object resourceId, String permission, String subjectType, Object subjectId) {
        final PermissionService.CheckPermissionRequest request = PermissionService.CheckPermissionRequest.newBuilder()
                .setConsistency(PermissionService.Consistency.newBuilder().setMinimizeLatency(true).build())
                .setResource(Core.ObjectReference.newBuilder().setObjectType(resourceType).setObjectId(resourceId.toString()).build())
                .setSubject(Core.SubjectReference.newBuilder().setObject(Core.ObjectReference.newBuilder().setObjectType(subjectType).setObjectId(subjectId.toString()).build()).build())
                .setPermission(permission)
                .build();

        final PermissionService.CheckPermissionResponse response = permissionsService.checkPermission(request);
        return response.getPermissionship() == PermissionService.CheckPermissionResponse.Permissionship.PERMISSIONSHIP_HAS_PERMISSION;
    }

    public boolean checkPermission(Authentication authentication, String resourceType, Object resourceId, String permission) {
        return checkPermission(resourceType, resourceId, permission, userSubjectType, authentication.getName());
    }

    @Override
    public Set<String> getAllowedResources(String resourceType, String permission, String subjectType, Object subjectId) {
        final PermissionService.LookupResourcesRequest lookupResourcesRequest = PermissionService.LookupResourcesRequest.newBuilder()
                .setConsistency(PermissionService.Consistency.newBuilder().setMinimizeLatency(true).build())
                .setResourceObjectType(resourceType)
                .setSubject(Core.SubjectReference.newBuilder().setObject(Core.ObjectReference.newBuilder().setObjectType(subjectType).setObjectId(subjectId.toString()).build()).build())
                .setPermission(permission)
                .build();
        final Set<String> resources = new HashSet<>();
        final Iterator<PermissionService.LookupResourcesResponse> responseIterator = permissionsService.lookupResources(lookupResourcesRequest);
        while (responseIterator.hasNext()) {
            resources.add(responseIterator.next().getResourceObjectId());
        }
        return resources;
    }

    @Override
    public Set<String> getAllowedResources(Authentication authentication, String resourceType, String permission) {
        return getAllowedResources(resourceType, permission, userSubjectType, authentication.getName());
    }

    @Override
    public Set<String> getAllowedResources(String resourceType, String permission) {
        return getAllowedResources(SecurityContextHolder.getContext().getAuthentication(), resourceType, permission);
    }
}
