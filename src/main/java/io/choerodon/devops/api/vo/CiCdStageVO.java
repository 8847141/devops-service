package io.choerodon.devops.api.vo;

import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class CiCdStageVO {
    private Long id;
    @ApiModelProperty("阶段名称")
    @NotEmpty(message = "error.stage.name.cannot.be.null")
    private String name;
    @ApiModelProperty("阶段所属流水线id")
    private Long ciCdPipelineId;
    @ApiModelProperty("阶段顺序")
    @NotNull(message = "error.stage.sequence.cannot.be.null")
    private Long sequence;
    private List<CiCdJobVO> jobList;
    private Long projectId;
    private Long objectVersionNumber;
    private Date lastUpdateDate;
    private String triggerType;
    private List<Long> cdAuditUserIds;
    private Boolean parallel;
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCiCdPipelineId() {
        return ciCdPipelineId;
    }

    public void setCiCdPipelineId(Long ciCdPipelineId) {
        this.ciCdPipelineId = ciCdPipelineId;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public List<CiCdJobVO> getJobList() {
        return jobList;
    }

    public void setJobList(List<CiCdJobVO> jobList) {
        this.jobList = jobList;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getObjectVersionNumber() {
        return objectVersionNumber;
    }

    public void setObjectVersionNumber(Long objectVersionNumber) {
        this.objectVersionNumber = objectVersionNumber;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public List<Long> getCdAuditUserIds() {
        return cdAuditUserIds;
    }

    public void setCdAuditUserIds(List<Long> cdAuditUserIds) {
        this.cdAuditUserIds = cdAuditUserIds;
    }

    public Boolean getParallel() {
        return parallel;
    }

    public void setParallel(Boolean parallel) {
        this.parallel = parallel;
    }
}
