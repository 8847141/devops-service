package io.choerodon.devops.domain.service;

import java.util.List;

import io.choerodon.core.convertor.ApplicationContextHelper;
import io.choerodon.devops.api.dto.gitlab.MemberDTO;
import io.choerodon.devops.domain.application.entity.UserAttrE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabMemberE;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.infra.common.util.TypeUtil;

/**
 * Created by n!Ck
 * Date: 2018/11/21
 * Time: 16:07
 * Description:
 */
public abstract class UpdateUserPermissionService {
    private GitlabProjectRepository gitlabProjectRepository;
    private GitlabRepository gitlabRepository;
    private GitlabGroupMemberRepository gitlabGroupMemberRepository;
    private DevopsEnvironmentRepository environmentRepository;
    private ApplicationRepository applicationRepository;
    private UserAttrRepository userAttrRepository;

    protected UpdateUserPermissionService() {
        this.gitlabProjectRepository = ApplicationContextHelper.getSpringFactory()
                .getBean(GitlabProjectRepository.class);
        this.gitlabRepository = ApplicationContextHelper.getSpringFactory().getBean(GitlabRepository.class);
        this.gitlabGroupMemberRepository = ApplicationContextHelper.getSpringFactory().getBean(GitlabGroupMemberRepository.class);
        this.environmentRepository = ApplicationContextHelper.getSpringFactory().getBean(DevopsEnvironmentRepository.class);
        this.applicationRepository = ApplicationContextHelper.getSpringFactory().getBean(ApplicationRepository.class);
        this.userAttrRepository = ApplicationContextHelper.getSpringFactory().getBean(UserAttrRepository.class);
    }

    public abstract Boolean updateUserPermission(Long projectId, Long id, List<Long> userIds, Integer option);

    protected void updateGitlabUserPermission(String type, Integer gitlabGroupId, Integer gitlabProjectId, List<Integer> addGitlabUserIds,
                                              List<Integer> deleteGitlabUserIds) {
        addGitlabUserIds.forEach(e -> {
            GitlabMemberE gitlabGroupMemberE = gitlabGroupMemberRepository.getUserMemberByUserId(gitlabGroupId, TypeUtil.objToInteger(e));
            if (gitlabGroupMemberE != null) {
                gitlabGroupMemberRepository.deleteMember(gitlabGroupId, TypeUtil.objToInteger(e));
                UserAttrE userAttrE = userAttrRepository.queryByGitlabUserId(TypeUtil.objToLong(e));
                List<Long> gitlabProjectIds = type.equals("env") ?
                        environmentRepository.listGitlabProjectIdByEnvPermission(TypeUtil.objToLong(gitlabGroupId), userAttrE.getIamUserId())
                        : applicationRepository.listGitlabProjectIdByAppPermission(TypeUtil.objToLong(gitlabGroupId), userAttrE.getIamUserId());
                if (gitlabProjectIds != null && !gitlabProjectIds.isEmpty()) {
                    gitlabProjectIds.forEach(aLong -> addGitlabMember(type, TypeUtil.objToInteger(aLong), TypeUtil.objToInteger(userAttrE.getGitlabUserId())));
                }

            }
            addGitlabMember(type, TypeUtil.objToInteger(gitlabProjectId), e);
        });
        deleteGitlabUserIds.forEach(e -> deleteGitlabMember(TypeUtil.objToInteger(gitlabProjectId), e));
    }

    private void addGitlabMember(String type, Integer gitlabProjectId, Integer userId) {
        GitlabMemberE gitlabMemberE = gitlabProjectRepository.getProjectMember(gitlabProjectId, userId);
        if (gitlabMemberE != null && gitlabMemberE.getId() == null) {
            MemberDTO memberDTO = null;
            if (type.equals("env")) {
                memberDTO = new MemberDTO(userId, 40, "");
            } else {
                memberDTO = new MemberDTO(userId, 30, "");
            }
            gitlabRepository.addMemberIntoProject(gitlabProjectId, memberDTO);
        }
    }

    private void deleteGitlabMember(Integer gitlabProjectId, Integer userId) {
        GitlabMemberE gitlabMemberE = gitlabProjectRepository
                .getProjectMember(gitlabProjectId, userId);
        if (gitlabMemberE.getId() != null) {
            gitlabRepository.removeMemberFromProject(gitlabProjectId, userId);
        }
    }
}
