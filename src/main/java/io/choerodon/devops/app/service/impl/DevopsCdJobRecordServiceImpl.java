package io.choerodon.devops.app.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.app.service.DevopsCdAuditRecordService;
import io.choerodon.devops.app.service.DevopsCdJobRecordService;
import io.choerodon.devops.app.service.DevopsCdPipelineRecordService;
import io.choerodon.devops.app.service.DevopsCdStageRecordService;
import io.choerodon.devops.infra.constant.PipelineCheckConstant;
import io.choerodon.devops.infra.dto.DevopsCdJobRecordDTO;
import io.choerodon.devops.infra.enums.JobTypeEnum;
import io.choerodon.devops.infra.enums.PipelineStatus;
import io.choerodon.devops.infra.enums.WorkFlowStatus;
import io.choerodon.devops.infra.mapper.DevopsCdJobRecordMapper;

/**
 * 〈功能简述〉
 * 〈〉
 *
 * @author wanghao
 * @since 2020/7/3 13:47
 */
@Service
public class DevopsCdJobRecordServiceImpl implements DevopsCdJobRecordService {

    private static final String ERROR_SAVE_JOB_RECORD_FAILED = "error.save.job.record.failed";
    private static final String ERROR_UPDATE_JOB_RECORD_FAILED = "error.update.job.record.failed";

    @Autowired
    private DevopsCdJobRecordMapper devopsCdJobRecordMapper;
    @Autowired
    @Lazy
    private DevopsCdStageRecordService devopsCdStageRecordService;
    @Autowired
    @Lazy
    private DevopsCdPipelineRecordService devopsCdPipelineRecordService;
    @Autowired
    private DevopsCdAuditRecordService devopsCdAuditRecordService;

    @Override
    public List<DevopsCdJobRecordDTO> queryByStageRecordId(Long stageRecordId) {
        DevopsCdJobRecordDTO jobRecordDTO = new DevopsCdJobRecordDTO();
        jobRecordDTO.setStageRecordId(stageRecordId);
        return devopsCdJobRecordMapper.select(jobRecordDTO);
    }

    @Override
    @Transactional
    public void save(DevopsCdJobRecordDTO devopsCdJobRecordDTO) {
        if (devopsCdJobRecordMapper.insert(devopsCdJobRecordDTO) != 1) {
            throw new CommonException(ERROR_SAVE_JOB_RECORD_FAILED);
        }
    }

    @Override
    public DevopsCdJobRecordDTO queryFirstByStageRecordId(Long stageRecordId) {
        return devopsCdJobRecordMapper.queryFirstByStageRecordId(stageRecordId);
    }

    @Override
    @Transactional
    public void update(DevopsCdJobRecordDTO devopsCdJobRecordDTO) {
        if (devopsCdJobRecordMapper.updateByPrimaryKeySelective(devopsCdJobRecordDTO) != 1) {
            throw new CommonException(ERROR_UPDATE_JOB_RECORD_FAILED);
        }
    }

    @Override
    public void deleteByStageRecordId(Long projectId) {

    }

    @Override
    public void updateStatusById(Long jobRecordId, String status) {
        DevopsCdJobRecordDTO cdJobRecordDTO = devopsCdJobRecordMapper.selectByPrimaryKey(jobRecordId);
        cdJobRecordDTO.setStatus(status);
        if (status.equals(WorkFlowStatus.FAILED.toValue())
                || status.equals(WorkFlowStatus.SUCCESS.toValue())
                || status.equals(WorkFlowStatus.STOP.toValue())) {
            cdJobRecordDTO.setFinishedDate(new Date());
        }
        if (status.equals(WorkFlowStatus.RUNNING.toValue())) {
            cdJobRecordDTO.setStartedDate(new Date());
        }
        if (devopsCdJobRecordMapper.updateByPrimaryKey(cdJobRecordDTO) != 1) {
            throw new CommonException(ERROR_UPDATE_JOB_RECORD_FAILED);
        }
    }

    @Override
    public DevopsCdJobRecordDTO queryById(Long id) {
        Assert.notNull(id, PipelineCheckConstant.ERROR_JOB_RECORD_ID_IS_NULL);
        return devopsCdJobRecordMapper.selectByPrimaryKey(id);
    }

    @Override
    @Transactional
    public void updateJobStatusFailed(Long jobRecordId) {
        DevopsCdJobRecordDTO devopsCdJobRecordDTO = queryById(jobRecordId);
        devopsCdJobRecordDTO.setStatus(PipelineStatus.FAILED.toValue());
        devopsCdJobRecordDTO.setFinishedDate(new Date());
        devopsCdJobRecordDTO.setDurationSeconds((new Date().getTime() - devopsCdJobRecordDTO.getStartedDate().getTime()) / 1000);
        update(devopsCdJobRecordDTO);
    }

    @Override
    public void updateJobStatusNotAudit(Long pipelineRecordId, Long stageRecordId, Long jobRecordId) {
        DevopsCdJobRecordDTO devopsCdJobRecordDTO = queryById(jobRecordId);
        // 更新job状态为待审核
        devopsCdJobRecordDTO.setStartedDate(new Date());
        devopsCdJobRecordDTO.setStatus(PipelineStatus.NOT_AUDIT.toValue());
        update(devopsCdJobRecordDTO);
        // 更新阶段状态为待审核
        devopsCdStageRecordService.updateStatusById(stageRecordId, PipelineStatus.NOT_AUDIT.toValue());
        // 同时更新流水线状态为待审核
        devopsCdPipelineRecordService.updateStatusById(pipelineRecordId, PipelineStatus.NOT_AUDIT.toValue());
        // 通知审核人员
        devopsCdAuditRecordService.sendJobAuditMessage(pipelineRecordId, devopsCdJobRecordDTO);
    }

    @Override
    public void retryCdJob(Long projectId, Long pipelineRecordId, Long stageRecordId, Long jobRecordId) {
        DevopsCdJobRecordDTO devopsCdJobRecordDTO = devopsCdJobRecordMapper.selectByPrimaryKey(jobRecordId);
        if (JobTypeEnum.CD_DEPLOY.value().equals(devopsCdJobRecordDTO.getType())) {
            // 更新流水线状态为执行中
            // 更新阶段状态为执行中
            // 更新任务状态为执行中

        } else if (JobTypeEnum.CD_HOST.value().equals(devopsCdJobRecordDTO.getType())) {

        }

    }

    @Override
    public void updateJobStatusSuccess(Long jobRecordId) {
        DevopsCdJobRecordDTO devopsCdJobRecordDTO = queryById(jobRecordId);
        devopsCdJobRecordDTO.setStatus(PipelineStatus.SUCCESS.toValue());
        devopsCdJobRecordDTO.setFinishedDate(new Date());
        devopsCdJobRecordDTO.setDurationSeconds((new Date().getTime() - devopsCdJobRecordDTO.getStartedDate().getTime()) / 1000);
        update(devopsCdJobRecordDTO);
    }


}
