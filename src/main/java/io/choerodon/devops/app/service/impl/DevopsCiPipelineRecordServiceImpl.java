package io.choerodon.devops.app.service.impl;

import static io.choerodon.devops.app.eventhandler.constants.SagaTopicCodeConstants.DEVOPS_GITLAB_CI_PIPELINE;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import io.choerodon.asgard.saga.annotation.Saga;
import io.choerodon.asgard.saga.producer.StartSagaBuilder;
import io.choerodon.asgard.saga.producer.TransactionalProducer;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.devops.api.vo.*;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.dto.*;
import io.choerodon.devops.infra.dto.gitlab.JobDTO;
import io.choerodon.devops.infra.dto.gitlab.ci.Pipeline;
import io.choerodon.devops.infra.dto.gitlab.ci.PipelineStatus;
import io.choerodon.devops.infra.dto.iam.IamUserDTO;
import io.choerodon.devops.infra.enums.CiJobTypeEnum;
import io.choerodon.devops.infra.enums.JobStatusEnum;
import io.choerodon.devops.infra.enums.SonarQubeType;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.feign.operator.GitlabServiceClientOperator;
import io.choerodon.devops.infra.mapper.DevopsCiJobArtifactRecordMapper;
import io.choerodon.devops.infra.mapper.DevopsCiJobRecordMapper;
import io.choerodon.devops.infra.mapper.DevopsCiPipelineRecordMapper;
import io.choerodon.devops.infra.util.ConvertUtils;
import io.choerodon.devops.infra.util.CustomContextUtil;
import io.choerodon.devops.infra.util.PageRequestUtil;
import io.choerodon.devops.infra.util.TypeUtil;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * 〈功能简述〉
 * 〈〉
 *
 * @author wanghao
 * @since 2020/4/3 9:26
 */
@Service
public class DevopsCiPipelineRecordServiceImpl implements DevopsCiPipelineRecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevopsCiPipelineRecordServiceImpl.class);


    private static final String ERROR_PIPELINE_ID_IS_NULL = "error.pipeline.id.is.null";
    private static final String ERROR_GITLAB_PIPELINE_ID_IS_NULL = "error.gitlab.pipeline.id.is.null";
    private static final String ERROR_GITLAB_PROJECT_ID_IS_NULL = "error.gitlab.project.id.is.null";

    private DevopsCiPipelineRecordMapper devopsCiPipelineRecordMapper;
    private DevopsCiJobRecordService devopsCiJobRecordService;
    private DevopsCiStageService devopsCiStageService;
    private DevopsCiJobService devopsCiJobService;
    private DevopsCiJobRecordMapper devopsCiJobRecordMapper;
    private DevopsCiPipelineService devopsCiPipelineService;
    private AppServiceService applicationService;
    private TransactionalProducer transactionalProducer;
    private UserAttrService userAttrService;
    private BaseServiceClientOperator baseServiceClientOperator;
    private GitlabServiceClientOperator gitlabServiceClientOperator;
    private DevopsGitlabCommitService devopsGitlabCommitService;
    private DevopsCiJobArtifactRecordMapper devopsCiJobArtifactRecordMapper;

    private ObjectMapper objectMapper = new ObjectMapper();

    // @lazy解决循环依赖
    public DevopsCiPipelineRecordServiceImpl(DevopsCiPipelineRecordMapper devopsCiPipelineRecordMapper,
                                             DevopsCiJobRecordService devopsCiJobRecordService,
                                             DevopsCiStageService devopsCiStageService,
                                             @Lazy DevopsCiJobService devopsCiJobService,
                                             DevopsCiJobRecordMapper devopsCiJobRecordMapper,
                                             @Lazy DevopsCiPipelineService devopsCiPipelineService,
                                             AppServiceService applicationService,
                                             TransactionalProducer transactionalProducer,
                                             UserAttrService userAttrService,
                                             BaseServiceClientOperator baseServiceClientOperator,
                                             GitlabServiceClientOperator gitlabServiceClientOperator,
                                             DevopsCiJobArtifactRecordMapper devopsCiJobArtifactRecordMapper,
                                             DevopsGitlabCommitService devopsGitlabCommitService) {
        this.devopsCiPipelineRecordMapper = devopsCiPipelineRecordMapper;
        this.devopsCiJobRecordService = devopsCiJobRecordService;
        this.devopsCiStageService = devopsCiStageService;
        this.devopsCiJobService = devopsCiJobService;
        this.devopsCiJobRecordMapper = devopsCiJobRecordMapper;
        this.devopsCiPipelineService = devopsCiPipelineService;
        this.applicationService = applicationService;
        this.transactionalProducer = transactionalProducer;
        this.userAttrService = userAttrService;
        this.baseServiceClientOperator = baseServiceClientOperator;
        this.gitlabServiceClientOperator = gitlabServiceClientOperator;
        this.devopsCiJobArtifactRecordMapper = devopsCiJobArtifactRecordMapper;
        this.devopsGitlabCommitService = devopsGitlabCommitService;
    }

    @Override
    @Saga(code = DEVOPS_GITLAB_CI_PIPELINE, description = "gitlab ci pipeline创建到数据库", inputSchemaClass = PipelineWebHookVO.class)
    public void create(PipelineWebHookVO pipelineWebHookVO, String token) {
        AppServiceDTO appServiceDTO = applicationService.baseQueryByToken(token);
        DevopsCiPipelineDTO devopsCiPipelineDTO = devopsCiPipelineService.queryByAppSvcId(appServiceDTO.getId());
        if (devopsCiPipelineDTO == null || Boolean.FALSE.equals(devopsCiPipelineDTO.getEnabled())) {
            return;
        }
        List<DevopsCiStageDTO> devopsCiStageDTOList = devopsCiStageService.listByPipelineId(devopsCiPipelineDTO.getId());
        List<DevopsCiJobDTO> devopsCiJobDTOS = devopsCiJobService.listByPipelineId(devopsCiPipelineDTO.getId());
        Map<Long, DevopsCiStageDTO> stageMap = devopsCiStageDTOList.stream().collect(Collectors.toMap(DevopsCiStageDTO::getId, v -> v));
        Map<String, DevopsCiJobDTO> jobMap = devopsCiJobDTOS.stream().collect(Collectors.toMap(DevopsCiJobDTO::getName, v -> v));
        // 检验是否是手动修改gitlab-ci.yaml文件生成的流水线记录
        for (CiJobWebHookVO job : pipelineWebHookVO.getBuilds()) {
            DevopsCiJobDTO devopsCiJobDTO = jobMap.get(job.getName());
            if (devopsCiJobDTO == null) {
                return;
            } else {
                DevopsCiStageDTO devopsCiStageDTO = stageMap.get(devopsCiJobDTO.getCiStageId());
                if (devopsCiStageDTO == null || !devopsCiStageDTO.getName().equals(job.getStage())) {
                    return;
                } else {
                    job.setType(devopsCiJobDTO.getType());
                }
            }
        }
        pipelineWebHookVO.setToken(token);
        try {
            String input = objectMapper.writeValueAsString(pipelineWebHookVO);
            transactionalProducer.apply(
                    StartSagaBuilder.newBuilder()
                            .withRefType("app")
                            .withRefId(appServiceDTO.getId().toString())
                            .withSagaCode(DEVOPS_GITLAB_CI_PIPELINE)
                            .withLevel(ResourceLevel.PROJECT)
                            .withSourceId(appServiceDTO.getProjectId())
                            .withJson(input),
                    builder -> {
                    });
        } catch (JsonProcessingException e) {
            throw new CommonException(e.getMessage(), e);
        }
    }

    @Override
    public void handleCreate(PipelineWebHookVO pipelineWebHookVO) {
        AppServiceDTO applicationDTO = applicationService.baseQueryByToken(pipelineWebHookVO.getToken());
        DevopsCiPipelineDTO devopsCiPipelineDTO = devopsCiPipelineService.queryByAppSvcId(applicationDTO.getId());

        DevopsCiPipelineRecordDTO record = new DevopsCiPipelineRecordDTO();
        record.setGitlabPipelineId(pipelineWebHookVO.getObjectAttributes().getId());
        DevopsCiPipelineRecordDTO devopsCiPipelineRecordDTO = devopsCiPipelineRecordMapper.selectOne(record);
        Long iamUserId = getIamUserIdByGitlabUserName(pipelineWebHookVO.getUser().getUsername());
        CustomContextUtil.setUserContext(iamUserId);

        //pipeline不存在则创建,存在则更新状态和阶段信息
        if (devopsCiPipelineRecordDTO == null) {
            devopsCiPipelineRecordDTO = new DevopsCiPipelineRecordDTO();
            devopsCiPipelineRecordDTO.setCiPipelineId(devopsCiPipelineDTO.getId());
            devopsCiPipelineRecordDTO.setGitlabPipelineId(pipelineWebHookVO.getObjectAttributes().getId());
            devopsCiPipelineRecordDTO.setTriggerUserId(iamUserId);
            devopsCiPipelineRecordDTO.setCommitSha(pipelineWebHookVO.getObjectAttributes().getSha());
            devopsCiPipelineRecordDTO.setCreatedDate(pipelineWebHookVO.getObjectAttributes().getCreatedAt());
            devopsCiPipelineRecordDTO.setFinishedDate(pipelineWebHookVO.getObjectAttributes().getFinishedAt());
            devopsCiPipelineRecordDTO.setDurationSeconds(pipelineWebHookVO.getObjectAttributes().getDuration());
            devopsCiPipelineRecordDTO.setStatus(pipelineWebHookVO.getObjectAttributes().getStatus());
            devopsCiPipelineRecordDTO.setGitlabProjectId(pipelineWebHookVO.getProject().getId());
            devopsCiPipelineRecordDTO.setGitlabTriggerRef(pipelineWebHookVO.getObjectAttributes().getRef());
            devopsCiPipelineRecordMapper.insertSelective(devopsCiPipelineRecordDTO);
            // 保存job执行记录
            Long pipelineRecordId = devopsCiPipelineRecordDTO.getId();
            saveJobRecords(pipelineWebHookVO, pipelineRecordId);
        } else {
            devopsCiPipelineRecordDTO.setGitlabPipelineId(pipelineWebHookVO.getObjectAttributes().getId());
            devopsCiPipelineRecordDTO.setTriggerUserId(iamUserId);
            devopsCiPipelineRecordDTO.setCommitSha(pipelineWebHookVO.getObjectAttributes().getSha());
            devopsCiPipelineRecordDTO.setCreatedDate(pipelineWebHookVO.getObjectAttributes().getCreatedAt());
            devopsCiPipelineRecordDTO.setFinishedDate(pipelineWebHookVO.getObjectAttributes().getFinishedAt());
            devopsCiPipelineRecordDTO.setDurationSeconds(pipelineWebHookVO.getObjectAttributes().getDuration());
            devopsCiPipelineRecordDTO.setStatus(pipelineWebHookVO.getObjectAttributes().getStatus());
            devopsCiPipelineRecordMapper.updateByPrimaryKeySelective(devopsCiPipelineRecordDTO);
            // 更新job状态
            // 保存job执行记录
            Long pipelineRecordId = devopsCiPipelineRecordDTO.getId();
            saveJobRecords(pipelineWebHookVO, pipelineRecordId);
        }
    }

    private void saveJobRecords(PipelineWebHookVO pipelineWebHookVO, Long pipelineRecordId) {
        pipelineWebHookVO.getBuilds().forEach(ciJobWebHookVO -> {
            DevopsCiJobRecordDTO devopsCiJobRecordDTO = devopsCiJobRecordService.queryByGitlabJobId(ciJobWebHookVO.getId());
            if (devopsCiJobRecordDTO == null) {
                devopsCiJobRecordDTO = new DevopsCiJobRecordDTO();
                devopsCiJobRecordDTO.setGitlabJobId(ciJobWebHookVO.getId());
                devopsCiJobRecordDTO.setCiPipelineRecordId(pipelineRecordId);
                devopsCiJobRecordDTO.setStartedDate(ciJobWebHookVO.getStartedAt());
                devopsCiJobRecordDTO.setFinishedDate(ciJobWebHookVO.getFinishedAt());
                devopsCiJobRecordDTO.setStage(ciJobWebHookVO.getStage());
                devopsCiJobRecordDTO.setType(ciJobWebHookVO.getType());
                devopsCiJobRecordDTO.setName(ciJobWebHookVO.getName());
                devopsCiJobRecordDTO.setStatus(ciJobWebHookVO.getStatus());
                devopsCiJobRecordDTO.setTriggerUserId(getIamUserIdByGitlabUserName(ciJobWebHookVO.getUser().getUsername()));
                devopsCiJobRecordDTO.setGitlabProjectId(pipelineWebHookVO.getProject().getId());
                devopsCiJobRecordMapper.insertSelective(devopsCiJobRecordDTO);
            } else {
                devopsCiJobRecordDTO.setCiPipelineRecordId(pipelineRecordId);
                devopsCiJobRecordDTO.setStartedDate(ciJobWebHookVO.getStartedAt());
                devopsCiJobRecordDTO.setFinishedDate(ciJobWebHookVO.getFinishedAt());
                devopsCiJobRecordDTO.setStatus(ciJobWebHookVO.getStatus());
                devopsCiJobRecordDTO.setTriggerUserId(getIamUserIdByGitlabUserName(ciJobWebHookVO.getUser().getUsername()));
                devopsCiJobRecordMapper.updateByPrimaryKeySelective(devopsCiJobRecordDTO);
            }
        });
    }

    @Override
    public Page<DevopsCiPipelineRecordVO> pagingPipelineRecord(Long projectId, Long ciPipelineId, PageRequest pageable) {
        Page<DevopsCiPipelineRecordVO> pipelineRecordInfo = PageHelper.doPageAndSort(PageRequestUtil.simpleConvertSortForPage(pageable), () -> devopsCiPipelineRecordMapper.listByCiPipelineId(ciPipelineId));
        List<DevopsCiPipelineRecordVO> pipelineRecordVOList = pipelineRecordInfo.getContent();
        if (CollectionUtils.isEmpty(pipelineRecordVOList)) {
            return pipelineRecordInfo;
        }
        pipelineRecordVOList.forEach(pipelineRecord -> {
            // 查询流水线记录下的job记录
            DevopsCiJobRecordDTO recordDTO = new DevopsCiJobRecordDTO();
            recordDTO.setCiPipelineRecordId(pipelineRecord.getId());
            List<DevopsCiJobRecordDTO> devopsCiJobRecordDTOS = devopsCiJobRecordMapper.select(recordDTO);

            // 只返回job的最新记录
            devopsCiJobRecordDTOS = filterJobs(devopsCiJobRecordDTOS);
            Map<String, List<DevopsCiJobRecordDTO>> jobRecordMap = devopsCiJobRecordDTOS.stream().collect(Collectors.groupingBy(DevopsCiJobRecordDTO::getStage));
            // 查询阶段信息
            List<DevopsCiStageDTO> devopsCiStageDTOList = devopsCiStageService.listByPipelineId(ciPipelineId);
            List<DevopsCiStageRecordVO> devopsCiStageRecordVOS = ConvertUtils.convertList(devopsCiStageDTOList, DevopsCiStageRecordVO.class);

            // 计算stage状态
            devopsCiStageRecordVOS.forEach(stageRecord -> {
                List<DevopsCiJobRecordDTO> ciJobRecordDTOS = jobRecordMap.get(stageRecord.getName());
                if (!CollectionUtils.isEmpty(ciJobRecordDTOS)) {
                    Map<String, List<DevopsCiJobRecordDTO>> statsuMap = ciJobRecordDTOS.stream().collect(Collectors.groupingBy(DevopsCiJobRecordDTO::getStatus));
                    calculateStageStatus(stageRecord, statsuMap);
                }

            });
            // stage排序
            devopsCiStageRecordVOS = devopsCiStageRecordVOS.stream().sorted(Comparator.comparing(DevopsCiStageRecordVO::getSequence)).filter(v -> v.getStatus() != null).collect(Collectors.toList());
            pipelineRecord.setStageRecordVOList(devopsCiStageRecordVOS);
        });
        return pipelineRecordInfo;
    }

    private List<DevopsCiJobRecordDTO> filterJobs(List<DevopsCiJobRecordDTO> devopsCiJobRecordDTOS) {
        List<DevopsCiJobRecordDTO> devopsCiJobRecordDTOList = new ArrayList<>();
        if (CollectionUtils.isEmpty(devopsCiJobRecordDTOS)) {
            return devopsCiJobRecordDTOList;
        }
        Map<String, List<DevopsCiJobRecordDTO>> jobMap = devopsCiJobRecordDTOS.stream().collect(Collectors.groupingBy(DevopsCiJobRecordDTO::getName));
        jobMap.forEach((k, v) -> {
            if (v.size() > 1) {
                Optional<DevopsCiJobRecordDTO> ciJobRecordDTO = v.stream().max(Comparator.comparing(DevopsCiJobRecordDTO::getId));
                devopsCiJobRecordDTOList.add(ciJobRecordDTO.get());
            } else if (v.size() == 1) {
                devopsCiJobRecordDTOList.add(v.get(0));
            }
        });
        return devopsCiJobRecordDTOList;
    }

    @Override
    public DevopsCiPipelineRecordVO queryPipelineRecordDetails(Long projectId, Long gitlabPipelineId) {
        DevopsCiPipelineRecordDTO pipelineRecordDTO = new DevopsCiPipelineRecordDTO();
        pipelineRecordDTO.setGitlabPipelineId(gitlabPipelineId);
        DevopsCiPipelineRecordDTO devopsCiPipelineRecordDTO = devopsCiPipelineRecordMapper.selectOne(pipelineRecordDTO);


        DevopsCiPipelineRecordVO devopsCiPipelineRecordVO = ConvertUtils.convertObject(devopsCiPipelineRecordDTO, DevopsCiPipelineRecordVO.class);
        IamUserDTO iamUserDTO = baseServiceClientOperator.queryUserByUserId(devopsCiPipelineRecordDTO.getTriggerUserId());
        devopsCiPipelineRecordVO.setUserDTO(iamUserDTO);

        // 添加提交信息
        addCommitInfo(devopsCiPipelineRecordVO, devopsCiPipelineRecordDTO);

        // 添加流水线信息
        DevopsCiPipelineVO ciPipelineVO = devopsCiPipelineService.queryById(devopsCiPipelineRecordDTO.getCiPipelineId());
        devopsCiPipelineRecordVO.setDevopsCiPipelineVO(ciPipelineVO);

        // 查询流水线记录下的job记录
        DevopsCiJobRecordDTO recordDTO = new DevopsCiJobRecordDTO();
        recordDTO.setCiPipelineRecordId(devopsCiPipelineRecordDTO.getId());
        List<DevopsCiJobRecordDTO> devopsCiJobRecordDTOS = devopsCiJobRecordMapper.select(recordDTO);

        // 只返回job的最新记录
        devopsCiJobRecordDTOS = filterJobs(devopsCiJobRecordDTOS);

        // 添加job type
        List<DevopsCiJobRecordVO> devopsCiJobRecordVOList = ConvertUtils.convertList(devopsCiJobRecordDTOS, this::convertJobRecord);

        //添加sonar
        for (DevopsCiJobRecordVO devopsCiJobRecordVO : devopsCiJobRecordVOList) {
            if (CiJobTypeEnum.SONAR.value().equals(devopsCiJobRecordVO.getType())) {
                SonarContentsVO sonarContentsVO = applicationService.getSonarContent(ciPipelineVO.getProjectId(), ciPipelineVO.getAppServiceId());
                if (!Objects.isNull(sonarContentsVO) && !CollectionUtils.isEmpty(sonarContentsVO.getSonarContents())) {
                    List<SonarContentVO> sonarContents = sonarContentsVO.getSonarContents();
                    List<SonarContentVO> sonarContentVOS = sonarContents.stream().filter(sonarContentVO -> {
                        return SonarQubeType.BUGS.getType().equals(sonarContentVO.getKey())
                                || SonarQubeType.CODE_SMELLS.getType().equals(sonarContentVO.getKey())
                                || SonarQubeType.VULNERABILITIES.getType().equals(sonarContentVO.getKey());
                    }).collect(Collectors.toList());
                    devopsCiJobRecordVO.setSonarContentVOS(sonarContentVOS);
                }
            }
        }

        Map<String, List<DevopsCiJobRecordVO>> jobRecordMap = devopsCiJobRecordVOList.stream().collect(Collectors.groupingBy(DevopsCiJobRecordVO::getStage));

        // 查询阶段信息
        List<DevopsCiStageDTO> devopsCiStageDTOList = devopsCiStageService.listByPipelineId(devopsCiPipelineRecordDTO.getCiPipelineId());
        List<DevopsCiStageRecordVO> devopsCiStageRecordVOS = ConvertUtils.convertList(devopsCiStageDTOList, DevopsCiStageRecordVO.class);

        // 计算stage状态
        devopsCiStageRecordVOS.forEach(stageRecord -> {
            List<DevopsCiJobRecordVO> devopsCiJobRecordVOS = jobRecordMap.get(stageRecord.getName());
            if (!CollectionUtils.isEmpty(devopsCiJobRecordVOS)) {
                Map<String, List<DevopsCiJobRecordVO>> statusMap = devopsCiJobRecordVOS.stream().collect(Collectors.groupingBy(DevopsCiJobRecordVO::getStatus));
                calculateStageStatus(stageRecord, statusMap);
                // 计算stage耗时
                stageRecord.setDurationSeconds(calculateStageDuration(devopsCiJobRecordVOS));
                stageRecord.setJobRecordVOList(devopsCiJobRecordVOS);
            }
        });
        // stage排序
        devopsCiStageRecordVOS = devopsCiStageRecordVOS.stream().sorted(Comparator.comparing(DevopsCiStageRecordVO::getSequence)).filter(v -> v.getStatus() != null).collect(Collectors.toList());
        devopsCiPipelineRecordVO.setStageRecordVOList(devopsCiStageRecordVOS);

        return devopsCiPipelineRecordVO;
    }

    /**
     * 将CiJob纪录进行转化和信息填充
     *
     * @param devopsCiJobRecordDTO 数据库中查出的纪录
     * @return 接口需要的数据
     */
    private DevopsCiJobRecordVO convertJobRecord(DevopsCiJobRecordDTO devopsCiJobRecordDTO) {
        DevopsCiJobRecordVO result = ConvertUtils.convertObject(devopsCiJobRecordDTO, DevopsCiJobRecordVO.class);
        List<DevopsCiJobArtifactRecordDTO> artifacts = devopsCiJobArtifactRecordMapper.listByGitlabJobId(devopsCiJobRecordDTO.getGitlabJobId());
        if (!artifacts.isEmpty()) {
            result.setArtifacts(ConvertUtils.convertList(artifacts, artifact -> new DevopsCiArtifactVO(artifact.getName(), artifact.getFileUrl())));
        }
        return result;
    }

    /**
     * 添加提交信息
     */
    private void addCommitInfo(DevopsCiPipelineRecordVO devopsCiPipelineRecordVO, DevopsCiPipelineRecordDTO devopsCiPipelineRecordDTO) {
        DevopsGitlabCommitDTO devopsGitlabCommitDTO = devopsGitlabCommitService.baseQueryByShaAndRef(devopsCiPipelineRecordDTO.getCommitSha(), devopsCiPipelineRecordDTO.getGitlabTriggerRef());
        IamUserDTO commitUser = null;
        if (devopsGitlabCommitDTO.getUserId() != null) {
            commitUser = baseServiceClientOperator.queryUserByUserId(devopsGitlabCommitDTO.getUserId());
        }

        CustomCommitVO customCommitVO = new CustomCommitVO();
        customCommitVO.setRef(devopsCiPipelineRecordDTO.getGitlabTriggerRef());
        customCommitVO.setCommitSha(devopsCiPipelineRecordDTO.getCommitSha());
        customCommitVO.setCommitContent(devopsGitlabCommitDTO.getCommitContent());
        customCommitVO.setCommitUrl(devopsGitlabCommitDTO.getUrl());
        customCommitVO.setGitlabProjectUrl(calculateGitlabProjectUrl(devopsGitlabCommitDTO.getUrl()));

        if (commitUser != null) {
            customCommitVO.setUserHeadUrl(commitUser.getImageUrl());
            customCommitVO.setUserName(commitUser.getLdap() ? commitUser.getLoginName() : commitUser.getEmail());
        }


        devopsCiPipelineRecordVO.setCommit(customCommitVO);
    }

    /**
     * 计算gitlab project地址
     *
     * @param url
     * @return
     */
    private String calculateGitlabProjectUrl(String url) {
        url = url.substring(0, url.lastIndexOf('/'));
        return url.substring(0, url.lastIndexOf('/'));
    }

    private Long calculateStageDuration(List<DevopsCiJobRecordVO> devopsCiJobRecordVOS) {
        Optional<DevopsCiJobRecordVO> max = devopsCiJobRecordVOS.stream().filter(v -> v.getDurationSeconds() != null).max(Comparator.comparingInt(v -> v.getDurationSeconds().intValue()));
        return max.orElse(new DevopsCiJobRecordVO()).getDurationSeconds();
    }

    @Override
    @Transactional
    public void deleteByPipelineId(Long ciPipelineId) {
        if (ciPipelineId == null) {
            throw new CommonException(ERROR_PIPELINE_ID_IS_NULL);
        }
        DevopsCiPipelineRecordDTO pipelineRecordDTO = new DevopsCiPipelineRecordDTO();
        pipelineRecordDTO.setCiPipelineId(ciPipelineId);
        devopsCiPipelineRecordMapper.delete(pipelineRecordDTO);
    }

    @Override
    public List<DevopsCiPipelineRecordDTO> queryByPipelineId(Long ciPipelineId) {
        if (ciPipelineId == null) {
            throw new CommonException(ERROR_PIPELINE_ID_IS_NULL);
        }
        DevopsCiPipelineRecordDTO pipelineRecordDTO = new DevopsCiPipelineRecordDTO();
        pipelineRecordDTO.setCiPipelineId(ciPipelineId);
        return devopsCiPipelineRecordMapper.select(pipelineRecordDTO);
    }

    @Override
    @Transactional
    public void deleteByGitlabProjectId(Long gitlabProjectId) {
        Objects.requireNonNull(gitlabProjectId);
        DevopsCiPipelineRecordDTO pipelineRecordDTO = new DevopsCiPipelineRecordDTO();
        pipelineRecordDTO.setGitlabProjectId(gitlabProjectId);
        devopsCiPipelineRecordMapper.delete(pipelineRecordDTO);
    }

    @Override
    public DevopsCiPipelineRecordDTO create(Long ciPipelineId, Long gitlabProjectId, Pipeline pipeline) {
        DevopsCiPipelineRecordDTO pipelineRecordDTO = new DevopsCiPipelineRecordDTO();
        pipelineRecordDTO.setCiPipelineId(ciPipelineId);
        pipelineRecordDTO.setGitlabProjectId(gitlabProjectId);
        pipelineRecordDTO.setGitlabPipelineId(TypeUtil.objToLong(pipeline.getId()));
        pipelineRecordDTO.setCreatedDate(pipeline.getCreatedAt());
        pipelineRecordDTO.setFinishedDate(pipeline.getFinished_at());
        pipelineRecordDTO.setDurationSeconds(TypeUtil.objToLong(pipeline.getDuration()));
        pipelineRecordDTO.setStatus(pipeline.getStatus().toValue());
        pipelineRecordDTO.setTriggerUserId(DetailsHelper.getUserDetails().getUserId());
        pipelineRecordDTO.setGitlabTriggerRef(pipeline.getRef());
        pipelineRecordDTO.setCommitSha(pipeline.getSha());
        devopsCiPipelineRecordMapper.insertSelective(pipelineRecordDTO);
        return devopsCiPipelineRecordMapper.selectByPrimaryKey(pipelineRecordDTO.getId());
    }

    @Override
    public void retry(Long projectId, Long gitlabPipelineId, Long gitlabProjectId) {
        Assert.notNull(gitlabPipelineId, ERROR_GITLAB_PIPELINE_ID_IS_NULL);
        Assert.notNull(gitlabProjectId, ERROR_GITLAB_PROJECT_ID_IS_NULL);

        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(DetailsHelper.getUserDetails().getUserId());
        checkUserBranchPushPermission(projectId, gitlabPipelineId, gitlabProjectId, userAttrDTO.getGitlabUserId());
        // 重试pipeline
        Pipeline pipeline = gitlabServiceClientOperator.retryPipeline(gitlabProjectId.intValue(), gitlabPipelineId.intValue(), userAttrDTO.getGitlabUserId().intValue());

        try {
            // 更新pipeline status
            DevopsCiPipelineRecordDTO devopsCiPipelineRecordDTO = updatePipelineStatus(gitlabPipelineId, pipeline.getStatus().toValue());
            // 更新job status
            List<JobDTO> jobDTOS = gitlabServiceClientOperator.listJobs(gitlabProjectId.intValue(), gitlabPipelineId.intValue(), userAttrDTO.getGitlabUserId().intValue());
            updateOrInsertJobRecord(devopsCiPipelineRecordDTO.getId(), gitlabProjectId, jobDTOS, userAttrDTO.getIamUserId());
        } catch (Exception e) {
            LOGGER.info("update pipeline Records failed， gitlabPipelineId {}.", gitlabPipelineId);
        }

    }

    @Override
    public void cancel(Long projectId, Long gitlabPipelineId, Long gitlabProjectId) {
        Assert.notNull(gitlabPipelineId, ERROR_GITLAB_PIPELINE_ID_IS_NULL);
        Assert.notNull(gitlabProjectId, ERROR_GITLAB_PROJECT_ID_IS_NULL);

        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(DetailsHelper.getUserDetails().getUserId());
        checkUserBranchPushPermission(projectId, gitlabPipelineId, gitlabProjectId, userAttrDTO.getGitlabUserId());

        gitlabServiceClientOperator.cancelPipeline(gitlabProjectId.intValue(), gitlabPipelineId.intValue(), userAttrDTO.getGitlabUserId().intValue());

        try {
            // 更新pipeline status
            DevopsCiPipelineRecordDTO devopsCiPipelineRecordDTO = updatePipelineStatus(gitlabPipelineId, PipelineStatus.CANCELED.toValue());
            // 更新job status
            List<JobDTO> jobDTOS = gitlabServiceClientOperator.listJobs(gitlabProjectId.intValue(), gitlabPipelineId.intValue(), userAttrDTO.getGitlabUserId().intValue());
            updateOrInsertJobRecord(devopsCiPipelineRecordDTO.getId(), gitlabProjectId, jobDTOS, userAttrDTO.getIamUserId());
        } catch (Exception e) {
            LOGGER.info("update pipeline Records failed， gitlabPipelineId {}.", gitlabPipelineId);
        }
    }

    @Override
    public DevopsCiPipelineRecordDTO queryById(Long ciPipelineRecordId) {

        return devopsCiPipelineRecordMapper.selectByPrimaryKey(ciPipelineRecordId);
    }

    /**
     * 校验用户是否有分支权限
     */
    private void checkUserBranchPushPermission(Long projectId, Long gitlabPipelineId, Long gitlabProjectId, Long gitlabUserId) {
        DevopsCiPipelineRecordDTO recordDTO = new DevopsCiPipelineRecordDTO();
        recordDTO.setGitlabPipelineId(gitlabPipelineId);
        DevopsCiPipelineRecordDTO devopsCiPipelineRecordDTO = devopsCiPipelineRecordMapper.selectOne(recordDTO);
        devopsCiPipelineService.checkUserBranchPushPermission(projectId, gitlabUserId, gitlabProjectId, devopsCiPipelineRecordDTO.getGitlabTriggerRef());
    }

    /**
     * 更新pipeline status
     */
    private DevopsCiPipelineRecordDTO updatePipelineStatus(Long gitlabPipelineId, String status) {
        DevopsCiPipelineRecordDTO recordDTO = new DevopsCiPipelineRecordDTO();
        recordDTO.setGitlabPipelineId(gitlabPipelineId);
        DevopsCiPipelineRecordDTO devopsCiPipelineRecordDTO = devopsCiPipelineRecordMapper.selectOne(recordDTO);
        devopsCiPipelineRecordDTO.setStatus(status);
        devopsCiPipelineRecordMapper.updateByPrimaryKeySelective(devopsCiPipelineRecordDTO);
        return devopsCiPipelineRecordDTO;
    }

    private void updateOrInsertJobRecord(Long ciPipelineRecordId, Long gitlabProjectId, List<JobDTO> jobDTOS, Long iamUserId) {
        jobDTOS.forEach(jobDTO -> {
            DevopsCiJobRecordDTO recordDTO = new DevopsCiJobRecordDTO();
            recordDTO.setGitlabJobId(TypeUtil.objToLong(jobDTO.getId()));
            DevopsCiJobRecordDTO devopsCiJobRecordDTO = devopsCiJobRecordMapper.selectOne(recordDTO);
            // job记录存在则更新，不存在则插入
            if (devopsCiJobRecordDTO == null) {
                devopsCiJobRecordService.create(ciPipelineRecordId, gitlabProjectId, jobDTO, iamUserId);
            } else {
                devopsCiJobRecordDTO.setGitlabJobId(jobDTO.getId().longValue());
                devopsCiJobRecordDTO.setTriggerUserId(iamUserId);
                devopsCiJobRecordDTO.setStatus(jobDTO.getStatus().toValue());
                devopsCiJobRecordMapper.updateByPrimaryKeySelective(devopsCiJobRecordDTO);
            }
        });

    }


    private <T> void calculateStageStatus(DevopsCiStageRecordVO stageRecord, Map<String, List<T>> statsuMap) {
        if (!CollectionUtils.isEmpty(statsuMap.get(JobStatusEnum.CREATED.value()))) {
            stageRecord.setStatus(JobStatusEnum.CREATED.value());
        } else if (!CollectionUtils.isEmpty(statsuMap.get(JobStatusEnum.PENDING.value()))) {
            stageRecord.setStatus(JobStatusEnum.PENDING.value());
        } else if (!CollectionUtils.isEmpty(statsuMap.get(JobStatusEnum.RUNNING.value()))) {
            stageRecord.setStatus(JobStatusEnum.RUNNING.value());
        } else if (!CollectionUtils.isEmpty(statsuMap.get(JobStatusEnum.FAILED.value()))) {
            stageRecord.setStatus(JobStatusEnum.FAILED.value());
        } else if (!CollectionUtils.isEmpty(statsuMap.get(JobStatusEnum.SUCCESS.value()))) {
            stageRecord.setStatus(JobStatusEnum.SUCCESS.value());
        } else if (!CollectionUtils.isEmpty(statsuMap.get(JobStatusEnum.CANCELED.value()))) {
            stageRecord.setStatus(JobStatusEnum.CANCELED.value());
        } else if (!CollectionUtils.isEmpty(statsuMap.get(JobStatusEnum.SKIPPED.value()))) {
            stageRecord.setStatus(JobStatusEnum.SKIPPED.value());
        } else if (!CollectionUtils.isEmpty(statsuMap.get(JobStatusEnum.MANUAL.value()))) {
            stageRecord.setStatus(JobStatusEnum.MANUAL.value());
        }
    }

    private Long getIamUserIdByGitlabUserName(String username) {
        if ("admin1".equals(username) || "root".equals(username)) {
            return 1L;
        }
        UserAttrDTO userAttrE = userAttrService.baseQueryByGitlabUserName(username);
        return userAttrE.getIamUserId();
    }
}
