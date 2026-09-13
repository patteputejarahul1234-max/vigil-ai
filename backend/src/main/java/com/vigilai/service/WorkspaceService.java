@Transactional
public WorkspaceResponse create(Long ownerId, WorkspaceRequest request) {
    if (ownerId == null) {
        throw ApiException.badRequest("User ID (ownerId) cannot be null when creating a workspace.");
    }

    Workspace workspace = Workspace.builder()
            .name(request.getName())
            .description(request.getDescription())
            .userId(ownerId)
            .ownerId(ownerId)
            .build();
    workspace = workspaceRepository.save(workspace);

    memberRepository.save(WorkspaceMember.builder()
            .workspaceId(workspace.getId())
            .userId(ownerId)
            .role(WorkspaceRole.OWNER)
            .build());

    try {
        activityLogService.log(workspace.getId(), ownerId, "WORKSPACE_CREATED", "WORKSPACE", workspace.getId(),
                "Created workspace \"" + workspace.getName() + "\"");
    } catch (Exception ignored) {}

    return toResponse(workspace);
}