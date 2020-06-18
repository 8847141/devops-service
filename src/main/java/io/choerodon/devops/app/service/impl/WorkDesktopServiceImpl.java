package io.choerodon.devops.app.service.impl;

import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.devops.api.vo.ApprovalVO;
import io.choerodon.devops.api.vo.LatestAppServiceVO;
import io.choerodon.devops.api.vo.UserAttrVO;
import io.choerodon.devops.app.service.DevopsGitService;
import io.choerodon.devops.app.service.UserAttrService;
import io.choerodon.devops.app.service.WorkDesktopService;
import io.choerodon.devops.infra.dto.AppServiceDTO;
import io.choerodon.devops.infra.dto.DevopsMergeRequestDTO;
import io.choerodon.devops.infra.dto.PipelineRecordDTO;
import io.choerodon.devops.infra.dto.iam.IamUserDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.dto.iam.Tenant;
import io.choerodon.devops.infra.enums.ApprovalTypeEnum;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class WorkDesktopServiceImpl implements WorkDesktopService {
    private static final String MERGE_REQUEST_CONTENT_FORMAT = "%s (%s)在应用服务“%s”中提交了合并请求";
    private static final String PIPELINE_CONTENT_FORMAT = "流水线 “%s” 目前暂停于【%s】阶段，需要您进行审核";
    private static final String MERGE_REQUEST_URL = "%s/%s-%s/%s/merge_requests/%d";
    @Autowired
    DevopsGitService devopsGitService;
    @Value("${services.gitlab.url}")
    private String gitlabUrl;
    @Autowired
    private DevopsMergeRequestMapper devopsMergeRequestMapper;
    @Autowired
    private AppServiceMapper appServiceMapper;
    @Autowired
    private DevopsBranchMapper devopsBranchMapper;
    @Autowired
    private DevopsGitlabCommitMapper devopsGitlabCommitMapper;
    @Autowired
    private PipelineStageRecordMapper pipelineStageRecordMapper;
    @Autowired
    private UserAttrService userAttrService;
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @Override
    public List<LatestAppServiceVO> listLatestAppService(Long organizationId, Long projectId) {
        Tenant tenant = baseServiceClientOperator.queryOrganizationById(organizationId);
        List<ProjectDTO> projectDTOList;
        if (projectId == null) {
            projectDTOList = baseServiceClientOperator.listIamProjectByOrgId(tenant.getTenantId());
        } else {
            projectDTOList = Collections.singletonList(baseServiceClientOperator.queryIamProjectById(projectId));
        }
        return listLatestUserAppServiceDTO(projectDTOList);
    }

    @Override
    public List<ApprovalVO> listApproval(Long organizationId, Long projectId) {
        Tenant tenant = baseServiceClientOperator.queryOrganizationById(organizationId);
        List<ProjectDTO> projectDTOList;
        if (projectId == null) {
            projectDTOList = baseServiceClientOperator.listIamProjectByOrgId(tenant.getTenantId());
        } else {
            projectDTOList = Collections.singletonList(baseServiceClientOperator.queryIamProjectById(projectId));
        }
        return listApprovalVOByProject(tenant, projectDTOList);
    }

    private List<ApprovalVO> listApprovalVOByProject(Tenant tenant, List<ProjectDTO> projectDTOList) {
        List<ApprovalVO> approvalVOList = new ArrayList<>();
        List<Long> projectIds = projectDTOList.stream().map(ProjectDTO::getId).collect(Collectors.toList());
        Map<Long, ProjectDTO> projectMap = projectDTOList.stream().collect(Collectors.toMap(ProjectDTO::getId, v -> v));
        // 1.查询合并请求
        List<AppServiceDTO> appServiceDTOList = appServiceMapper.listByActiveAndProjects(projectIds);
        approvalVOList.addAll(listMergeRequestApproval(tenant, projectMap, appServiceDTOList));

        // 2.查出流水线请求
        approvalVOList.addAll(listPipelineApproval(projectMap, projectIds));
        return approvalVOList;
    }

    private List<ApprovalVO> listMergeRequestApproval(Tenant tenant, Map<Long, ProjectDTO> projectMap, List<AppServiceDTO> appServiceDTOList) {
        List<ApprovalVO> approvalVOList = new ArrayList<>();
        List<Integer> gitlabProjectIds = appServiceDTOList.stream().map(AppServiceDTO::getGitlabProjectId).collect(Collectors.toList());
        Map<Integer, AppServiceDTO> gitlabProjectAndAppMap = appServiceDTOList.stream().collect(Collectors.toMap(AppServiceDTO::getGitlabProjectId, v -> v));
        // 查出该用户待审批的合并请求
        List<DevopsMergeRequestDTO> mergeRequestDTOList = new ArrayList<>();
        if (gitlabProjectIds.size() != 0) {
            mergeRequestDTOList = devopsMergeRequestMapper.listToBeAuditedByThisUserUnderProjectIds(gitlabProjectIds, DetailsHelper.getUserDetails() == null ? 0L : DetailsHelper.getUserDetails().getUserId());
        }
        // 根据authorId查出合并请求发起者信息
        Set<Long> authorIds = mergeRequestDTOList.stream().map(DevopsMergeRequestDTO::getAuthorId).collect(Collectors.toSet());
        List<UserAttrVO> userAttrDTOList = userAttrService.listUsersByGitlabUserIds(authorIds);
        List<Long> iamUserIds = userAttrDTOList.stream().map(UserAttrVO::getIamUserId).collect(Collectors.toList());
        Map<Long, IamUserDTO> iamUserDTOMap = baseServiceClientOperator.queryUsersByUserIds(iamUserIds).stream().collect(Collectors.toMap(IamUserDTO::getId, v -> v));
        Map<Long, UserAttrVO> userAttrVO = userAttrDTOList.stream().collect(Collectors.toMap(UserAttrVO::getGitlabUserId, v -> v));
        mergeRequestDTOList.forEach(devopsMergeRequestDTO -> {
            IamUserDTO iamUserDTO = iamUserDTOMap.get(userAttrVO.get(devopsMergeRequestDTO.getAuthorId()).getIamUserId());
            ApprovalVO approvalVO = new ApprovalVO()
                    .setImageUrl(iamUserDTO.getImageUrl())
                    .setType(ApprovalTypeEnum.MERGE_REQUEST.getType())
                    .setMergeRequestUrl(String.format(MERGE_REQUEST_URL, gitlabUrl, tenant.getTenantNum(), projectMap.get(devopsMergeRequestDTO.getProjectId()).getCode(), gitlabProjectAndAppMap.get(devopsMergeRequestDTO.getGitlabProjectId().intValue()).getCode(), devopsMergeRequestDTO.getGitlabMergeRequestId()))
                    .setProjectId(devopsMergeRequestDTO.getProjectId())
                    .setProjectName(projectMap.get(devopsMergeRequestDTO.getProjectId()).getName())
                    .setGitlabProjectId(devopsMergeRequestDTO.getGitlabProjectId().intValue())
                    .setContent(String.format(MERGE_REQUEST_CONTENT_FORMAT, iamUserDTO.getRealName(), iamUserDTO.getLoginName(), gitlabProjectAndAppMap.get(devopsMergeRequestDTO.getGitlabProjectId().intValue()).getName()));
            approvalVOList.add(approvalVO);
        });

        return approvalVOList;
    }

    private List<ApprovalVO> listPipelineApproval(Map<Long, ProjectDTO> projectNameMap, List<Long> projectIds) {
        List<ApprovalVO> approvalVOList = new ArrayList<>();

        Long userId = DetailsHelper.getUserDetails().getUserId() == null ? 0 : DetailsHelper.getUserDetails().getUserId();
        // 查出该用户待审批的流水线阶段
        List<PipelineRecordDTO> pipelineRecordDTOList = pipelineStageRecordMapper.listToBeAuditedByProjectIds(projectIds, userId);
        List<PipelineRecordDTO> pipelineRecordDTOAuditByThisUserList = pipelineRecordDTOList.stream()
                .filter(pipelineRecordDTO -> pipelineRecordDTO.getAuditUser() != null && pipelineRecordDTO.getAuditUser().contains(String.valueOf(userId)))
                .collect(Collectors.toList());
        pipelineRecordDTOAuditByThisUserList.forEach(pipelineRecordDTO -> {
            ApprovalVO approvalVO = new ApprovalVO()
                    .setType(ApprovalTypeEnum.PIPE_LINE.getType())
                    .setProjectId(pipelineRecordDTO.getProjectId())
                    .setProjectName(projectNameMap.get(pipelineRecordDTO.getProjectId()).getName())
                    .setContent(String.format(PIPELINE_CONTENT_FORMAT, pipelineRecordDTO.getPipelineName(), pipelineRecordDTO.getStageName()))
                    .setPipelineId(pipelineRecordDTO.getPipelineId())
                    .setPipelineRecordId(pipelineRecordDTO.getId())
                    .setStageRecordId(pipelineRecordDTO.getStageRecordId())
                    .setTaskRecordId(pipelineRecordDTO.getTaskRecordId());
            approvalVOList.add(approvalVO);
        });
        return approvalVOList;
    }

    private List<LatestAppServiceVO> listLatestUserAppServiceDTO(List<ProjectDTO> projectDTOList) {
        List<Long> projectIds = projectDTOList.stream().map(ProjectDTO::getId).collect(Collectors.toList());
        Map<Long, ProjectDTO> projectDTOMap = projectDTOList.stream().collect(Collectors.toMap(ProjectDTO::getId, v -> v));
        Long userId = DetailsHelper.getUserDetails().getUserId() == null ? 0 : DetailsHelper.getUserDetails().getUserId();
        List<LatestAppServiceVO> latestAppServiceVOList = new ArrayList<>();
        latestAppServiceVOList.addAll(appServiceMapper.listLatestUseAppServiceIdAndDate(projectIds, userId));
        latestAppServiceVOList.addAll(devopsBranchMapper.listLatestUseAppServiceIdAndDate(projectIds, userId));
        latestAppServiceVOList.addAll(devopsGitlabCommitMapper.listLatestUseAppServiceIdAndDate(projectIds, userId));
        latestAppServiceVOList.addAll(devopsMergeRequestMapper.listLatestUseAppServiceIdAndDate(projectIds, userId));

        // 去掉重复的appService,只保留最近使用的
        List<LatestAppServiceVO> latestAppServiceVOListWithoutRepeatService = latestAppServiceVOList.stream().sorted(Comparator.comparing(LatestAppServiceVO::getLastUpdateDate).reversed())
                .filter(distinctByKey(LatestAppServiceVO::getLastUpdateDate))
                .collect(Collectors.toList());

        int end = Math.min(latestAppServiceVOListWithoutRepeatService.size(), 10);

        List<LatestAppServiceVO> latestTenAppServiceList = latestAppServiceVOListWithoutRepeatService.subList(0, end);

        Set<Long> appServiceIds = latestTenAppServiceList.stream().map(LatestAppServiceVO::getId).collect(Collectors.toSet());
        Map<Long, AppServiceDTO> appServiceDTOMap = appServiceMapper.listAppServiceByIds(appServiceIds, null, null).stream().collect(Collectors.toMap(AppServiceDTO::getId, v -> v));

        latestTenAppServiceList.forEach(latestAppServiceVO -> {
            AppServiceDTO appServiceDTO = appServiceDTOMap.get(latestAppServiceVO.getId());
            ProjectDTO projectDTO = projectDTOMap.get(appServiceDTO.getProjectId());
            latestAppServiceVO.setProjectName(projectDTO.getName())
                    .setProjectId(appServiceDTO.getProjectId())
                    .setCode(appServiceDTO.getCode())
                    .setName(appServiceDTO.getName());
        });
        return latestTenAppServiceList;
    }

}
