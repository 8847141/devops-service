package io.choerodon.devops.api.vo;

import org.hzero.starter.keyencrypt.core.Encrypt;

import io.choerodon.devops.infra.constant.EncryptKeyConstants;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  15:37 2019/4/12
 * Description:
 */
public class PipelineUserRecordRelationshipVO {

    @Encrypt(EncryptKeyConstants.IAM_USER_ENCRYPT_KEY)
    private Long userId;
    @Encrypt(EncryptKeyConstants.DEVOPS_PIPELINE_RECORD_ENCRYPT_KEY)
    private Long pipelineRecordId;
    @Encrypt(EncryptKeyConstants.DEVOPS_PIPELINE_STAGE_RECORD_ENCRYPT_KEY)
    private Long stageRecordId;
    @Encrypt(EncryptKeyConstants.DEVOPS_PIPELINE_TASK_RECORD_ENCRYPT_KEY)
    private Long taskRecordId;
    private String type;
    private Boolean isApprove;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPipelineRecordId() {
        return pipelineRecordId;
    }

    public void setPipelineRecordId(Long pipelineRecordId) {
        this.pipelineRecordId = pipelineRecordId;
    }

    public Long getStageRecordId() {
        return stageRecordId;
    }

    public void setStageRecordId(Long stageRecordId) {
        this.stageRecordId = stageRecordId;
    }

    public Long getTaskRecordId() {
        return taskRecordId;
    }

    public void setTaskRecordId(Long taskRecordId) {
        this.taskRecordId = taskRecordId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsApprove() {
        return isApprove;
    }

    public void setIsApprove(Boolean isApprove) {
        this.isApprove = isApprove;
    }
}
