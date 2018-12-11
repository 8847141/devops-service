package io.choerodon.devops.app.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.choerodon.core.domain.Page;
import io.choerodon.devops.api.dto.C7nCertificationDTO;
import io.choerodon.devops.api.dto.CertificationDTO;
import io.choerodon.devops.api.dto.OrgCertificationDTO;
import io.choerodon.devops.api.validator.DevopsCertificationValidator;
import io.choerodon.devops.app.service.CertificationService;
import io.choerodon.devops.app.service.DevopsEnvironmentService;
import io.choerodon.devops.app.service.GitlabGroupMemberService;
import io.choerodon.devops.domain.application.entity.*;
import io.choerodon.devops.domain.application.handler.ObjectOperation;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.domain.application.valueobject.C7nCertification;
import io.choerodon.devops.domain.application.valueobject.Organization;
import io.choerodon.devops.domain.application.valueobject.certification.*;
import io.choerodon.devops.infra.common.util.EnvUtil;
import io.choerodon.devops.infra.common.util.FileUtil;
import io.choerodon.devops.infra.common.util.GitUserNameUtil;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.common.util.enums.*;
import io.choerodon.devops.infra.dataobject.CertificationFileDO;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.websocket.helper.EnvListener;

/**
 * Created by n!Ck
 * Date: 2018/8/20
 * Time: 17:47
 * Description:
 */
@Service
public class CertificationServiceImpl implements CertificationService {

    private static final String CERT_PREFIX = "cert-";
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    @Value("${cert.testCert}")
    private Boolean testCert;

    @Autowired
    private CertificationRepository certificationRepository;
    @Autowired
    private DevopsEnvironmentRepository devopsEnvironmentRepository;
    @Autowired
    private IamRepository iamRepository;
    @Autowired
    private DevopsCertificationValidator devopsCertificationValidator;
    @Autowired
    private UserAttrRepository userAttrRepository;
    @Autowired
    private GitlabGroupMemberService gitlabGroupMemberService;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;
    @Autowired
    private EnvListener envListener;
    @Autowired
    private EnvUtil envUtil;
    @Autowired
    private DevopsEnvFileResourceRepository devopsEnvFileResourceRepository;
    @Autowired
    private GitlabRepository gitlabRepository;
    @Autowired
    private DevopsEnvCommandRepository devopsEnvCommandRepository;
    @Autowired
    private DevopsEnvUserPermissionRepository devopsEnvUserPermissionRepository;

    @Override
    public void create(Long projectId, C7nCertificationDTO certificationDTO,
                       MultipartFile key, MultipartFile cert, Boolean isGitOps) {

        //校验用户是否有环境的权限
        devopsEnvUserPermissionRepository.checkEnvDeployPermission(TypeUtil.objToLong(GitUserNameUtil.getUserId()), certificationDTO.getEnvId());

        String certName = certificationDTO.getCertName();
        String type = certificationDTO.getType();
        List<String> domains = certificationDTO.getDomains();

        Long envId = certificationDTO.getEnvId();

        CertificationFileDO certificationFileDO = null;
        //如果创建的时候选择证书
        if (certificationDTO.getCertId() != null) {
            CertificationE certificationE = certificationRepository.queryById(certificationDTO.getCertId());
            certificationFileDO = certificationRepository.getCertFile(certificationE.getCertificationFileId());
        }

        //校验环境是否链接
        DevopsEnvironmentE devopsEnvironmentE = devopsEnvironmentRepository.queryById(envId);

        envUtil.checkEnvConnection(devopsEnvironmentE.getClusterE().getId(), envListener);

        devopsCertificationValidator.checkCertification(envId, certName);


        // status operating
        CertificationE certificationE = new CertificationE(null,
                certName, devopsEnvironmentE, domains, CertificationStatus.OPERATING.getStatus());

        C7nCertification c7nCertification = null;

        if (!isGitOps) {
            String envCode = devopsEnvironmentE.getCode();
            String path = fileTmpPath(projectId, envCode);

            c7nCertification = getC7nCertification(
                    certName, type, domains, certificationFileDO == null ? getFileContent(path, key) : certificationFileDO.getKeyFile(), certificationFileDO == null ? getFileContent(path, cert) : certificationFileDO.getCertFile(), envCode);

            if (certificationFileDO == null) {
                removeFiles(path, key);
                removeFiles(path, cert);
            }
            // sent certification to agent
            ObjectOperation<C7nCertification> certificationOperation = new ObjectOperation<>();
            certificationOperation.setType(c7nCertification);
            operateEnvGitLabFile(certName, devopsEnvironmentE, c7nCertification);


            //创建证书,当集群速度较快时，会导致部署速度快于gitlab创文件的返回速度，从而证书成功的状态会被错误更新为处理中，所以用after去区分是否部署成功。成功不再执行证书数据库操作
            CertificationE afterCertificationE = certificationRepository.queryByEnvAndName(devopsEnvironmentE.getId(), certificationE.getName());
            if (afterCertificationE == null) {
                // create
                certificationE = certificationRepository.create(certificationE);
                Long certId = certificationE.getId();
                // cert command
                certificationE.setCommandId(createCertCommandE(CommandType.CREATE.getType(), certId, null));
                certificationRepository.updateCommandId(certificationE);
                // store crt & key if type is upload
                storeCertFile(c7nCertification, certId);
            }
        } else {
            // create
            certificationE = certificationRepository.create(certificationE);
            Long certId = certificationE.getId();
            // cert command
            certificationE.setCommandId(createCertCommandE(CommandType.CREATE.getType(), certId, null));
            certificationRepository.updateCommandId(certificationE);
            // store crt & key if type is upload
            storeCertFile(c7nCertification, certId);
        }

    }

    private void removeFiles(String path, MultipartFile multipartFile) {
        if (multipartFile != null) {
            new File(path + FILE_SEPARATOR + multipartFile.getOriginalFilename()).deleteOnExit();
        }
    }

    private void storeCertFile(C7nCertification c7nCertification, Long certId) {
        if (c7nCertification != null) {
            CertificationExistCert existCert = c7nCertification.getSpec().getExistCert();
            if (existCert != null) {
                CertificationE certificationE = new CertificationE();
                certificationE.setCertificationFileId(certificationRepository.storeCertFile(
                        new CertificationFileDO(existCert.getCert(), existCert.getKey())));
                certificationE.setId(certId);
                certificationRepository.updateCertFileId(certificationE);
            }
        }
    }

    private String fileTmpPath(Long projectId, String envCode) {
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        return String.format("tmp%s%s%s%s", FILE_SEPARATOR, projectE.getCode(), FILE_SEPARATOR, envCode);
    }

    private String getFileContent(String path, MultipartFile file) {
        return file == null ? null : FileUtil.getFileContent(new File(FileUtil.multipartFileToFile(path, file)));
    }

    @Override
    public C7nCertification getC7nCertification(String name, String type, List<String> domains,
                                                String keyContent, String certContent, String envCode) {
        C7nCertification c7nCertification = new C7nCertification();

        c7nCertification.setMetadata(new CertificationMetadata(name,
                envCode));
        CertificationSpec spec = new CertificationSpec(testCert);
        if (type.equals(CertificationType.REQUEST.getType())) {
            CertificationAcme acme = new CertificationAcme();
            acme.initConfig(new CertificationConfig(domains));
            spec.setAcme(acme);
        } else if (type.equals(CertificationType.UPLOAD.getType())) {
            CertificationExistCert existCert = new CertificationExistCert(keyContent, certContent);
            spec.setExistCert(existCert);
        }
        spec.setCommonName(domains.get(0));
        spec.setDnsNames(domains.size() > 1 ? domains.stream().skip(1).collect(Collectors.toList()) : null);
        c7nCertification.setSpec(spec);
        return c7nCertification;
    }

    private void operateEnvGitLabFile(String certName,
                                      DevopsEnvironmentE devopsEnvironmentE,
                                      C7nCertification c7nCertification) {
        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        gitlabGroupMemberService.checkEnvProject(devopsEnvironmentE, userAttrE);
        devopsEnvironmentService.handDevopsEnvGitRepository(devopsEnvironmentE);

        ObjectOperation<C7nCertification> objectOperation = new ObjectOperation<>();
        objectOperation.setType(c7nCertification);
        objectOperation.operationEnvGitlabFile(CERT_PREFIX + certName,
                TypeUtil.objToInteger(devopsEnvironmentE.getGitlabEnvProjectId()), "create",
                userAttrE.getGitlabUserId(), null, null, null, null);
    }

    @Override
    public void deleteById(Long certId) {
        CertificationE certificationE = certificationRepository.queryById(certId);
        Long certEnvId = certificationE.getEnvironmentE().getId();
        //校验用户是否有环境的权限
        devopsEnvUserPermissionRepository.checkEnvDeployPermission(TypeUtil.objToLong(GitUserNameUtil.getUserId()), certEnvId);
        DevopsEnvironmentE devopsEnvironmentE = devopsEnvironmentRepository.queryById(certEnvId);

        envUtil.checkEnvConnection(devopsEnvironmentE.getClusterE().getId(), envListener);


        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        gitlabGroupMemberService.checkEnvProject(devopsEnvironmentE, userAttrE);
        Integer gitLabEnvProjectId = TypeUtil.objToInteger(devopsEnvironmentE.getGitlabEnvProjectId());
        String certificateType = ObjectType.CERTIFICATE.getType();
        String certName = certificationE.getName();
        DevopsEnvFileResourceE devopsEnvFileResourceE = devopsEnvFileResourceRepository
                .queryByEnvIdAndResource(certEnvId, certId, certificateType);

        if (devopsEnvFileResourceE == null) {
            certificationRepository.deleteById(certId);
        }
        certificationE.setCommandId(createCertCommandE(CommandType.DELETE.getType(), certId, null));
        certificationRepository.updateCommandId(certificationE);
        certificationE.setStatus(CertificationStatus.DELETING.getStatus());
        certificationRepository.updateStatus(certificationE);

        if (devopsEnvFileResourceE != null && devopsEnvFileResourceE.getFilePath() != null
                && devopsEnvFileResourceRepository
                .queryByEnvIdAndPath(certEnvId, devopsEnvFileResourceE.getFilePath()).size() == 1) {
            gitlabRepository.deleteFile(
                    gitLabEnvProjectId,
                    devopsEnvFileResourceE.getFilePath(),
                    "DELETE FILE " + certName,
                    TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
        } else {
            ObjectOperation<C7nCertification> certificationOperation = new ObjectOperation<>();
            C7nCertification c7nCertification = new C7nCertification();
            CertificationMetadata certificationMetadata = new CertificationMetadata();
            certificationMetadata.setName(certName);
            c7nCertification.setMetadata(certificationMetadata);
            certificationOperation.setType(c7nCertification);
            certificationOperation.operationEnvGitlabFile(
                    null, gitLabEnvProjectId,
                    "delete", userAttrE.getGitlabUserId(), certId, certificateType, certEnvId,
                    devopsEnvironmentService.handDevopsEnvGitRepository(devopsEnvironmentE));
        }
    }

    @Override
    public List<OrgCertificationDTO> listByProject(Long projectId) {
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
        List<OrgCertificationDTO> orgCertificationDTOS = new ArrayList<>();
        certificationRepository.listByProject(projectId, organization.getId()).forEach(certificationDTO -> {
            OrgCertificationDTO orgCertificationDTO = new OrgCertificationDTO();
            orgCertificationDTO.setName(certificationDTO.getCertName());
            orgCertificationDTO.setId(certificationDTO.getId());
            orgCertificationDTO.setDomain(certificationDTO.getDomains().get(0));
            orgCertificationDTOS.add(orgCertificationDTO);
        });
        return orgCertificationDTOS;
    }

    @Override
    public void certDeleteByGitOps(Long certId) {
        CertificationE certificationE = certificationRepository.queryById(certId);

        //校验环境是否连接
        DevopsEnvironmentE devopsEnvironmentE = devopsEnvironmentRepository.queryById(certificationE.getEnvironmentE().getId());

        envUtil.checkEnvConnection(devopsEnvironmentE.getClusterE().getId(), envListener);

        //实例相关对象数据库操作
        DevopsEnvCommandE devopsEnvCommandE = devopsEnvCommandRepository
                .query(certificationE.getCommandId());
        devopsEnvCommandE.setStatus(CommandStatus.SUCCESS.getStatus());
        devopsEnvCommandRepository.update(devopsEnvCommandE);
        certificationRepository.deleteById(certId);
    }

    @Override
    public Page<CertificationDTO> page(Long projectId, Long envId, PageRequest pageRequest, String params) {
        if (params == null) {
            params = "{}";
        }

        return certificationRepository.page(projectId, null, envId, pageRequest, params);
    }

    @Override
    public List<CertificationDTO> getActiveByDomain(Long envId, String domain) {
        return certificationRepository.getActiveByDomain(envId, domain);
    }

    @Override
    public Boolean checkCertNameUniqueInEnv(Long envId, String certName) {
        return certificationRepository.checkCertNameUniqueInEnv(envId, certName);
    }

    @Override
    public Long createCertCommandE(String type, Long certId, Long userId) {
        DevopsEnvCommandE devopsEnvCommandE = new DevopsEnvCommandE();
        devopsEnvCommandE.setCommandType(type);
        devopsEnvCommandE.setCreatedBy(userId);
        devopsEnvCommandE.setObject(ObjectType.CERTIFICATE.getType());
        devopsEnvCommandE.setStatus(CommandStatus.OPERATING.getStatus());
        devopsEnvCommandE.setObjectId(certId);
        return devopsEnvCommandRepository.create(devopsEnvCommandE).getId();
    }

}
