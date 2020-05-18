package io.choerodon.devops.app.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.devops.app.service.DevopsCiPipelineRecordService;
import io.choerodon.devops.app.service.ProjectPipelineService;
import io.choerodon.devops.app.service.UserAttrService;
import io.choerodon.devops.infra.dto.UserAttrDTO;
import io.choerodon.devops.infra.feign.operator.GitlabServiceClientOperator;
import io.choerodon.devops.infra.util.GitUserNameUtil;
import io.choerodon.devops.infra.util.TypeUtil;

/**
 * Created by Zenger on 2018/4/10.
 */
@Service
public class ProjectPipelineServiceImpl implements ProjectPipelineService {
    @Value("${services.gitlab.url}")
    private String gitlabUrl;
    @Autowired
    private GitlabServiceClientOperator gitlabServiceClientOperator;
    @Autowired
    private UserAttrService userAttrService;
    @Autowired
    private DevopsCiPipelineRecordService devopsCiPipelineRecordService;


    public Integer getGitlabUserId() {
        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        return TypeUtil.objToInteger(userAttrDTO.getGitlabUserId());
    }


    @Override
    public Boolean retry(Long gitlabProjectId, Long pipelineId) {
        return gitlabServiceClientOperator.retryPipeline(gitlabProjectId.intValue(),
                pipelineId.intValue(), getGitlabUserId()) != null;
    }

    @Override
    public Boolean cancel(Long gitlabProjectId, Long pipelineId) {
        return gitlabServiceClientOperator.cancelPipeline(gitlabProjectId.intValue(),
                pipelineId.intValue(), getGitlabUserId()) != null;
    }

    @Override
    public Boolean create(Long gitlabProjectId, String ref) {
        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(DetailsHelper.getUserDetails().getUserId());
        return gitlabServiceClientOperator.createPipeline(gitlabProjectId.intValue(), userAttrDTO.getGitlabUserId().intValue(), ref) != null;
    }
}
